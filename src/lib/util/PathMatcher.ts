import picomatch = require('picomatch');

type MatchingFunction = (string) => boolean;


export class PathMatcher {
	private readonly matcher: MatchingFunction;

	/**
	 *
	 * @param {string[]} patterns - An array of strings that are either positive patterns (don't start with `!`), or
	 *                              negative patterns (do start with `!`).
	 */
	constructor(patterns: string[]) {
		this.matcher = this.generateMatchingFunction(patterns);
	}

	/**
	 * Creates a MatchingFunction for the provided patterns.
	 * @param {string[]} patterns - An array of strings that are either positive patterns (don't start with `!`), or
	 *                              negative patterns (do start with `!`).
	 * @returns {MatchingFunction} A function that will match the provided patterns.
	 */
	private generateMatchingFunction(patterns: string[]): MatchingFunction {
		// Picomatch matches patterns using a logical OR (i.e. it returns true if the target matches ANY provided pattern).
		//
		// Inclusion patterns (e.g., '**/*.js') indicate what a path is allowed to look like. We want files that match ANY
		// inclusion pattern, and since that's consistent with the logical OR used by Picomatch, we can just create a single
		// matcher for all inclusion patterns.
		//
		// Exclusion patterns (e.g., '!**/node_modules/**') indicate what a path is NOT allowed to look like. We want to
		// exclude files UNLESS they match EVERY exclusion pattern, which is a logical AND.
		// DeMorgan's Law states that (p && q) == !(!p || !q), so we can turn our AND into an OR by inverting every exclusion
		// pattern and negating the result of that matcher.
		const inclusionPatterns = [];
		const exclusionPatterns = [];
		patterns.forEach(p => p.startsWith('!') ? exclusionPatterns.push(p.slice(1)) : inclusionPatterns.push(p));

		const inclusionMatcher = inclusionPatterns && inclusionPatterns.length ? picomatch(inclusionPatterns) : (): boolean => true;
		const exclusionMatcher = exclusionPatterns && exclusionPatterns.length ? picomatch(exclusionPatterns) : (): boolean => false;

		return (t: string): boolean => {return inclusionMatcher(t) && !exclusionMatcher(t)};
	}

	/**
	 * Returns the subset of the provided paths matching ANY of this matcher's positive patterns AND ALL of its negative patterns.
	 * @param {string[]} targets - An array of paths
	 * @returns {string[]} - The subset of the target strings that match the provided patterns.
	 */
	public filterPathsByPatterns(targets: string[]): string[] {
		return targets.filter(this.matcher);
	}

	/**
	 * Returns true if the provided target string matches ANY of this matcher's positive patterns AND ALL of its negative patterns.
	 * @param {string} target - A path.
	 * @returns {boolean}
	 */
	public pathMatchesPatterns(target: string): boolean {
		return this.matcher(target);
	}
}
