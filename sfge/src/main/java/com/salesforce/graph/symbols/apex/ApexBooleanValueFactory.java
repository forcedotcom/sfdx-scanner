package com.salesforce.graph.symbols.apex;

import com.salesforce.graph.symbols.MethodCallApexValueBuilder;
import java.util.Optional;

/** Generates {@link ApexBooleanValue}s from methods. */
public final class ApexBooleanValueFactory {
    private static final String IS_RUNNING_TEST = "Test.IsRunningTest";
    public static final MethodCallApexValueBuilder METHOD_CALL_BUILDER_FUNCTION =
            (g, vertex, symbols) -> {
                String fullMethodName = vertex.getFullMethodName();

                ApexValueBuilder builder =
                        ApexValueBuilder.getWithoutSymbolProvider().returnedFrom(null, vertex);

                // Test.IsRunningTest
                if (IS_RUNNING_TEST.equalsIgnoreCase(fullMethodName)) {
                    // We are only interested in paths that execute in a production context. Return
                    // false to force
                    // comparisons checking this value to be skipped.
                    return Optional.of(builder.valueVertex(vertex).buildBoolean(false));
                }

                return Optional.empty();
            };

    private ApexBooleanValueFactory() {}
}
