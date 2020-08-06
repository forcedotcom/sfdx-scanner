import {RuleGroup, Rule} from '../../types';
import {RuleFilter} from '../RuleFilter';

export interface RuleCatalog {
	/**
	 * Asynchronous construction
	 */
	init(): Promise<void>;

	/**
	 * Accepts a set of filter criteria, and returns the paths of all categories and rulesets matching those criteria.
	 * @param {RuleFilter[]} filters
	 */
	getRuleGroupsMatchingFilters(filters: RuleFilter[]): RuleGroup[];

	getRulesMatchingFilters(filters: RuleFilter[]): Promise<Rule[]>;
}
