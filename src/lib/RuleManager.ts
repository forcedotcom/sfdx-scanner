import {RecombinedRuleResults, Rule} from '../types';
import {RuleFilter} from './RuleFilter';
import {OUTPUT_FORMAT} from '../Constants';

export interface RuleManager {
	init(): Promise<void>;

	getRulesMatchingCriteria(filters: RuleFilter[]): Rule[];

	/**
	 * @param engineOptions - see RuleEngine#run
	 */
	runRulesMatchingCriteria(filters: RuleFilter[], target: string[], format: OUTPUT_FORMAT, engineOptions: Map<string, string>): Promise<RecombinedRuleResults>;
}
