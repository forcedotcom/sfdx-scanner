package com.salesforce.graph.ops;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.AbstractClassInstanceScope;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SymbolProviderUtil {
    private static final Logger LOGGER = LogManager.getLogger(SymbolProviderUtil.class);

    /**
     * Iterate through the sub expressions, providing a mapping of the original value to the most
     * specific value that is available from the symbol provider.
     */
    // TODO: Common code with below
    public static Map<InvocableVertex, Map<ChainedVertex, ChainedVertex>> resolveParameters(
            SymbolProvider symbols, InvocableVertex originalVertex) {
        Map<InvocableVertex, Map<ChainedVertex, ChainedVertex>> results = new HashMap<>();

        for (InvocableVertex invocable : originalVertex.firstToList()) {
            for (ChainedVertex parameter : invocable.getParameters()) {
                if (parameter.isResolvable()) {
                    String symbolicName = parameter.getSymbolicName().orElse(null);
                    if (symbolicName != null) {
                        ChainedVertex resolved = symbols.getValue(symbolicName).orElse(null);
                        if (resolved != null) {
                            Map<ChainedVertex, ChainedVertex> mapping =
                                    results.computeIfAbsent(invocable, k -> new HashMap<>());
                            ChainedVertex previous = mapping.put(parameter, resolved);
                            if (previous != null && !previous.equals(resolved)) {
                                throw new UnexpectedException(
                                        "Conflicting resolution. symbolicName="
                                                + symbolicName
                                                + ", previous="
                                                + previous
                                                + ", conflict="
                                                + resolved);
                            }
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info(
                                        "Resolved. line="
                                                + parameter.getBeginLine()
                                                + ", symbolicName="
                                                + symbolicName
                                                + ", resolved="
                                                + resolved);
                            }
                        }
                    }
                }
            }
        }

        // Add the invocation mapping
        Map<InvocableVertex, Map<ChainedVertex, ChainedVertex>> invocations =
                resolveInvokedParameters(symbols, originalVertex.firstToList());
        for (Map.Entry<InvocableVertex, Map<ChainedVertex, ChainedVertex>> invocationMapping :
                invocations.entrySet()) {
            Map<ChainedVertex, ChainedVertex> parameters =
                    results.computeIfAbsent(invocationMapping.getKey(), k -> new HashMap<>());
            for (Map.Entry<ChainedVertex, ChainedVertex> parameterMapping :
                    invocationMapping.getValue().entrySet()) {
                ChainedVertex resolved = parameterMapping.getValue();
                ChainedVertex previous = parameters.put(parameterMapping.getKey(), resolved);
                if (previous != null && !previous.equals(parameterMapping.getValue())) {
                    throw new UnexpectedException(
                            "Conflicting resolution. previous="
                                    + previous
                                    + ", conflict="
                                    + resolved);
                }
            }
        }

        return results;
    }

    /**
     * An invocable parameter called from a constructor may have been passed to a method. For
     * example the following constructor will invoked via new FLSClass('Account'), will keep track
     * of the fact that VariableExpression(objectName) points to LiteralExpression('Account') at the
     * time that Schema.getGlobalDescribe().get(objectName) is invoked.
     *
     * <p>public FLSClass(String objectName) { m =
     * Schema.getGlobalDescribe().get(objectName).getDescribe().getMap(); }
     *
     * <p>The {@link AbstractClassInstanceScope} keeps track of these values and will resolve the
     * parameter at a future time when m is invoked. We could mutate the call in the ClassScope, but
     * it is currently a goal to not mutate the vertices after they are first created
     */
    public static Map<InvocableVertex, Map<ChainedVertex, ChainedVertex>> resolveInvokedParameters(
            SymbolProvider symbols, List<InvocableVertex> vertices) {
        Map<InvocableVertex, Map<ChainedVertex, ChainedVertex>> results = new HashMap<>();

        for (InvocableVertex invocable : vertices) {
            for (int i = 0; i < invocable.getParameters().size(); i++) {
                ChainedVertex parameter = invocable.getParameters().get(i);
                ChainedVertex resolved = symbols.getValueAtTimeOfInvocation(invocable, parameter);
                // Reference equality is intentional
                if (parameter != resolved) {
                    Map<ChainedVertex, ChainedVertex> mapping =
                            results.computeIfAbsent(invocable, k -> new HashMap<>());
                    mapping.put(parameter, resolved);
                }
            }
        }

        return results;
    }

    private SymbolProviderUtil() {}
}
