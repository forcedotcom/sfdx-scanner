import {Rule} from '../types';
import {RuleFilter} from './RuleFilter';

export enum OUTPUT_FORMAT {
	XML = 'xml',
	JUNIT = 'junit',
	CSV = 'csv',
	TABLE = 'table'
}

export interface RuleManager {
	init(): Promise<void>;

	getRulesMatchingCriteria(filters: RuleFilter[]): Promise<Rule[]>;

	runRulesMatchingCriteria(filters: RuleFilter[], target: string[] | string, format: OUTPUT_FORMAT): Promise<string | {columns; rows}>;
}
