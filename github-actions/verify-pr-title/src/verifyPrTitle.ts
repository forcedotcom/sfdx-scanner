/**
 * This is a regex segment that matches the accepted options for PR type.
 * NOTE: This segment allows for exactly one type.
 */
const TYPE_SEGMENT = "(NEW|FIX|CHANGE)";

/**
 * These are all of the possible options for PR Scope.
 */
const SCOPE_OPTIONS = [
	"CodeAnalyzer",
	"CPD",
	"ESLint",
	"GraphEngine",
	"PMD",
	"RetireJS"
];

/**
 * This is a regex segment that matches the accepted options for PR scope, enclosed
 * in parentheses.
 * NOTE: This segment allows for multiple scopes, separated by a pipe (|)
 */
const SCOPE_SEGMENT = `\\((${SCOPE_OPTIONS.join("|")})(${SCOPE_OPTIONS.map(s => `\\|${s}`).join("|")})*\\)`;

/**
 * This regex portion matches @W-0000@ through @W-999999999@,
 * to enforce that the title contains a work record number consumable
 * by Git2Gus.
 */
const WORK_ITEM_SEGMENT = "@W-\\d{4,8}@";

/**
 * This regex combines the above segments to collectively enforce our naming template.
 */
const NAMING_REGEX = new RegExp(`^${TYPE_SEGMENT} ${SCOPE_SEGMENT}: ${WORK_ITEM_SEGMENT}: .+`);

/**
 * Verifies that PR title conforms to our naming template.
 * @param title
 */
export function verifyPrTitleMatchesTemplate(title: string): boolean {
	return NAMING_REGEX.test(title);
}
