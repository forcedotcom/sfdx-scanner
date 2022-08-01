import {Logger, SfdxError} from '@salesforce/core';
import {AbstractRuleEngine} from '../services/RuleEngine';
import {CUSTOM_CONFIG, ENGINE, Severity} from '../../Constants';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget, RuleViolation, SfgeConfig, TargetPattern} from '../../types';
import {Config} from '../util/Config';
import {SfgeCatalogWrapper, SfgeExecutionWrapper} from './SfgeWrapper';
import * as EngineUtils from "../util/CommonEngineUtils";

const CATALOG_START = 'CATALOG_START';
const CATALOG_END = 'CATALOG_END';
const DFA = "dfa";
const PATHLESS = "pathless";

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

	protected logger: Logger;
	protected config: Config;
	protected catalog: Catalog;

	protected abstract getEnum(): ENGINE;

	/**
	 * @override
	 */
	public async getTargetPatterns(): Promise<TargetPattern[]> {
		return await this.config.getTargetPatterns(this.getEnum());
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
		const ruleType = this.isDfaEngine() ? DFA : PATHLESS;
		const catalogOutput: string = await SfgeCatalogWrapper.getCatalog(ruleType);
		const ruleOutputStart: number = catalogOutput.indexOf(CATALOG_START) + CATALOG_START.length;
		const ruleOutputEnd: number = catalogOutput.indexOf(CATALOG_END);
		const ruleOutput: string = catalogOutput.slice(ruleOutputStart, ruleOutputEnd);
		const partialRules: SfgePartialRule[] = JSON.parse(ruleOutput) as SfgePartialRule[];

		this.catalog = this.createCatalogFromPartialRules(partialRules);
		return this.catalog;
	}

	/**
	 * Convert the rule objects provided by SFGE into an actual catalog suitable for the analyzer's needs.
	 * @param partialRules - The partial rules returned by SFGE's "catalog" flow.
	 * @private
	 */
	private createCatalogFromPartialRules(partialRules: SfgePartialRule[]): Catalog {
		// For each raw rule, we'll want to synthesize an actual rule object.
		const completeRules: Rule[] = [];
		// We'll also want to put out the name of every category we encounter.
		const categoryNames: Set<string> = new Set();

		partialRules.forEach(({name, description, category}) => {
			completeRules.push({
				engine: this.getEnum(),
				sourcepackage: "sfge",
				name,
				description,
				// SFGE rules each belong to exactly one category, so the string must be converted to a singleton array.
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
				engine: this.getEnum(),
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
		// If the engine wasn't filtered out, there's no reason we shouldn't run it.
		return true;
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

		// If there are no targets, there's no point in running the rules.
		if (targetCount === 0) {
			this.logger.trace(`No targets from ${this.getName()} found. Nothing to execute. Returning early.`);
			return [];
		}

		// The rules have yet to be filtered by DFA/Non-DFA, so it's possible that some of the provided rules
		// don't actually belong to this SFGE subvariant. So we should filter the rules so we only run ones
		// in this engine's catalog.
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
				this.logger.trace(`Rule ${rule.name} is ineligible to run with this SFGE subvariant`);
			}
		}

		// If there are no rules that we can run, there's no point in running.
		if (filteredRules.length === 0) {
			this.logger.trace(`No eligible rules for ${this.getName()} found. Nothing to execute. Returning early.`);
			return [];
		}

		this.logger.trace(`About to run ${this.getName()} rules. Targets: ${targetCount} files and/or methods, Selected rules: ${JSON.stringify(filteredRules)}`);

		// Make sure we've actually got a config, because if we don't, we have a problem.
		if (!engineOptions.has(CUSTOM_CONFIG.SfgeConfig)) {
			throw SfdxError.create('@salesforce/sfdx-scanner', 'BaseSfgeEngine', 'missingConfig');
		}
		const sfgeConfig: SfgeConfig = JSON.parse(engineOptions.get(CUSTOM_CONFIG.SfgeConfig)) as SfgeConfig;
		try {
			const output = await SfgeExecutionWrapper.runSfge(targets, filteredRules, sfgeConfig);

			// TODO: There should be some kind of method-call here to pull logs and warnings from the output.
			const results = this.processStdout(output);
			this.logger.trace(`Found ${results.length} results for ${this.getName()}`);
			return results;
		} catch (e) {
			const message = e instanceof Error ? e.message : e as string;
			this.logger.trace(`${this.getName()} evaluation failed. ${message}`);
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
				engine: this.getEnum().valueOf(),
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
		return await this.config.isEngineEnabled(this.getEnum());
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
