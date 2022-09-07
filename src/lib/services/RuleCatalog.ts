import {RuleGroup, Rule} from '../../types';
import {RuleFilter} from '../RuleFilter';
import {RuleEngine} from './RuleEngine';

export interface RuleCatalog {
	/**
	 * Asynchronous construction
	 */
	init(): Promise<void>;

	/**
	 * Accepts a set of filter criteria, and returns the paths of all categories and rulesets matching those criteria.
	 * @param {RuleFilter[]} filters
	 * @param {RuleEngine[]} engines
	 */
	getRuleGroupsMatchingFilters(filters: RuleFilter[], engines: RuleEngine[]): Promise<RuleGroup[]>;

	getRulesMatchingFilters(filters: RuleFilter[]): Rule[];

	getRule(engine: string, ruleName: string): Rule;
}
