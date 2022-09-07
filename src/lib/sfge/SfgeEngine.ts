import {Logger} from '@salesforce/core';
import {SfgeWrapper} from './SfgeWrapper';
import {AbstractRuleEngine} from '../services/RuleEngine';
import {CUSTOM_CONFIG, ENGINE, Severity} from '../../Constants';
import {Controller} from '../../Controller';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget, SfgeConfig, TargetPattern} from '../../types';
import {Config} from '../util/Config';
import * as EngineUtils from '../util/CommonEngineUtils';
import { EventCreator } from '../util/EventCreator';

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

type SfgeViolation = {
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

export class SfgeEngine extends AbstractRuleEngine {
	private static ENGINE_ENUM: ENGINE = ENGINE.SFGE;
	private static ENGINE_NAME: string = ENGINE.SFGE.valueOf();

	private logger: Logger;
	private config: Config;
	private eventCreator: EventCreator;
	private initialized: boolean;

	/**
	 * Invokes sync/async initialization required for the engine
	 */
	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getName());
		this.config = await Controller.getConfig();
		this.eventCreator = await EventCreator.create({});
		this.initialized = true;
	}

	/**
	 * Returns the name of the engine as referenced everywhere within the code
	 */
	public getName(): string {
		return SfgeEngine.ENGINE_NAME;
	}

	public async getTargetPatterns(): Promise<TargetPattern[]> {
		return await this.config.getTargetPatterns(SfgeEngine.ENGINE_ENUM);
	}

	/**
	 * Fetches the default catalog of rules supported by this engine.
	 */
	public async getCatalog(): Promise<Catalog> {
		const catalogOutput: string = await SfgeWrapper.getCatalog();
		const ruleOutputStart: number = catalogOutput.indexOf(CATALOG_START) + CATALOG_START.length;
		const ruleOutputEnd: number = catalogOutput.indexOf(CATALOG_END);
		const ruleOutput: string = catalogOutput.slice(ruleOutputStart, ruleOutputEnd);
		const partialRules: SfgePartialRule[] = JSON.parse(ruleOutput) as SfgePartialRule[];

		return this.createCatalogFromPartialRules(partialRules);
	}

	private createCatalogFromPartialRules(partialRules: SfgePartialRule[]): Catalog {
		// For each raw rule, we'll want to synthesize an actual rule object.
		const completeRules: Rule[] = [];
		// We'll also want to pull out the name of every category we encounter.
		const categoryNames: Set<string> = new Set();

		partialRules.forEach(({name, description, category}) => {
			completeRules.push({
				engine: ENGINE.SFGE,
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
	 */
	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public shouldEngineRun(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string,string>): boolean {
		// If the engine isn't filtered out, there's no reason to not run it.
		return true;
	}

	/**
	 * @param engineOptions - A mapping of keys to values for engineOptions. Not all key/value pairs will apply to all engines.
	 */
	public async run(ruleGroups: RuleGroup[], rules: Rule[], targets: RuleTarget[], engineOptions: Map<string,string>): Promise<RuleResult[]> {
		// Make sure we have actual targets to run against.
		let targetCount = 0;
		targets.forEach((t) => {
			if (t.methods.length > 0) {
				// If we're targeting individual methods, then each method is counted as a separate target for this purpose.
				targetCount += t.methods.length;
			} else {
				targetCount += t.paths.length;
			}
		});

		if (targetCount === 0) {
			this.logger.trace(`No targets from ${SfgeEngine.ENGINE_NAME} found. Nothing to execute. Returning early.`);
			return [];
		}

		this.logger.trace(`About to run ${SfgeEngine.ENGINE_NAME} rules. Targets: ${targetCount} files and/or methods, Selected rules: ${JSON.stringify(rules)}`);

		let results: RuleResult[];
		try {
			// Execute SFGE
			const output = await SfgeWrapper.runSfge(targets, rules, JSON.parse(engineOptions.get(CUSTOM_CONFIG.SfgeConfig)) as SfgeConfig);
			results = this.processStdout(output);
		} catch (e) {
			// Handle errors thrown
			const message = e instanceof Error ? e.message : e as string;
			this.logger.trace(`${SfgeEngine.ENGINE_NAME} evaluation failed. ${message}`);
			this.eventCreator.createUxErrorMessage('error.external.sfgeIncompleteAnalysis', [SfgeEngine.processStderr(message)]);
			// Handle output results no matter the outcome
			results = this.processStdout(message);
		}

		this.logger.trace(`Found ${results.length} results for ${SfgeEngine.ENGINE_NAME}`);
		return results;
	}

	/**
	 * TODO: Not supported yet. Idea is to detect if a custom rule path is supported by this engine.
	 */
	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public matchPath(path: string): boolean {
		// TODO: Implement this method for real, eventually.
		return false;
	}

	/**
	 * Returns value of `isEngineEnabled` based on Config or an internal decision.
	 */
	public async isEnabled(): Promise<boolean> {
		return await this.config.isEngineEnabled(SfgeEngine.ENGINE_ENUM);
	}

	/**
	 * Helps decide if an instance of this engine should be included in a run based on the values provided in the --engine
	 * filter and Engine Options.
	 */
	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public isEngineRequested(filterValues: string[], engineOptions: Map<string,string>): boolean {
		// If the engine was specifically requested, or there were no engine filters at all, then this engine should be
		// treated as requested. That way:
		// - SFGE will be included in cataloging for `scanner:rule:list` if no filters are provided or if it's requested,
		// - SFGE will be manually excluded from `scanner:run` by virtue of being a DFA engine,
		// - SFGE will be manually included in `scanner:run:dfa` by virtue of being a DFA engine.
		return EngineUtils.isFilterEmptyOrNameInFilter(this.getName(), filterValues);
	}

	public isDfaEngine(): boolean {
		// NOTE: If SFGE implements and exposes non-DFA rules, then this will no longer be accurate. In that case, the
		// best course of action is probably to divide SFGE into two engines: one for DFA rules and one for pathless rules.
		return true;
	}

	private static processStderr(output: string): string {
		// We should handle errors by checking for our error start string.
		const errorStart = output.indexOf(ERROR_START);
		if (errorStart === -1) {
			// If our error start string is missing altogether, then something went disastrously wrong, and we should
			// assume that the entire stderr is relevant.
			return output;
		} else {
			// If the error start string is present, it means we exited cleanly and everything prior to the string is noise
			// that can be omitted.
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

			result.violations.push({
				ruleName: sfgeViolation.ruleName,
				severity: sfgeViolation.severity,
				message: sfgeViolation.message,
				category: sfgeViolation.category,
				url: sfgeViolation.url,
				sinkLine: sfgeViolation.sinkLineNumber || null,
				sinkColumn: sfgeViolation.sinkColumnNumber || null,
				sinkFileName: sfgeViolation.sinkFileName || "",
				sourceLine: sfgeViolation.sourceLineNumber,
				sourceColumn: sfgeViolation.sourceColumnNumber,
				sourceType: sfgeViolation.sourceType,
				sourceMethodName: sfgeViolation.sourceVertexName
			});
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
