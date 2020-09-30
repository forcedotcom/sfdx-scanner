// Matches @W-0000@ through @W-99999999@
const reBugId = /@W-\d{4,8}@/;

// Matches r/ R/ d/ D/
const reBranch = /^[rdRD]\//;


/**
 * Verifies title conforms to the pattern required by git2gus.
 * git2gus requires the WorkItemId to be enclosed by two '@' signs.
 * A valid WorkItemId consists of 'W-' followed by 4-8 digits.
 */
export function verifyPRTitleForBugId(title: string): boolean {
	return reBugId.test(title);
}

/**
 * Verifies that the Title does not begin with d/ or r/ to prevent
 * titles that look like d/W-xxx or r/W-xxx
 */
export function verifyPRTitleForBadTitle(title: string): boolean {
	return !reBranch.test(title);
}
