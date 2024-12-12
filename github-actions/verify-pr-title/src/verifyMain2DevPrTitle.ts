import {SEPARATOR, WORK_ITEM_PORTION} from "./common";

/**
 * This regex portion matches the accepted Type options for a pull request
 * merging {@code main-4} (or an {@code m2d/*} branch) back into {@dev-4}.
 * NOTE: The only acceptable option for this is {@code MAIN2DEV}, with flexible
 * casing.
 */
const PR_TYPE_PORTION = "MAIN2DEV";

/**
 * This regex portion matches the accepted Descriptor portion of a pull request
 * title merging {@code main-4} (or an {@code m2d/*} branch) back into {@dev-4}.
 * It can contain anything, but its contents must include the words "merging" (as a
 * reminder that the PR must be merged with a merge commit as opposed to a squash or rebase),
 * and "vX.Y.Z", which should correspond to the new release.
 */
const DESCRIPTOR_PORTION = ".*merging.+\\d+\\.\\d+\\.\\d+.*";

/**
 * This RegExp matches the title format for pull requests merging {@code main-4} (or
 * an {@code m2d/*} branch) back into {@code dev-4}.
 */
const MAIN2DEV_PR_REGEX = new RegExp(`^${PR_TYPE_PORTION}${SEPARATOR}${WORK_ITEM_PORTION}${SEPARATOR}${DESCRIPTOR_PORTION}`, "i");

/**
 * Verifies that the provided string is an acceptable title for a PR
 * merging {@code main-4} (or an {@code m2d/*} branch) back into {@code dev-4}.
 * @param title
 */
export function verifyMain2DevPrTitle(title: string): boolean {
	return MAIN2DEV_PR_REGEX.test(title);
}
