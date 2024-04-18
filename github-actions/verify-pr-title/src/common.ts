/**
 * This regex portion matches @W-0000@ through @W-999999999@,
 * to enforce that the title contains a work record number consumable
 * by Git2Gus.
 * All pull request titles will require this segment.
 */
export const WORK_ITEM_PORTION = "@W-\\d{4,9}@"

/**
 * This regex matches one or more space, comma, period, n-dash,
 * colon, or semicolon characters. All pull request titles allow these
 * characters as separators between portions
 */
export const SEPARATOR = "[ -.,;:]+";

