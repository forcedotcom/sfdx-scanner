import { CommandLineSupport } from '../services/CommandLineSupport';

export type PmdSupportOptions = {
	/**
	 * A mapping from language names to Sets of files that define rules for those languages.
	 */
	rulePathsByLanguage: Map<string, Set<string>>;
};

export abstract class PmdSupport extends CommandLineSupport {
	protected readonly rulePathsByLanguage: Map<string, Set<string>>;

	protected constructor(opts: PmdSupportOptions) {
		super({});
		this.rulePathsByLanguage = opts.rulePathsByLanguage;
	}

	protected buildSharedClasspath(): string[] {
		const classpath: string[] = [];
		// Every entry in the rule paths map should be added to the classpath.
		for (const rulePaths of this.rulePathsByLanguage.values()) {
			classpath.push(...rulePaths);
		}
		return classpath;
	}
}
