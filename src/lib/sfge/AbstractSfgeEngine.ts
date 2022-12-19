import {Logger} from '@salesforce/core';
import {SfgeCatalogWrapper, SfgeExecuteWrapper} from './SfgeWrapper';
import {AbstractRuleEngine} from '../services/RuleEngine';
import {CUSTOM_CONFIG, ENGINE, RuleType, Severity} from '../../Constants';
import {Controller} from '../../Controller';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget, RuleViolation, SfgeConfig, TargetPattern} from '../../types';
import {Config} from '../util/Config';
import * as EngineUtils from '../util/CommonEngineUtils';
import {EventCreator} from '../util/EventCreator';

const CATALOG_START = 'CATALOG_START';
const CATALOG_END = 'CATALOG_END';
const VIOLATIONS_START = "VIOLATIONS_START";
const VIOLATIONS_END = "VIOLATIONS_END";
const ERROR_START = "SfgeErrorStart\n";


type SfgePartialRule = {
	name: string;
	description: string;
	category: string;
}

export type SfgeViolation = {
	ruleName: string;
	message: string;
	severity: number;
	category: string;
	url: string;
	sourceLineNumber: number;
	sourceColumnNumber: number;
	sourceFileName: string;
	sourceType: string;
	sourceVertexName: string;
	sinkLineNumber: number;
	sinkColumnNumber: number;
	sinkFileName: string;
};

export abstract class AbstractSfgeEngine extends AbstractRuleEngine {
	protected static ENGINE_ENUM: ENGINE = ENGINE.SFGE;
	protected static ENGINE_NAME: string = ENGINE.SFGE.valueOf();

	private logger: Logger;
	private config: Config;
	private eventCreator: EventCreator;
	private catalog: Catalog;
	private initialized: boolean;

	protected abstract convertViolation(sfgeViolation: SfgeViolation): RuleViolation;

	protected abstract getRuleType(): RuleType;

	protected abstract getSubVariantName(): string;

	/**
	 * Invokes sync/async initialization required for the engine.
	 * @override
	 */
	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		// Make sure that the sub-variants have different names for their loggers.
		// This way, we can implement the method at the abstract level instead of
		// the concrete classes.
		this.logger = await Logger.child(`${this.getSubVariantName()}`);
		this.config = await Controller.getConfig();
		this.eventCreator = await EventCreator.create({});
		this.initialized = true;
	}

	/**
	 * Returns the name of the engine as referenced everywhere within the code.
	 * NOTE: By defining this at the abstract class, all engines in this family
	 * will share the same name and everything that comes with it (e.g., config).
	 * @override
	 */
	public getName(): string {
		return AbstractSfgeEngine.ENGINE_NAME;
	}

	/**
	 * Get the patterns that this engine family can match against.
	 * @override
	 */
	public async getTargetPatterns(): Promise<TargetPattern[]> {
		return await this.config.getTargetPatterns(AbstractSfgeEngine.ENGINE_ENUM);
	}

	/**
	 * Fetches the default catalog of rules supported by this engine.
	 * Different concrete implementations of this class will return
	 * different catalogs.
	 * @override
	 */
	public async getCatalog(): Promise<Catalog> {
		// If we've already got a catalog, return it immediately.
		if (this.catalog) {
			return this.catalog;
		}
		// Each engine sub-variant should only catalog rules it can execute.
		const ruleType: RuleType = this.getRuleType();
		const catalogOutput: string = await SfgeCatalogWrapper.getCatalog(ruleType);
		const ruleOutputStart: number = catalogOutput.indexOf(CATALOG_START) + CATALOG_START.length;
		const ruleOutputEnd: number = catalogOutput.indexOf(CATALOG_END);
		const ruleOutput: string = catalogOutput.slice(ruleOutputStart, ruleOutputEnd);
		const partialRules: SfgePartialRule[] = JSON.parse(ruleOutput) as SfgePartialRule[];

		this.catalog = this.createCatalogFromPartialRules(partialRules);
		return this.catalog;
	}

	/**
	 * Convert the partial rule descriptions returned by Graph Engine into a {@link Catalog}
	 * object that Code Analyzer can use.
	 * @private
	 */
	private createCatalogFromPartialRules(partialRules: SfgePartialRule[]): Catalog {
		// For each raw rule, we'll want to synthesize an actual rule object.
		const completeRules: Rule[] = [];
		// We'll also want to pull out the name of every category we encounter.
		const categoryNames: Set<string> = new Set();

		partialRules.forEach(({name, description, category}) => {
			completeRules.push({
				engine: AbstractSfgeEngine.ENGINE_ENUM,
				sourcepackage: "sfge",
				name,
				description,
				// Graph Engine rules each belong to exactly one category, so the string must be converted to a singleton array.
				categories: [category],
				// Graph Engine does not use rulests.
				rulesets: [],
				// Currently, all Graph Engine rules are Apex-specific.
				languages: ["apex"],
				// Currently, all Graph Engine rules are default-enabled.
				defaultEnabled: true
			});
			categoryNames.add(category);
		});

		// Now we can synthesize categories.
		const completeCategories: RuleGroup[] = [...categoryNames.values()].map(name => {
			return {
				engine: ENGINE.SFGE,
				name,
				paths: []
			};
		});

		return {
			rules: completeRules,
			categories: completeCategories,
			// Graph Engine does not use rulesets.
			rulesets: []
		};
	}

	/**
	 * Helps make decision to run an engine or not based on the Rules, Target paths, and the Engine Options selected per
	 * run. At this point, filtering should have already happened.
	 * @override
	 */
	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public shouldEngineRun(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string,string>): boolean {
		// If the engine wasn't filtered out, there's no reason not to run it.
		// TODO: WE MAY WANT TO RE-ASSESS THIS.
		return true;
	}

	/**
	 * @param engineOptions - A mapping of keys to values for engineOptions. Not all key/value pairs will apply to all engines.
	 * @override
	 */
	public async run(ruleGroups: RuleGroup[], rules: Rule[], targets: RuleTarget[], engineOptions: Map<string,string>): Promise<RuleResult[]> {
		// Make sure we have actual targets to run against.
		let targetCount = 0;
		targets.forEach((t) => {
			if (t.methods.length > 0) {
				// If we're targeting individual methods, then each method is counted
				// as a separate target for this purpose.
				targetCount += t.methods.length;
			} else {
				targetCount += t.paths.length;
			}
		});

		// If there are no targets, there's no point in running the rules.
		if (targetCount === 0) {
			this.logger.trace(`No targets from ${AbstractSfgeEngine.ENGINE_NAME} found. Nothing to execute. Returning early.`);
			return [];
		}

		// At this point, the rules have yet to be filtered by DFA/Non-DFA, so it's possible that some provided rules
		// aren't actually compatible with this GraphEngine sub-variant. So we should filter out any rules that
		// aren't in this engine's catalog.
		const catalogRuleNames: Set<string> = new Set();
		const catalog = await this.getCatalog();
		for (const catalogRule of catalog.rules) {
			catalogRuleNames.add(catalogRule.name);
		}
		const filteredRules: Rule[] = [];
		for (const rule of rules) {
			if (catalogRuleNames.has(rule.name)) {
				filteredRules.push(rule);
			} else {
				this.logger.trace(`Rule ${rule.name} is ineligible to run with this ${this.getSubVariantName()}`);
			}
		}

		// If there are no rules that can be run, there's nothing left to do.
		if (filteredRules.length === 0) {
			this.logger.trace(`No eligible rules for ${this.getSubVariantName()}. Nothing to execute. Returning early.`);
			return [];
		}

		this.logger.trace(`About to run ${this.getSubVariantName()} rules. Targets: ${targetCount} files and/or methods, Selected rules: ${JSON.stringify(filteredRules)}`);

		const sfgeConfig: SfgeConfig = JSON.parse(engineOptions.get(CUSTOM_CONFIG.SfgeConfig)) as SfgeConfig;
		let results: RuleResult[];
		try {
			// Execute graph engine
			const output = await SfgeExecuteWrapper.runSfge(targets, filteredRules, sfgeConfig);
			results = this.processStdout(output);
		} catch (e) {
			// Handle errors thrown
			const message = e instanceof Error ? e.message : e as string;
			this.logger.trace(`${this.getSubVariantName()} evaluation failed. ${message}`);
			await this.eventCreator.createUxErrorMessage('error.external.sfgeIncompleteAnalysis', [AbstractSfgeEngine.processStderr(message)]);
			// Handle output results no matter the outcome.
			results = this.processStdout(message);
		}
		this.logger.trace(`Found ${results.length} results for ${this.getSubVariantName()}`);
		return results;
	}

	/**
	 * TODO: Not supported yet. Idea is to detect if a custom rule path is supported by this engine.
	 * @override
	 */
	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public matchPath(path: string): boolean {
		// TODO: Implement this method for real, eventually.
		return false;
	}

	/**
	 * Returns value of `isEngineEnabled` based on Config or an internal decision.
	 * @override
	 */
	public async isEnabled(): Promise<boolean> {
		return await this.config.isEngineEnabled(AbstractSfgeEngine.ENGINE_ENUM);
	}

	/**
	 * Helps decide if an instance of this engine should be included in a run based on the values
	 * provided in the --engine filter and Engine Options.
	 * @override
	 */
	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public isEngineRequested(filterValues: string[], engineOptions: Map<string,string>): boolean {
		// If `sfge` is requested or there are no engine filters at all, then all GraphEngine sub-variants
		// should be treated as requested. This way, they can all be cataloged together.
		return EngineUtils.isFilterEmptyOrNameInFilter(this.getName(), filterValues);
	}

	private static processStderr(output: string): string {
		// We should handle errors by checking for our error start string.
		const errorStart = output.indexOf(ERROR_START);
		if (errorStart === -1) {
			// If our error start string is missing altogether, then something went disastrously wrong,
			// and we should assume that the entire stderr is relevant.
			return output;
		} else {
			// If the error start string is present, it means we exited cleanly and everything prior
			// to the string is noise that can be omitted.
			return output.slice(errorStart + ERROR_START.length);
		}
	}

	protected processStdout(output: string): RuleResult[] {
		// Pull the violation objects from the output.
		const violationsStart = output.indexOf(VIOLATIONS_START);
		if (violationsStart === -1) {
			return [];
		}

		const violationsEnd = output.indexOf(VIOLATIONS_END);
		const violationsJson = output.slice(violationsStart + VIOLATIONS_START.length, violationsEnd);
		const sfgeViolations: SfgeViolation[] = JSON.parse(violationsJson) as SfgeViolation[];

		if (!sfgeViolations || sfgeViolations.length === 0) {
			// Exit early for no results.
			return [];
		}

		// Each file should have at most one result, with an array of violations.
		// Use a map to guarantee uniqueness.
		const resultMap: Map<string,RuleResult> = new Map();
		for (const sfgeViolation of sfgeViolations) {
			// Index violations by their source file, since the source files were what was
			// actually targeted by the user and therefore the more logical choice for how
			// to sort and display violations.
			const indexFile = sfgeViolation.sourceFileName;
			const result: RuleResult = resultMap.get(indexFile) || {
				engine: this.getName(),
				fileName: indexFile,
				violations: []
			};

			result.violations.push(this.convertViolation(sfgeViolation));
			resultMap.set(indexFile, result);
		}
		return [...resultMap.values()];
	}

	getNormalizedSeverity(severity: number): Severity {
		switch (severity) {
			case 1:
				return Severity.HIGH;
			case 2:
				return Severity.MODERATE;
			case 3:
				return Severity.LOW;
			default:
				return Severity.MODERATE;
		}
	}
}
