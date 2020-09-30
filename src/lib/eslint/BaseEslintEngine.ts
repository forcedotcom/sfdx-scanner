import {Logger, SfdxError} from '@salesforce/core';
import {Catalog, LooseObject, Rule, RuleGroup, RuleResult, RuleTarget, RuleViolation, ESRule, ESReport, ESMessage} from '../../types';
import {ENGINE} from '../../Constants';
import {OutputProcessor} from '../pmd/OutputProcessor';
import {RuleEngine} from '../services/RuleEngine';
import {CLIEngine} from 'eslint';
import * as path from 'path';
import {Config} from '../util/Config';
import {Controller} from '../../Controller';
import {deepCopy} from '../../lib/util/Utils';

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

const ENV = 'env';
const BASECONFIG = 'baseConfig';

export interface EslintStrategy {

	/** Initialize strategy */
	init(): Promise<void>;

	/** Get engine that strategy supports */
	getEngine(): ENGINE;

	/** Get eslint config that can be used to get catalog */
	/* eslint-disable @typescript-eslint/no-explicit-any */
	getCatalogConfig(): Record<string, any>;

	/** Get eslint engine to use for scanning. */
	getRunConfig(engineOptions: Map<string, string>): Promise<Record<string, any>>;

	/** Get languages supported by engine */
	getLanguages(): string[];

	/** After applying target patterns, last chance to filter any unsupported files */
	filterUnsupportedPaths(paths: string[]): string[];

	/** Allow the strategy to convert the RuleViolation */
	processRuleViolation(fileName: string, ruleViolation: RuleViolation): void;
}

export class StaticDependencies {
	/* eslint-disable @typescript-eslint/no-explicit-any */
	createCLIEngine(config: Record<string,any>): CLIEngine {
		return new CLIEngine(config);
	}

	resolveTargetPath(target: string): string {
		return path.resolve(target);
	}

	getCurrentWorkingDirectory(): string {
		return process.cwd();
	}
}

export abstract class BaseEslintEngine implements RuleEngine {

	private strategy: EslintStrategy;
	protected logger: Logger;
	private initializedBase: boolean;
	protected outputProcessor: OutputProcessor;
	private baseDependencies: StaticDependencies;
	private config: Config;
	private catalog: Catalog;

	// We'll leave init abstract to allow implementations to initialize
	async abstract init(): Promise<void>;

	async initializeContents(strategy: EslintStrategy, baseDependencies = new StaticDependencies()): Promise<void> {
		if (this.initializedBase) {
			return;
		}
		this.config = await Controller.getConfig();
		this.strategy = strategy;
		this.logger = await Logger.child(this.getName());
		this.baseDependencies = baseDependencies;

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

	async getTargetPatterns(): Promise<string[]> {
		return await this.config.getTargetPatterns(this.strategy.getEngine());
	}

	getCatalog(): Promise<Catalog> {
		if (!this.catalog) {
			const categoryMap: Map<string, RuleGroup> = new Map();
			const rules: Rule[] = [];

			// Get all rules supported by eslint
			const cli = this.baseDependencies.createCLIEngine(this.strategy.getCatalogConfig());
			const allRules = cli.getRules();

			// Add eslint rules to catalog
			allRules.forEach((esRule: ESRule, key: string) => {
				const docs = esRule.meta.docs;

				const rule = this.processRule(key, docs);
				if (rule) {
					// Add only rules supported by the engine implementation
					rules.push(rule);
					const categoryName = docs.category;
					let category = categoryMap.get(categoryName);
					if (!category) {
						category = { name: categoryName, engine: this.getName(), paths: [] };
						categoryMap.set(categoryName, category);
					}
					category.paths.push(docs.url);
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

	/* eslint-disable @typescript-eslint/no-explicit-any */
	private processRule(key: string, docs: any): Rule {
		// Massage eslint rule into Catalog rule format
		const rule = {
			engine: this.getName(),
			sourcepackage: this.getName(),
			name: key,
			description: docs.description,
			categories: [docs.category],
			rulesets: [docs.category],
			languages: [...this.strategy.getLanguages()],
			defaultEnabled: docs.recommended,
			url: docs.url
		};
		return rule;
	}

	async run(ruleGroups: RuleGroup[], rules: Rule[], targets: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]> {
		// If we didn't find any paths, we're done.
		if (!targets || targets.length === 0) {
			this.logger.trace('No matching target files found. Nothing to execute.');
			return [];
		}

		// Get sublist of rules supported by the engine
		const filteredRules = await this.selectRelevantRules(rules);
		if (Object.keys(filteredRules).length === 0) {
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
				const config = {cwd};

				config["rules"] = filteredRules;

				target.paths = this.strategy.filterUnsupportedPaths(target.paths);

				if (target.paths.length === 0) {
					// No target files to analyze
					this.logger.trace(`No target files to analyze from ${target.paths}`);
					continue; // to the next target
				}

				// get run-config for the engine and add to config
				Object.assign(config, deepCopy(await this.strategy.getRunConfig(engineOptions)));

				// TODO: This whole code block is part of a fix to W-7791882, which was known from the start to be sub-optimal.
				//       It requires too much leaking of the abstraction. So during the 3.0 cycle, we should replace it with
				//       something better.
				// From https://eslint.org/docs/developer-guide/nodejs-api:
				// options.baseConfig. Configuration object, extended by all configurations used with this instance.
				// You can use this option to define the default settings that will be used if your configuration files don't configure it.
				// If they don't already have a baseConfig property, we'll need to instantiate one.
				config[BASECONFIG] = config[BASECONFIG] || {[ENV]: {}};
				// We'll also need to potentially modify the provided config's environment variables. We can merge two objects
				// by using the spread syntax (...x). Later parameters override earlier ones in a conflict, so we want
				// the default values to be overridden by whatever was already in the env property, and we want the manual
				// override to trump both of those things.
				const envOverride = engineOptions.has(ENV) ? JSON.parse(engineOptions.get(ENV)) : {};
				config[BASECONFIG][ENV] = {...DEFAULT_ENV_VARS, ...config[BASECONFIG][ENV], ...envOverride};
				// ==== This is the end of the sup-optimal solution to W-7791882.

				this.logger.trace(`About to run ${this.getName()}. targets: ${target.paths.length}`);

				const cli = this.baseDependencies.createCLIEngine(config);

				const report = cli.executeOnFiles(target.paths);
				this.logger.trace(`Finished running ${this.getName()}`);

				// Map results to supported format
				this.addRuleResultsFromReport(results, report, cli.getRules());
			}

			return results;
		} catch (e) {
			throw new SfdxError(e.message || e);
		}
	}

	/* eslint-disable @typescript-eslint/no-explicit-any */
	private async selectRelevantRules(rules: Rule[]): Promise<Record<string,any>> {
		const filteredRules = {};

		// the eslint engines will run all rules explicitly enabled and all 'recommended' rules
		// that are inherited from the 'extends' configuration attribute. Disable all rules that
		// the engine would run by default. The requested rules will be  enabled below.
		if (rules.length > 0) {
			for (const rule of (await this.getCatalog()).rules.filter(r => r.defaultEnabled)) {
				filteredRules[rule.name] = 'off';
			}
		}

		rules.forEach(rule => filteredRules[rule.name] = 'error');

		this.logger.trace(`Count of rules selected for ${this.getName()}: ${rules.length}`);
		return filteredRules;
	}

	private addRuleResultsFromReport(results: RuleResult[], report: ESReport, ruleMap: Map<string, ESRule>): void {
		for (const r of report.results) {
			// Only add report entries that have actual violations to report.
			if (r.messages && r.messages.length > 0) {
				results.push(this.toRuleResult(r.filePath, r.messages, ruleMap));
			}
		}
	}

	private toRuleResult(fileName: string, messages: ESMessage[], ruleMap: Map<string, ESRule>): RuleResult {
		return {
			engine: this.getName(),
			fileName,
			violations: messages.map(
				(v): RuleViolation => {
					const rule = ruleMap.get(v.ruleId);
					const category = rule ? rule.meta.docs.category : "";
					const url = rule ? rule.meta.docs.url : "";
					const violation: RuleViolation = {
						line: v.line,
						column: v.column,
						severity: v.severity,
						message: v.message,
						ruleName: v.ruleId,
						category,
						url
					};

					this.strategy.processRuleViolation(fileName, violation);

					return violation;
				}
			)
		};
	}
}
