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
            "Remove unreachable code to proceed with the analysis: %s,%s:%d";

    public static final String VARIABLE_DECLARED_MULTIPLE_TIMES =
            "Rename or remove reused variable to proceed with analysis: %s,%s:%d";

    /** CRUD/FLS Violation messages * */
    // format: "CRUD" or "FLS", DML operation, Object type, Field information
    public static final String VIOLATION_MESSAGE_TEMPLATE =
            "%1$s validation is missing for [%2$s] operation on [%3$s]%4$s.";

    public static final String STRIP_INACCESSIBLE_READ_WARNING_TEMPLATE =
            "For stripInaccessible checks on READ operation, Salesforce Graph Engine can't verify that only sanitized data is used after the check. Discard unsanitized data for [%2$s].";

    public static final String UNRESOLVED_CRUD_FLS_TEMPLATE =
            "Salesforce Graph Engine couldn't resolve the parameter passed to [%2$s] operation%4$s. Confirm that this operation has the necessary %1$s checks.";

    public static final String FIELDS_MESSAGE_TEMPLATE = " with field(s) [%s]";
    public static final String FIELD_HANDLING_NOTICE =
            ". Confirm that the objects and fields involved in these segments have FLS checks: [%s]";

    public static final String INVALID_SYNTAX_TEMPLATE = "Invalid syntax at %d:%d. (%s)";

    public static final String FIX_COMPILATION_ERRORS = "Fix compilation errors in %s and retry";

    public static final String EXCEPTION_FORMAT_TEMPLATE = "%s, Caused by:\n%s";
}
