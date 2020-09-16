/**
 * An interface for Dependency Checker engines.
 * @author Josh Feingold
 */
import {DependencyResult} from '../../types';

export interface DependencyChecker {
	getName(): string;

	run(target?: string): Promise<DependencyResult[]>;

	isEnabled(): boolean;

	init(): Promise<void>;
}
