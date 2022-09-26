package com.salesforce.exception;

/**
 * Thrown by long running methods if {@link Thread#interrupted()} returns true and processing should
 * stop. Any long running methods should periodically invoke {@link Thread#interrupted()} and throw
 * this exception if appropriate.
 */
public final class SfgeInterruptedException extends SfgeRuntimeException {
    public SfgeInterruptedException() {
        super();
    }
}
