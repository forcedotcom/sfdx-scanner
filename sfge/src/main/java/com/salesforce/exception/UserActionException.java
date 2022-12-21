package com.salesforce.exception;

/**
 * Denotes exceptions caused by incorrect code. Code may have compiled but user needs to take an
 * action to fix their code.
 */
public final class UserActionException extends SfgeRuntimeException {
    public UserActionException(String message) {
        super(message);
    }

    /**
     * Construct structured user action exception
     *
     * @param messageTemplate typically from {@link com.salesforce.config.UserFacingMessages}
     * @param filename where the issue was noticed
     * @param definingType class name to fix
     * @param lineNumber line number where the problem is
     */
    public UserActionException(
            String messageTemplate, String filename, String definingType, int lineNumber) {
        this(String.format(messageTemplate, filename, definingType, lineNumber));
    }
}
