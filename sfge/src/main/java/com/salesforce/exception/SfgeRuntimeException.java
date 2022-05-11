package com.salesforce.exception;

public abstract class SfgeRuntimeException extends RuntimeException {
    public SfgeRuntimeException() {
        super();
    }

    public SfgeRuntimeException(Throwable cause) {
        super(cause);
    }

    public SfgeRuntimeException(String msg) {
        super(msg);
    }

    public SfgeRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
