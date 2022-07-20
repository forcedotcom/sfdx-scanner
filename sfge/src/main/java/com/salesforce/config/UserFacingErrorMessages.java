package com.salesforce.config;

/**
 * Contains error message constants that will be displayed to users. TODO: move all other
 * user-facing messages here.
 */
public final class UserFacingErrorMessages {

    /** UserActionException * */

    // format: filename,defined type, line number
    public static final String UNREACHABLE_CODE =
            "Please remove unreachable code to proceed with analysis: %s,%s:%d";
}
