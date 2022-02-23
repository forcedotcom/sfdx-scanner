import {Severity} from '../../Constants';
import {Catalog, EngineExecutionDescriptor, Rule, RuleGroup, RuleResult, RuleTarget, TargetPattern} from '../../types';

export interface RuleEngine {

	/**
	 * Returns the name of the engine as referenced everywhere within the code
	 */
	getName(): string;

	/**
	 * Patterns of target that an engine can process
	 */
	getTargetPatterns(): Promise<TargetPattern[]>;

	/**
	 * Fetches the default catalog of rules supported by the engine
	 */
	getCatalog(): Promise<Catalog>;

	/**
	 * Converts the severity created by engine to a normalized value across all engines.
	 */
	normalizeSeverity(results: RuleResult[]): void;

	/**
	 * Helps make decision to run an engine or not based on the Rules, Target paths and
	 * the Engine Options selected per run. At this point, we should be already past filtering.
	 */
	shouldEngineRun(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): boolean;

	/**
	 * @param descriptor - An object containing the values necessary for executing the engine, including rules, groups,
	 * 		targets, and engine options.
	 */
	runEngine(descriptor: EngineExecutionDescriptor): Promise<RuleResult[]>;

	/**
	 * Invokes sync/async initialization required for the engine
	 */
	init(): Promise<void>;

	/**
	 * TODO: Not supported yet. Idea is to detect if a custom rule path
	 * is supported by the engine
	 */
	matchPath(path: string): boolean;

	/**
	 * Returns value of isEngineEnabled based on Config or an internal decision
	 */
	isEnabled(): Promise<boolean>;

	/**
	 * Helps decide if an instance of this engine should be included in a run
	 * based on the values provided in the --engine filter and Engine Options
	 */
	isEngineRequested(filterValues: string[], engineOptions: Map<string, string>): boolean;
}

export abstract class AbstractRuleEngine implements RuleEngine {

	abstract getName(): string;
	abstract getTargetPatterns(): Promise<TargetPattern[]>;
	abstract getCatalog(): Promise<Catalog>;
	abstract shouldEngineRun(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): boolean;
	abstract run(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]>;
	abstract init(): Promise<void>;
	abstract matchPath(path: string): boolean;
	abstract isEnabled(): Promise<boolean>;
	abstract isEngineRequested(filterValues: string[], engineOptions: Map<string, string>): boolean;
	abstract getNormalizedSeverity(severity: number): Severity;

	async runEngine(descriptor: EngineExecutionDescriptor): Promise<RuleResult[]>{
        const results = await this.run(descriptor.ruleGroups, descriptor.rules, descriptor.target, descriptor.engineOptions);
		if (descriptor.normalizeSeverity) {
			this.normalizeSeverity(results);
		}
        return results;
    }

	public normalizeSeverity(results: RuleResult[]): void{
		for (const result of results) {
			for (const violation of result.violations) {
				violation.normalizedSeverity = this.getNormalizedSeverity(violation.severity);
			}
		}
	}
}
