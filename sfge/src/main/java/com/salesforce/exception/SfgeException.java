package com.salesforce.exception;

public abstract class SfgeException extends Exception {
    public SfgeException() {}

    public SfgeException(String msg) {
        super(msg);
    }

    public SfgeException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
