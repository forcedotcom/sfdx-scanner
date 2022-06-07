import {Logger, SfdxError} from '@salesforce/core';
import { Catalog, ESRuleConfig, ESRuleConfigValue, LooseObject, Rule, RuleGroup, RuleResult, RuleTarget, ESRule, TargetPattern, ESRuleMetadata } from '../../types';
import {ENGINE, Severity} from '../../Constants';
import {OutputProcessor} from '../services/OutputProcessor';
import {AbstractRuleEngine} from '../services/RuleEngine';
import {Config} from '../util/Config';
import {Controller} from '../../Controller';
import {deepCopy} from '../util/Utils';
import {StaticDependencies, EslintProcessHelper, ProcessRuleViolationType} from './EslintCommons';
import * as engineUtils from '../util/CommonEngineUtils';
import {ESLint} from 'eslint';

// TODO: DEFAULT_ENV_VARS is part of a fix for W-7791882 that was known from the beginning to be a sub-optimal solution.
//       During the 3.0 release cycle, an alternate fix should be implemented that doesn't leak the abstraction. If this
//       requires deleting DEFAULT_ENV_VARS, so be it.
// These are the environment variables that we'll want enabled by default in our ESLint baseConfig.
const DEFAULT_ENV_VARS: LooseObject = {
	es6: true, 				// `Map` class and others
	node: true, 			// `process` global var and others
	browser: true,			// `document` global var
	webextensions: true,	// Chrome
	jasmine: true,			// `describe', 'expect', 'it' global vars
	jest: true,				// 'jest' global var
	jquery: true,			// '$' global var
	mocha: true				// `describe' and 'it' global vars
};

type EslintEnv = {
	[key: string]: boolean;
};

const ENV = 'env';

export interface EslintStrategy {

	/** Initialize strategy */
	init(): Promise<void>;

	/** Get engine that strategy supports */
	getEngine(): ENGINE;

	/** Get config options for eslint engine to use for scanning. */
	getRunOptions(engineOptions: Map<string, string>): Promise<ESLint.Options>;

	/** Get languages supported by engine */
	getLanguages(): string[];

	/** After applying target patterns, last chance to filter any unsupported files */
	filterUnsupportedPaths(paths: string[]): string[];

	/**
	 * Indicates whether the rule with the specified name should be treated as enabled by default (i.e., run in the
	 * absence of filter criteria).
	 * @param {string} name - The name of a rule.
	 * @returns {boolean} true if the rule should be enabled by default.
	 */
	ruleDefaultEnabled(name: string): boolean;

	/**
	 * Returns the default configuration associated with the specified rule, as per the corresponding "recommended" ruleset.
	 * @param {string} ruleName - The name of a rule in this engine.
	 * @returns {ESRuleConfigValue} The rule's default recommended configuration.
	 */
	getDefaultConfig(ruleName: string): ESRuleConfigValue;

	/** Returns all rules available for this engine */
	getRuleMap(): Map<string, ESRule>;

	/** Allow the strategy to convert the RuleViolation */
	processRuleViolation(): ProcessRuleViolationType;
}

export abstract class BaseEslintEngine extends AbstractRuleEngine {

	private strategy: EslintStrategy;
	protected logger: Logger;
	private initializedBase: boolean;
	protected outputProcessor: OutputProcessor;
	private baseDependencies: StaticDependencies;
	private helper: EslintProcessHelper;
	private config: Config;
	private catalog: Catalog;

	async initializeContents(strategy: EslintStrategy, baseDependencies = new StaticDependencies()): Promise<void> {
		if (this.initializedBase) {
			return;
		}
		this.config = await Controller.getConfig();
		this.strategy = strategy;
		this.logger = await Logger.child(this.getName());
		this.baseDependencies = baseDependencies;
		this.helper = new EslintProcessHelper();

		this.initializedBase = true;
	}

	matchPath(path: string): boolean {
		// TODO implement matchPath when Custom Rules are handled for eslint
		this.logger.trace(`Custom rules for eslint is not supported yet: ${path}`);
		return false;
	}

	getName(): string {
		return this.strategy.getEngine().valueOf();
	}

	async isEnabled(): Promise<boolean> {
		return await this.config.isEngineEnabled(this.strategy.getEngine());
	}

	async getTargetPatterns(): Promise<TargetPattern[]> {
		return await this.config.getTargetPatterns(this.strategy.getEngine());
	}

	getCatalog(): Promise<Catalog> {
		if (!this.catalog) {
			const categoryMap: Map<string, RuleGroup> = new Map();
			const rules: Rule[] = [];

			// Get all rules supported by eslint
			const allRules = this.strategy.getRuleMap();

			// Add eslint rules to catalog
			allRules.forEach((esRule: ESRule, key: string) => {
				const meta = esRule.meta;

				const rule = this.processRule(key, meta);
				if (rule) {
					// Add only rules supported by the engine implementation
					rules.push(rule);
					const categoryName = meta.type;
					let category = categoryMap.get(categoryName);
					if (!category) {
						category = { name: categoryName, engine: this.getName(), paths: [] };
						categoryMap.set(categoryName, category);
					}
					category.paths.push(meta.docs.url);
				}
			});

			this.catalog = {
				categories: Array.from(categoryMap.values()),
				rules: rules,
				rulesets: []
			};
		}

		return Promise.resolve(this.catalog);
	}



	private processRule(key: string, meta: ESRuleMetadata): Rule {
		// Massage eslint rule into Catalog rule format
		const rule = {
			engine: this.getName(),
			sourcepackage: this.getName(),
			name: key,
			description: meta.docs.description,
			categories: [meta.type],
			rulesets: [meta.type],
			languages: [...this.strategy.getLanguages()],
			defaultEnabled: this.strategy.ruleDefaultEnabled(key),
			defaultConfig: this.strategy.getDefaultConfig(key),
			url: meta.docs.url
		};
		return rule;
	}

	shouldEngineRun(
		ruleGroups: RuleGroup[],
		rules: Rule[],
		target: RuleTarget[],
		engineOptions: Map<string, string>): boolean {

		return !this.helper.isCustomRun(engineOptions)
			&& (target && target.length > 0)
			&& rules.length > 0;
	}

	isEngineRequested(filterValues: string[], engineOptions: Map<string, string>): boolean {
		return !this.helper.isCustomRun(engineOptions)
		&& engineUtils.isFilterEmptyOrNameInFilter(this.getName(), filterValues);
	}

	public isDfaEngine(): boolean {
		return false;
	}

	async run(ruleGroups: RuleGroup[], rules: Rule[], targets: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]> {

		// Get sublist of rules supported by the engine
		const configuredRules = this.configureRules(rules).rules;
		if (Object.keys(configuredRules).length === 0) {
			// No rules to run
			this.logger.trace('No matching rules to run. Nothing to execute.');
			return [];
		}

		try {
			const results: RuleResult[] = [];

			// Process one target path at a time to trigger eslint
			for (const target of targets) {
				// TODO: Will this break the typescript parser cwd setting?
				const cwd = target.isDirectory ? this.baseDependencies.resolveTargetPath(target.target) : this.baseDependencies.getCurrentWorkingDirectory();
				this.logger.trace(`Using current working directory in config as ${cwd}`);
				const config: ESLint.Options = {cwd};

				target.paths = this.strategy.filterUnsupportedPaths(target.paths);

				if (target.paths.length === 0) {
					// No target files to analyze
					this.logger.trace(`No target files to analyze from ${JSON.stringify(target.paths)}`);
					continue; // to the next target
				}

				// get run-config for the engine and add to config
				Object.assign(config, deepCopy(await this.strategy.getRunOptions(engineOptions)));
				// At this point, the inner `overrideConfig` property should exist. If it doesn't, we'll set it to an
				// empty object to avoid NPEs.
				config.overrideConfig = config.overrideConfig || {};
				config.overrideConfig.rules = configuredRules;

				// TODO: This whole code block is part of a fix to W-7791882, which was known from the start to be sub-optimal.
				//       It requires too much leaking of the abstraction. So during the 3.0 cycle, we should replace it with
				//       something better.
				// From https://eslint.org/docs/developer-guide/nodejs-api:
				// options.baseConfig. Configuration object, extended by all configurations used with this instance.
				// You can use this option to define the default settings that will be used if your configuration files don't configure it.
				// If they don't already have a baseConfig property, we'll need to instantiate one.
				config.baseConfig = config.baseConfig || {env: {}};
				// We'll also need to potentially modify the provided config's environment variables. We can merge two objects
				// by using the spread syntax (...x). Later parameters override earlier ones in a conflict, so we want
				// the default values to be overridden by whatever was already in the env property, and we want the manual
				// override to trump both of those things.
				const envOverride = engineOptions.has(ENV) ? JSON.parse(engineOptions.get(ENV)) as LooseObject : {};
				BaseEslintEngine.validateEnv(envOverride);
				config.baseConfig.env = {...DEFAULT_ENV_VARS, ...config.baseConfig.env, ...envOverride};
				// ==== This is the end of the sup-optimal solution to W-7791882.

				this.logger.trace(`About to run ${this.getName()}. targets: ${target.paths.length}`);
				const eslint = this.baseDependencies.createESLint(config);
				const esResults: ESLint.LintResult[] = await eslint.lintFiles(target.paths);
				this.logger.trace(`Finished running ${this.getName()}`);

				const rulesMeta = eslint.getRulesMetaForResults(esResults);
				const rulesMap: Map<string,ESRuleMetadata> = new Map();
				Object.keys(rulesMeta).forEach(key => rulesMap.set(key, rulesMeta[key]));
				// Map results to supported format
				this.helper.addRuleResultsFromReport(this.strategy.getEngine(), results, esResults, rulesMap, this.strategy.processRuleViolation());
			}

			return results;
		} catch (e) {
			const message: string = e instanceof Error ? e.message : e as string;
			throw new SfdxError(message);
		}
	}

	getNormalizedSeverity(severity: number): Severity {
		switch (severity) {
			case 1:
				return Severity.MODERATE;
			case 2:
				return Severity.HIGH;
			default:
				return Severity.MODERATE;
		}
	}

	/**
	 * Uses a list of rules to generate an object suitable for use as the "rules" property of an ESLint configuration.
	 * @param {Rule[]} rules - A list of rules that we want to run
	 * @returns {ESRuleConfig} A mapping from rule names to the configuration at which they should run.
	 * @private
	 */
	private configureRules(rules: Rule[]): ESRuleConfig {
		const configuredRules: ESRuleConfig = {rules: {}};
		rules.forEach(rule => {
			// If the rule has a default configuration associated with it, we use it. Otherwise, we default to "error".
			configuredRules.rules[rule.name] = rule.defaultConfig || 'error';
		});
		return configuredRules;
	}

	private static validateEnv(env: unknown): env is EslintEnv {
		if (env == null) {
			// Null/undefined count as valid for our purposes.
			return true;
		}
		// For a non-null value, iterate through the keys and make sure each one corresponds to a boolean.
		for (const key of Object.keys(env)) {
			if (typeof env[key] !== 'boolean') {
				// A typical typeguard function would return false here. But in our case, there's no point continuing at all
				// if the env isn't valid. So we should just throw an exception instead.
				throw new Error(`Provided ESLint env ${JSON.stringify(env)} must have exclusively boolean properties`);
			}
		}
		// If all the keys are booleans, we're good.
		return true;
	}
}
