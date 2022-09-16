package com.salesforce.exception;

/** Indicates a developer error where the same key was added to a map twice */
public final class DuplicateKeyException extends SfgeRuntimeException {
    public DuplicateKeyException(Object key, Object previousEntry, Object newEntry) {
        super("Duplicate keys. key=" + key + ", previous=" + previousEntry + ", new=" + newEntry);
    }
}
