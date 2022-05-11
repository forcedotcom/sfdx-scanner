package com.salesforce.rules.fls.apex.operations;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.system.SObjectAccessDecision;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import java.util.Optional;

/** Validates if a given method is a stripInaccessible() call on Read access type */
public final class StripInaccessibleReadValidator {

    private StripInaccessibleReadValidator() {}

    public static Optional<ApexValue<?>> detectSanitization(
            MethodCallExpressionVertex methodCallExpressionVertex, SymbolProvider symbols) {
        final String fullMethodName = methodCallExpressionVertex.getFullMethodName();

        if ("Security.stripInaccessible".equalsIgnoreCase(fullMethodName)) {
            final Optional<ApexValue<?>> returnedValue =
                    symbols.getReturnedValue(methodCallExpressionVertex);

            if (returnedValue.isPresent() && returnedValue.get() instanceof SObjectAccessDecision) {
                final SObjectAccessDecision decisionValue =
                        (SObjectAccessDecision) returnedValue.get();
                // Check if the accessType is Read
                if (FlsConstants.StripInaccessibleAccessType.READABLE.equals(
                        decisionValue.getAccessType().orElse(null))) {
                    return Optional.of(decisionValue.getSanitizableValue().orElse(null));
                }
            } else {
                throw new UnexpectedException(
                        "stripInaccessible() did not resolve to AccessDecision: "
                                + methodCallExpressionVertex);
            }
        }

        return Optional.empty();
    }
}
