package com.salesforce.graph.ops.expander;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.symbols.apex.ApexValue;
import java.util.Optional;

/** Thrown when a {@link ApexReturnValuePathCollapser} determines that a path is not valid */
final class ReturnValueInvalidCollapsedException extends ApexPathExpanderException {
    private final ForkEvent forkEvent;
    private final ApexPath path;
    private final Optional<ApexValue<?>> returnValue;

    ReturnValueInvalidCollapsedException(
            ForkEvent forkEvent, ApexPath path, Optional<ApexValue<?>> returnValue) {
        this.forkEvent = forkEvent;
        this.path = path;
        this.returnValue = returnValue;
    }

    Optional<ForkEvent> getForkEvent() {
        return Optional.ofNullable(forkEvent);
    }

    ApexPath getPath() {
        return path;
    }

    Optional<ApexValue<?>> getReturnValue() {
        return returnValue;
    }
}
