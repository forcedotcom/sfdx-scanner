import {Rule, Catalog, RuleGroup, RuleResult} from '../../types';

export interface RuleEngine {
	getName(): string;

	getCatalog(): Promise<Catalog>;

	run(ruleGroups: RuleGroup[], rules: Rule[], target: string[] | string): Promise<RuleResult[]>;

	init(): Promise<void>;

	matchPath(path: string): boolean;

	isEnabled(): boolean;
}
