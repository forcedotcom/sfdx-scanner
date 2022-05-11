package com.salesforce.exception;

import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import javax.annotation.Nullable;

/**
 * Indicates a bug in the sfge code. Thrown by collection classes when we detect that a list is
 * being added to itself or a map is being added as a key to itself.
 */
public final class CircularReferenceException extends SfgeRuntimeException {
    public CircularReferenceException(
            ApexValue<?> apexValue, @Nullable InvocableWithParametersVertex vertex) {
        super("ApexValue=" + apexValue + ", vertex=" + vertex);
    }
}
