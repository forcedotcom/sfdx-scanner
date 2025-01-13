import {SEPARATOR, WORK_ITEM_PORTION} from "./common";

/**
 * This regex portion matches the accepted options for a Feature Branch PR type.
 * NOTE: Only one type may be selected at a time.
 */
const PR_TYPE_PORTION = "(NEW|FIX|CHANGE)"

/**
 * This regex portion matches any string wrapped in parentheses without additional
 * parentheses inside it.
 */
const SCOPE_PORTION = "\\([^()]+\\)";

/**
 * This RegExp matches the title format for Feature Branch pull requests,
 * i.e., a PR aimed at {@code dev-4} or a {@code release-x.y.z} branch, not
 * coming from {@code main-4}.
 */
const FEATURE_PR_REGEX = new RegExp(`^${PR_TYPE_PORTION}${SEPARATOR}${SCOPE_PORTION}${SEPARATOR}${WORK_ITEM_PORTION}${SEPARATOR}[^\\s]+.*`, "i");

/**
 * Verifies that the provided string is an acceptable title for a PR
 * aimed at {@code dev-4} or a {@code release-x.y.z} branch, not coming
 * from {@code main-4}.
 * @param title
 */
export function verifyFeaturePrTitle(title: string): boolean {
	return FEATURE_PR_REGEX.test(title);
}
