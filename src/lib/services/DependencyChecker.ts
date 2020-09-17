/**
 * An interface for Dependency Checker engines.
 * @author Josh Feingold
 */
import {DependencyResult} from '../../types';
import {Service} from './Service';

export interface DependencyChecker extends Service {
	run(target?: string): Promise<DependencyResult[]>;

	init(): Promise<void>;
}
