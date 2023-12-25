import {Rule} from '../types';
import {RuleFilter} from './RuleFilter';
import {Results} from "./output/Results";

export enum OUTPUT_FORMAT {
	CSV = 'csv',
	HTML = 'html',
	JSON = 'json',
	JUNIT = 'junit',
	SARIF = 'sarif',
	TABLE = 'table',
	XML = 'xml'
}

export type RunOptions  = {
	format: OUTPUT_FORMAT;
	normalizeSeverity: boolean;
	runDfa: boolean;
	withPilot: boolean;
	sfVersion: string;
}

export type EngineOptions = Map<string, string>;

export interface RuleManager {
	init(): Promise<void>;

	/**
	 * Returns rules matching the filter criteria provided, and any non-conflicting implicit filters.
	 * @param {RuleFilter[]} filters - A collection of filters.
	 */
	getRulesMatchingCriteria(filters: RuleFilter[]): Promise<Rule[]>;

	/**
	 * Returns rules that match only the provided filters, completely ignoring any implicit filtering.
	 * @param filters
	 */
	getRulesMatchingOnlyExplicitCriteria(filters: RuleFilter[]): Rule[];

	/**
	 * @param engineOptions - see RuleEngine#run
	 */
	runRulesMatchingCriteria(filters: RuleFilter[], target: string[], runOptions: RunOptions, engineOptions: EngineOptions): Promise<Results>;
}
