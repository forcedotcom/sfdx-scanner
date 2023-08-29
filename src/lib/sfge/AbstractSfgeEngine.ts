import {Logger} from '@salesforce/core';
import {SfgeCatalogWrapper, SfgeExecuteWrapper} from './SfgeWrapper';
import {AbstractRuleEngine} from '../services/RuleEngine';
import {CUSTOM_CONFIG, ENGINE, RuleType, Severity} from '../../Constants';
import {Controller} from '../../Controller';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget, RuleViolation, SfgeConfig, TargetPattern} from '../../types';
import {Config} from '../util/Config';
import {EventCreator} from '../util/EventCreator';

const CATALOG_START = 'CATALOG_START';
const CATALOG_END = 'CATALOG_END';
const VIOLATIONS_START = "VIOLATIONS_START";
const VIOLATIONS_END = "VIOLATIONS_END";
const ERROR_START = "SfgeErrorStart";


type SfgePartialRule = {
	name: string;
	description: string;
	category: string;
	isPilot: boolean;
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
	private initialized: boolean;
	private eventCreator: EventCreator;
	private catalog: Catalog;

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
	 *       will share the same name and everything that comes with it (e.g., config).
	 *       As such, the user experiences a single unified engine whose catalog includes
	 *       both DFA and non-DFA rules, instead of two discrete engines.
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

		partialRules.forEach(({name, description, category, isPilot}) => {
			completeRules.push({
				engine: AbstractSfgeEngine.ENGINE_ENUM,
				sourcepackage: "sfge",
				name,
				description,
				isPilot,
				// Graph Engine rules each belong to exactly one category, so the string must be converted to a singleton array.
				categories: [category],
				// Graph Engine does not use rulests.
				rulesets: [],
				// Currently, all Graph Engine rules are Apex-specific.
				languages: ["apex"],
				// Graph Engine rules are DFA if the engine is DFA.
				isDfa: this.isDfaEngine(),
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
			// If the transaction succeeded, then just use whatever results we find
			// inside the output.
			results = this.parseViolations(output);
		} catch (e) {
			// Get the message from the error.
			const message: string = e instanceof Error ? e.message : e as string;
			// The error message contains both stdout and stderr, so we have to process
			// it as both of those things.
			results = await this.processExecutionFailure(message);
		}
		this.logger.trace(`Found ${results.length} results for ${this.getSubVariantName()}`);
		return results;
	}

	/**
	 * Accepts the complete output of a failed transaction.
	 * If partial results can be found in the output, they are returned and any errors
	 * are logged to the console.
	 * If no partial results are found, then the error is rethrown.
	 * @param rawErrorMessage Per {@link SfgeExecuteWrapper.handleResults}, the concatenated {@code stdout} and {@code stderr} from a failed execution.
	 * @protected
	 */
	protected async processExecutionFailure(rawErrorMessage: string): Promise<RuleResult[]> {
		this.logger.trace(`${this.getSubVariantName()} evaluation failed. ${rawErrorMessage}`);
		// Try to pull partial results from the message.
		const results: RuleResult[] = this.parseViolations(rawErrorMessage);
		// Pull the error from the message.
		const errorMessage = AbstractSfgeEngine.parseError(rawErrorMessage);

		// If there are no results, then we assume the error was fatal and rethrow it,
		// to forcibly bring it to the user's attention.
		if (results.length === 0) {
			throw new Error(errorMessage);
		}

		// If there are results, then the error was definitely non-fatal, but results are incomplete.
		// Log the error to the console, but still return the partial results.
		await this.eventCreator.createUxErrorMessage('error.external.sfgeIncompleteAnalysis', [errorMessage]);
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
	 * Parses {@code output} to find an error message, or returns the entire string if no
	 * designated error can be found.
	 * @param output String containing (possibly among other things) the {@code stderr} from a failed transaction.
	 * @protected
	 */
	protected static parseError(output: string): string {
		// We should handle errors by checking for our error start string.
		const errorStart = output.indexOf(ERROR_START);
		if (errorStart === -1) {
			// If our error start string is missing altogether, then something went disastrously wrong,
			// and we should assume that the entire stderr is relevant.
			return output;
		} else {
			// If the error start string is present, it means we exited cleanly and everything prior
			// to the string is noise that can be omitted.
			// Note: The substring is basically guaranteed to start with some whitespace, so just lop that off.
			return output.slice(errorStart + ERROR_START.length).trimStart();
		}
	}

	/**
	 * Seeks and returns violations within {@code output}, returning an empty array if none can be found.
	 * @param output String containing (possibly among other things) the {@code stdout} from a transaction.
	 * @protected
	 */
	protected parseViolations(output: string): RuleResult[] {
		// Figure out where the violations start.
		const violationsStart = output.indexOf(VIOLATIONS_START);
		// If we can't find the start character, return an empty list.
		if (violationsStart === -1) {
			return [];
		}

		// Pull the raw SFGE violations out of the output.
		const violationsEnd = output.indexOf(VIOLATIONS_END);
		const violationsJson = output.slice(violationsStart + VIOLATIONS_START.length, violationsEnd);
		const sfgeViolations: SfgeViolation[] = JSON.parse(violationsJson) as SfgeViolation[];

		// If there are no SFGE violatiosn, return an empty list.
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
