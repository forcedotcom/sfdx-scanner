/**
 * An interface for Dependency Checker engines.
 * @author Josh Feingold
 */

export interface DependencyChecker {
	getName(): string;

	run(target?: string): Promise<[boolean, string, string]>;

	isEnabled(): boolean;

	init(): Promise<void>;
}
