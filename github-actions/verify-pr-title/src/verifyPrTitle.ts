// Matches @W-0000@ through @W-99999999@
const re = /@W-\d{4,8}@/;

/**
 * Verifies title conforms to the pattern required by git2gus.
 * git2gus requires the WorkItemId to be enclosed by two '@' signs.
 * A valid WorkItemId consists of 'W-' followed by 4-8 digits.
 */
export function verifyPRTitle(title: string): boolean {
	return re.test(title);
}
