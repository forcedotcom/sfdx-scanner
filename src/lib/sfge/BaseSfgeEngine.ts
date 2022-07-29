import {Logger, Messages, SfdxError} from '@salesforce/core';
import {AbstractRuleEngine} from '../services/RuleEngine';
import {CUSTOM_CONFIG, ENGINE, Severity} from '../../Constants';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget, RuleViolation, SfgeConfig, TargetPattern} from '../../types';
import {Config} from '../util/Config';
import {uxEvents, EVENTS} from '../ScannerEvents';
import {SfgeCatalogWrapper, SfgeExecutionWrapper} from './SfgeWrapper';
import * as EngineUtils from "../util/CommonEngineUtils";

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'BaseSfgeEngine');

const CATALOG_START = 'CATALOG_START';
const CATALOG_END = 'CATALOG_END';

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

export abstract class BaseSfgeEngine extends AbstractRuleEngine {
	private static ENGINE_ENUM: ENGINE = ENGINE.SFGE;
	private static ENGINE_NAME: string = ENGINE.SFGE.valueOf();

	protected logger: Logger;
	protected config: Config;
	protected catalog: Catalog;

	/**
	 * Returns the name of the engine as referenced everywhere within the code.
	 * @override
	 */
	public getName(): string {
		// NOTE: Both engines can share the same name without issue.
		return BaseSfgeEngine.ENGINE_NAME;
	}

	/**
	 * @override
	 */
	public async getTargetPatterns(): Promise<TargetPattern[]> {
		// NOTE: Both engines can share the same target patterns without issue.
		return await this.config.getTargetPatterns(BaseSfgeEngine.ENGINE_ENUM);
	}

	/**
	 * Fetches the default catalog of rules supported by this engine.
	 * DFA engine will return DFA rules, non-DFA engine will return non-DFA rules.
	 * @override
	 */
	public async getCatalog(): Promise<Catalog> {
		// If we've already generated the catalog, don't generate another.
		if (this.catalog) {
			return this.catalog;
		}
		// DFA engine should catalog DFA rules, and non-DFA engine should catalog non-DFA rules.
		const ruleType = this.isDfaEngine() ? "dfa" : "pathless";
		const catalogOutput: string = await SfgeCatalogWrapper.getCatalog(ruleType);
		const ruleOutputStart: number = catalogOutput.indexOf(CATALOG_START) + CATALOG_START.length;
		const ruleOutputEnd: number = catalogOutput.indexOf(CATALOG_END);
		const ruleOutput: string = catalogOutput.slice(ruleOutputStart, ruleOutputEnd);
		const partialRules: SfgePartialRule[] = JSON.parse(ruleOutput) as SfgePartialRule[];

		this.catalog = this.createCatalogFromPartialRules(partialRules);
		return this.catalog;
	}

	private createCatalogFromPartialRules(partialRules: SfgePartialRule[]): Catalog {
		// For each raw rule, we'll want to synthesize an actual rule object.
		const completeRules: Rule[] = [];
		// We'll also want to put out the name of every category we encounter.
		const categoryNames: Set<string> = new Set();

		partialRules.forEach(({name, description, category}) => {
			completeRules.push({
				engine: ENGINE.SFGE,
				sourcepackage: "sfge",
				name,
				description,
				// SFGE rules each belogn to exactly one category, so the string must be converted to a singleton array.
				categories: [category],
				// SFGE does not use rulesets.
				rulesets: [],
				// Currently, all SFGE rules are Apex-specific.
				languages: ["apex"],
				// Currently, all SFGE rules are default-enabled.
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
			// SFGE does not use rulesets.
			rulesets: []
		};
	}

	/**
	 * Helps make decision to run an engine or not based on the Rules, Target paths, and the Engine Options selected per
	 * run. At this point, filtering should have already happened.
	 * @override
	 */
	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public shouldEngineRun(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): boolean {
		// If the EngineOptions doesn't include an SFGE config, don't try to run this engine.
		// JUSTIFICATION: Validations earlier in the process prevent user from explicitly running
		// SFGE with no config, but if SFGE is merely an enabled engine, then not specifying a config
		// should implicitly cause SFGE to not run, instead of throwing a violation.
		// Such an implementation is maximally courteous to users upgrading from a previous
		// version.
		if (engineOptions.has(CUSTOM_CONFIG.SfgeConfig)) {
			return true;
		} else {
			uxEvents.emit(EVENTS.WARNING_VERBOSE, messages.getMessage("warning.sfgeSkippedWithoutConfig", []));
			// TODO: We should throw a (verbose-only?) warning here, indicating that the engine
			//  was implicitly skipped.
			return false;
		}
	}

	/**
	 * @param engineOptions - A mapping of keys to values for engineOptions. Not all key/value pairs will apply to all engines.
	 * @override
	 */
	public async run(ruleGroups: RuleGroup[], rules: Rule[], targets: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]> {
		// Make sure we have actual targets to run against.
		let targetCount = 0;
		targets.forEach((t) => {
			if (t.methods.length > 0) {
				// If we're targeting individual methods, then each method is
				// counted as a separate target for this purpose.
				targetCount += t.methods.length;
			} else {
				targetCount += t.paths.length;
			}
		});

		// The rules have yet to be filtered by DFA/Non-DFA, so it's possible that some of the provided rules
		// don't actually belong to this SFGE subvariant. So we should filter the rules so we only run ones
		// in this engine's catalog.
		const catalogRuleNames: Set<string> = new Set();
		for (const catalogRule of this.catalog.rules) {
			catalogRuleNames.add(catalogRule.name);
		}
		const filteredRules: Rule[] = [];
		for (const rule of rules) {
			if (catalogRuleNames.has(rule.name)) {
				filteredRules.push(rule);
			} else {
				this.logger.trace(`Rule ${rule.name} is ineligible to run with this SFGE subvariant`);
			}
		}

		if (targetCount === 0) {
			this.logger.trace(`No targets from ${BaseSfgeEngine.ENGINE_NAME} found. Nothing to execute. Returning early.`);
			return [];
		}

		this.logger.trace(`About to run ${BaseSfgeEngine.ENGINE_NAME} rules. Targets: ${targetCount} files and/or methods, Selected rules: ${JSON.stringify(filteredRules)}`);

		try {
			const output = await SfgeExecutionWrapper.runSfge(targets, filteredRules, JSON.parse(engineOptions.get(CUSTOM_CONFIG.SfgeConfig)) as SfgeConfig);

			// TODO: There should be some kind of method-call here to pull logs and warnings from the output.
			const results = this.processStdout(output);
			this.logger.trace(`Found ${results.length} results for ${BaseSfgeEngine.ENGINE_NAME}`);
			return results;
		} catch (e) {
			const message = e instanceof Error ? e.message : e as string;
			this.logger.trace(`${BaseSfgeEngine.ENGINE_NAME} evaluation failed. ${message}`);
			throw new SfdxError(BaseSfgeEngine.processStderr(message));
		}
	}

	protected processStdout(output: string): RuleResult[] {
		// Pull the violation objects from the output.
		const violationsStartString = "VIOLATIONS_START";
		const violationsStart = output.indexOf(violationsStartString);
		if (violationsStart === -1) {
			return [];
		}
		const violationsEndString = "VIOLATIONS_END";
		const violationsEnd = output.indexOf(violationsEndString);
		const violationsJson = output.slice(violationsStart + violationsStartString.length, violationsEnd);
		const sfgeViolations: SfgeViolation[] = JSON.parse(violationsJson) as SfgeViolation[];

		if (!sfgeViolations || sfgeViolations.length === 0) {
			// Exit early for no results.
			return [];
		}

		// Each file should have at most one result, with an array of violations. Use a map to guarantee uniqueness.
		const resultMap: Map<string,RuleResult> = new Map();
		for (const sfgeViolation of sfgeViolations) {
			// Index violations by their source file, since the source files were what was actually targeted by the user
			// and therefore the more logical choice for how to sort and display violations.
			const indexFile = sfgeViolation.sourceFileName;
			const result: RuleResult = resultMap.get(indexFile) || {
				engine: ENGINE.SFGE.valueOf(),
				fileName: indexFile,
				violations: []
			};

			result.violations.push(this.convertViolation(sfgeViolation));
			resultMap.set(indexFile, result);
		}
		return [...resultMap.values()];
	}

	protected abstract convertViolation(sfgeViolation: SfgeViolation): RuleViolation;

	private static processStderr(output: string): string {
		// We should handle errors by checking for our error start string.
		const errorStartString = "SfgeErrorStart\n";
		const errorStart = output.indexOf(errorStartString);
		if (errorStart === -1) {
			// If our error start string is missing altogether, then something went disastrously wrong, and we should
			// assume that the entire stderr is relevant.
			return output;
		} else {
			// If the error start string is present, it means we exited cleanly and everything prior to the string is noise
			// that can be omitted.
			return output.slice(errorStart + errorStartString.length);
		}
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
		return await this.config.isEngineEnabled(BaseSfgeEngine.ENGINE_ENUM);
	}


	/**
	 * Helps decide if an instance of this engine should be included in a run based on the values provided in the --engine
	 * filter and Engine Options.
	 * @override
	 */
	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public isEngineRequested(filterValues: string[], engineOptions: Map<string,string>): boolean {
		// All SFGE engines should be treated as requested if they were specifically requested,
		// or if no engine filters were provided at all.
		// That way, all SFGE engines will be included in cataloging for `scanner:rule:list`
		// if no filters are provided or if `sfge` is requested.
		// The same is true of `scanner:run` and `scanner:run:dfa`, but the incompatible engine
		// will be prevented from running by virtue of not having the right DFA status.
		return EngineUtils.isFilterEmptyOrNameInFilter(this.getName(), filterValues);
	}

	/**
	 * @override
	 */
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
