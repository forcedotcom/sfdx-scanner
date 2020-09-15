import {RecombinedRuleResults, Rule} from '../types';
import {OUTPUT_FORMAT} from '../Constants';
import {RuleFilter} from './RuleFilter';

export interface RuleManager {
	init(): Promise<void>;

	getRulesMatchingCriteria(filters: RuleFilter[]): Promise<Rule[]>;

	/**
	 * @param engineOptions - see RuleEngine#run
	 */
	runRulesMatchingCriteria(filters: RuleFilter[], target: string[], format: OUTPUT_FORMAT, engineOptions: Map<string, string>): Promise<RecombinedRuleResults>;
}
