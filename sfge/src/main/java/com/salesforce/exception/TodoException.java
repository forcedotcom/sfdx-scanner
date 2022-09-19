package com.salesforce.exception;

public final class TodoException extends SfgeRuntimeException {
    public TodoException() {
        this("TODO");
    }

    public TodoException(Object obj) {
        super(obj.toString());
    }

    public TodoException(String message) {
        super(message);
    }

    public TodoException(String message, Throwable cause) {
        super(message, cause);
    }
}
