package com.salesforce.graph.symbols.apex;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.MethodCallApexValueBuilder;
import com.salesforce.graph.vertex.ChainedVertex;
import java.util.List;
import java.util.Optional;

/** Generates {@link ApexSoqlValue}s from methods. */
public final class ApexSoqlValueFactory {
    private static final String QUERY = "query";
    private static final String DATABASE = "database";
    public static final MethodCallApexValueBuilder METHOD_CALL_BUILDER_FUNCTION =
            (g, vertex, symbols) -> {
                String methodName = vertex.getMethodName();
                List<String> chainedNames = vertex.getChainedNames();
                if (QUERY.equalsIgnoreCase(methodName)
                        && chainedNames.size() == 1
                        && DATABASE.equalsIgnoreCase(chainedNames.get(0))) {
                    if (vertex.getParameters().isEmpty() || vertex.getParameters().size() > 2) {
                        throw new UnexpectedException(vertex);
                    }
                    // Do not resolve this parameter, it will be resolved in #setConcreteType
                    ChainedVertex parameter = vertex.getParameters().get(0);
                    return Optional.of(
                            ApexValueBuilder.get(symbols)
                                    .valueVertex(parameter)
                                    .returnedFrom(null, vertex)
                                    .buildSoql());
                }
                return Optional.empty();
            };

    private ApexSoqlValueFactory() {}
}
