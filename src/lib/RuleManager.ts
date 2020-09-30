import {RecombinedRuleResults, Rule} from '../types';
import {RuleFilter} from './RuleFilter';

export enum OUTPUT_FORMAT {
	CSV = 'csv',
	HTML = 'html',
	JSON = 'json',
	JUNIT = 'junit',
	TABLE = 'table',
	XML = 'xml'
}

export interface RuleManager {
	init(): Promise<void>;

	getRulesMatchingCriteria(filters: RuleFilter[]): Promise<Rule[]>;

	getRulesMatchingOnlyExplicitCriteria(filters: RuleFilter[]): Promise<Rule[]>;

	/**
	 * @param engineOptions - see RuleEngine#run
	 */
	runRulesMatchingCriteria(filters: RuleFilter[], target: string[], format: OUTPUT_FORMAT, engineOptions: Map<string, string>): Promise<RecombinedRuleResults>;
}
