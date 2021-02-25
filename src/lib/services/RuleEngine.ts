import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget, TargetPattern} from '../../types';

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
	 * Helps make decision to run an engine or not based on the Rules, Target paths and
	 * the Engine Options selected per run. At this point, we should be already past filtering.
	 */
	shouldEngineRun(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): boolean;

	/**
	 * @param engineOptions - a mapping of keys to values for engineOptions. not all key/value pairs will apply to all engines.
	 */
	run(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]>;

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
