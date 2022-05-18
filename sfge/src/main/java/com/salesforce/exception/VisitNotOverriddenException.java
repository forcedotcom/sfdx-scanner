package com.salesforce.exception;

import com.salesforce.graph.vertex.BaseSFVertex;

/**
 * Thrown by abstract classes to indicate that the concrete implementation is missing. The visitor
 * pattern relies on compile time dispatch to the correct overloaded visitor method in order for the
 * pattern to work.
 */
public class VisitNotOverriddenException extends SfgeRuntimeException {
    public VisitNotOverriddenException() {
        this("Concrete classes need to override visit or else the visitor won't work.");
    }

    public VisitNotOverriddenException(String message) {
        super(message);
    }

    public VisitNotOverriddenException(BaseSFVertex vertex) {
        super(
                "Concrete classes need to override visit or else the visitor won't work."
                        + " class="
                        + vertex.getClass().getSimpleName());
    }
}
