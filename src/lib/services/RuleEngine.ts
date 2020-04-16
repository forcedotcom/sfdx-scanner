import {Rule} from '../../types';
import {RuleFilter} from '../RuleManager';

export interface RuleEngine {
	getName(): string;

	getAll(): Promise<Rule[]>;

	run(filters: RuleFilter[], target: string[] | string): Promise<string>;

	init(): Promise<RuleEngine>;

	matchPath(path: string): boolean;
}
