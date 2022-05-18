package com.salesforce.graph.symbols.apex;

import com.salesforce.graph.symbols.MethodCallApexValueBuilder;
import com.salesforce.graph.symbols.ScopeUtil;
import java.util.Optional;

public final class ApexIdValueFactory {
    private static final String ID_VALUE_OF = "Id.valueOf";
    public static final MethodCallApexValueBuilder METHOD_CALL_BUILDER_FUNCTION =
            (g, vertex, symbols) -> {
                final String fullMethodName = vertex.getFullMethodName();

                final ApexValueBuilder builder =
                        ApexValueBuilder.get(symbols).returnedFrom(null, vertex);

                if (ID_VALUE_OF.equalsIgnoreCase(fullMethodName)) {
                    ApexValue.validateParameterSize(vertex, 1);
                    ApexValue<?> apexValue =
                            ScopeUtil.resolveToApexValueOrBuild(
                                    builder, vertex.getParameters().get(0));
                    return Optional.of(builder.buildId(apexValue));
                }

                return Optional.empty();
            };

    private ApexIdValueFactory() {}
}
