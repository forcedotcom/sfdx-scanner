package com.salesforce.config;

/**
 * Contains error message constants that will be displayed to users. TODO: move all other
 * user-facing messages here.
 */
public final class UserFacingMessages {

    /** Main args and process checks * */
    public static final String REQUIRES_AT_LEAST_ONE_ARGUMENT =
            "SFGE invocation requires at least one argument.";

    public static final String UNRECOGNIZED_ACTION = "Unrecognized action to invoke SFGE: %s.";
    public static final String INCORRECT_ARGUMENT_COUNT =
            "Wrong number of arguments. Expected %d; received %d";

    /** UserActionException * */

    // format: filename,defined type, line number
    public static final String UNREACHABLE_CODE =
            "Remove unreachable code to proceed with analysis: %s,%s:%d";

    /** CRUD/FLS Violation messages * */
    // format: "CRUD" or "FLS", DML operation, Object type, Field information
    public static final String VIOLATION_MESSAGE_TEMPLATE =
            "%1$s validation is missing for [%2$s] operation on [%3$s]%4$s";

    public static final String STRIP_INACCESSIBLE_READ_WARNING_TEMPLATE =
            "For stripInaccessible checks on READ operation, "
                    + "SFGE doesn't have the capability to verify that only sanitized data is used after the check."
                    + "Please confirm that unsanitized data is discarded for [%2$s]";

    public static final String UNRESOLVED_CRUD_FLS_TEMPLATE =
            "SFGE couldn't resolve the parameter passed to [%2$s] operation%4$s. "
                    + "Please confirm that this operation has the necessary %1$s checks";

    public static final String FIELDS_MESSAGE_TEMPLATE = " with field(s) [%s]";
    public static final String FIELD_HANDLING_NOTICE =
            " - SFGE may not have parsed some objects/fields correctly. Please confirm that the objects/fields involved in these segments have FLS checks: [%s]";
}
