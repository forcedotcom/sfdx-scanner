package com.salesforce.config;

/**
 * Contains error message constants that will be displayed to users. TODO: move all other
 * user-facing messages here.
 */
public final class UserFacingMessages {

    /** UserActionException * */

    // format: filename,defined type, line number
    public static final String UNREACHABLE_CODE =
            "Please remove unreachable code to proceed with analysis: %s,%s:%d";


    /** CRUD/FLS Violation messages **/
    public static final String VIOLATION_MESSAGE_TEMPLATE =
            "%1$s validation is missing for [%2$s] operation on [%3$s]";
    public static final String STRIP_INACCESSIBLE_READ_WARNING_TEMPLATE =
                            "For stripInaccessible checks on READ operation, "
                                    + "SFGE does not have the capability to verify that only sanitized data is used after the check."
                                    + "Please ensure that unsanitized data is discarded for [%2$s]";

    public static final String UNRESOLVED_CRUD_FLS_TEMPLATE = "SFGE was unable to resolve the parameter passed to [%2$s] operation. " +
        "Ensure manually that this operation has the necessary CRUD/FLS checks.";

    public static final String FIELDS_MESSAGE_TEMPLATE = " with field(s) [%s]";
    public static final String FIELD_HANDLING_NOTICE =
        " - SFGE may not have parsed some objects/fields correctly. Please confirm if the objects/fields involved in these segments have FLS checks: [%s]";

}
