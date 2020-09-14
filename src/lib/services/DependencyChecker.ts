

export interface DependencyChecker {
	getName(): string;

	run(): Promise<any>;

	isEnabled(): boolean;

	init(): Promise<void>;
}
