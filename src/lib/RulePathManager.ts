export interface RulePathManager {
	init(): Promise<void>;

	addPathsForLanguage(language: string, paths: string[]): Promise<string[]>;

	getAllPaths(): string[];

	getMatchingPaths(paths: string[]): Promise<string[]>;

	removePaths(paths: string[]): Promise<string[]>;

	getRulePathEntries(engine: string): Promise<Map<string, Set<string>>>;
}

