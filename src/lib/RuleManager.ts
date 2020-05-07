import {Rule} from '../types';
import {RuleFilter} from './RuleFilter';

export enum OUTPUT_FORMAT {
	XML = 'xml',
	JSON = 'json',
	JUNIT = 'junit',
	CSV = 'csv',
	TABLE = 'table'
}

export interface RuleManager {
	init(): Promise<void>;

	getRulesMatchingCriteria(filters: RuleFilter[]): Promise<Rule[]>;

	runRulesMatchingCriteria(filters: RuleFilter[], target: string[], format: OUTPUT_FORMAT): Promise<string | { columns; rows }>;
}
