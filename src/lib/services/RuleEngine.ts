import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget} from '../../types';

export interface RuleEngine {
	getName(): string;

	getTargetPatterns(path?: string): Promise<string[]>;

	getCatalog(): Promise<Catalog>;

	run(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[]): Promise<RuleResult[]>;

	init(): Promise<void>;

	matchPath(path: string): boolean;

	isEnabled(): boolean;
}
