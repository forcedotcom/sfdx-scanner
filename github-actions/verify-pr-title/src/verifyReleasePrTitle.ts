import {SEPARATOR, WORK_ITEM_PORTION} from "./common";

/**
 * This regex portion matches the accepted options for a Release Branch PR type.
 * NOTE: Only one type may be selected at a time.
 */
const PR_TYPE_PORTION = "RELEASE"

/**
 * This RegExp matches the title format for Release Branch pull requests,
 * i.e., a PR aimed at the {@code release} or {@code main} branches.
 */
const RELEASE_PR_REGEX = new RegExp(`^${PR_TYPE_PORTION}${SEPARATOR}${WORK_ITEM_PORTION}${SEPARATOR}[^\\s]+.*`, "i");

/**
 * Verifies that the provided string is an acceptable title for a PR
 * aimed at the {@code release} or {@code main} branches.
 * @param title
 */
export function verifyReleasePrTitle(title: string): boolean {
	return RELEASE_PR_REGEX.test(title);
}
