package com.salesforce.graph.ops.expander;

/**
 * Thrown by {@link ApexReturnValuePathCollapser} to indicate that a return value should be excluded
 */
public class ReturnValueInvalidException extends ApexPathExpanderException {
    ReturnValueInvalidException(String message) {
        super(message);
    }
}
