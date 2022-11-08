/**
 * This regex portion matches @W-0000@ through @W-999999999@,
 * to enforce that the title contains a work record number consumable
 * by Git2Gus.
 * All pull request titles will require this segment.
 */
const WORK_ITEM_SEGMENT = "@W-\\d{4,8}@";

/**
 * This is a regex segment that matches the accepted options for feature branch PR type.
 * NOTE: This segment allows for exactly one type.
 */
const FEATURE_TYPE_SEGMENT = "(NEW|FIX|CHANGE)";

/**
 * These are all the possible options for feature branch PR Scope.
 */
const FEATURE_SCOPE_OPTIONS = [
	"CodeAnalyzer",
	"CPD",
	"ESLint",
	"GraphEngine",
	"PMD",
	"RetireJS"
];

/**
 * This is a regex segment that matches the accepted options for feature branch PR scope, enclosed
 * in parentheses.
 * NOTE: This segment allows for multiple scopes, separated by a pipe (|)
 */
const FEATURE_SCOPE_SEGMENT = `\\((${FEATURE_SCOPE_OPTIONS.join("|")})(${FEATURE_SCOPE_OPTIONS.map(s => `\\|${s}`).join("|")})*\\)`;

/**
 * This regex combines the above segments to collectively enforce our naming template for pull requests aimed a development branch.
 */
const DEV_BRANCH_NAMING_REGEX = new RegExp(`^\\s*${FEATURE_TYPE_SEGMENT}\\s*${FEATURE_SCOPE_SEGMENT}\\s*:\\s*${WORK_ITEM_SEGMENT}\\s*:.+`, "i");

/**
 * This regex combines segments to collectively enforce our naming template for pull requests aimed at a release branch.
 */
const RELEASE_BRANCH_NAMING_REGEX = new RegExp(`\\s*RELEASE\\s*:\\s*${WORK_ITEM_SEGMENT}\\s*:.+`, "i");

/**
 * Returns true if the provided string is an acceptable title for a PR aimed at a development branch.
 * (e.g., `docdev`, `dev`)
 * @param title
 */
export function verifyDevBranchPrTitle(title: string): boolean {
	return DEV_BRANCH_NAMING_REGEX.test(title);
}

/**
 * Returns true if the provided string is an acceptable title for a PR aimed at a release branch.
 * (e.g., `documentation`, `release`)
 * @param title
 */
export function verifyReleaseBranchPrTitle(title: string): boolean {
	return RELEASE_BRANCH_NAMING_REGEX.test(title);
}
