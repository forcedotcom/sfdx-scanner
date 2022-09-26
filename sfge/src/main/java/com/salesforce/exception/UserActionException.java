package com.salesforce.exception;

/**
 * Denotes exceptions caused by incorrect code. Code may have compiled but user needs to take an
 * action to fix their code.
 */
public final class UserActionException extends SfgeRuntimeException {
    public UserActionException(String message) {
        super(message);
    }
}
