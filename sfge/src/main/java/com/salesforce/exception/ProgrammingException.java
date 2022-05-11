package com.salesforce.exception;

/** Indicates a programming logic error, such as an item being initialized more than once. */
public class ProgrammingException extends SfgeRuntimeException {
    public ProgrammingException(String message) {
        super(message);
    }

    public ProgrammingException(Object obj) {
        super(obj != null ? obj.toString() : "<null>");
    }
}
