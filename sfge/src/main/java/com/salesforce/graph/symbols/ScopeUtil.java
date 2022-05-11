package com.salesforce.graph.symbols;

import com.salesforce.graph.symbols.apex.ApexBooleanValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BooleanExpressionVertex;
import com.salesforce.graph.vertex.CastExpressionVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.PrefixExpressionVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScopeUtil {
    private static final Logger LOGGER = LogManager.getLogger(ScopeUtil.class);

    /**
     * Return value a more specific value if possible. It may return the same value if {@code value}
     * can't be resolved any further.
     *
     * @deprecated use {@link #resolveToApexValue}
     */
    @Deprecated
    public static ChainedVertex recursivelyResolve(SymbolProvider scope, ChainedVertex value) {
        if (value != null && value.isResolvable()) {
            if (value instanceof VariableExpressionVertex.ForLoop) {
                VariableExpressionVertex.ForLoop forLoopExpression =
                        (VariableExpressionVertex.ForLoop) value;
                ChainedVertex values = forLoopExpression.getForLoopValues();
                Optional<ChainedVertex> resolved = scope.getValue(values);
                if (resolved.isPresent()) {
                    return forLoopExpression.cloneWithResolvedValues(resolved.get());
                }
            } else {
                String symbolicName = value.getSymbolicName().orElse(null);
                if (symbolicName != null) {
                    ChainedVertex resolved = scope.getValue(symbolicName).orElse(null);
                    if (resolved != null) {
                        // Intentionally using reference equality
                        if (resolved == value) {
                            // It is the same object
                            return value;
                        } else {
                            return scope.getValue(resolved).orElse(resolved);
                        }
                    }
                }
            }
        }
        return value;
    }

    /**
     * Attempt to resolve the vertex to an ApexValue, or build an {@link
     * com.salesforce.graph.symbols.apex.ApexSingleValue} if the vertex doesn't resolve to anything.
     */
    public static ApexValue<?> resolveToApexValueOrBuild(
            ApexValueBuilder builder, ChainedVertex vertex) {
        // TODO: Should this set the status to Indeterminant?
        return resolveToApexValue(builder, vertex)
                .orElseGet(() -> builder.deepClone().valueVertex(vertex).buildUnknownType());
    }

    public static Optional<ApexValue<?>> resolveToApexValue(
            SymbolProvider symbols, ChainedVertex vertex) {
        return resolveToApexValue(ApexValueBuilder.get(symbols), vertex);
    }

    @SuppressWarnings(
            "PMD.AvoidReassigningParameters") // TODO: move to visitor-based instanceof check
    public static Optional<ApexValue<?>> resolveToApexValue(
            ApexValueBuilder builderParam, ChainedVertex vertex) {
        ApexValue<?> resolved = null;
        final SymbolProvider symbols = builderParam.getSymbolProvider();
        // deepClone since this code modifies the builder and the caller might use it for another
        // purpose
        final ApexValueBuilder builder = builderParam.deepClone();

        // Look at the first child of a negated expression and negate the result if the result is a
        // determinant boolean
        boolean negated = false;
        if (vertex instanceof PrefixExpressionVertex) {
            PrefixExpressionVertex prefixExpression = (PrefixExpressionVertex) vertex;
            if (prefixExpression.isOperatorNegation()) {
                negated = true;
                vertex = prefixExpression.getOnlyChild();
            }
        }

        // We are interested in resolving the item within the cast
        while (vertex instanceof CastExpressionVertex) {
            vertex = vertex.getOnlyChild();
        }

        if (vertex instanceof InvocableVertex) {
            resolved = symbols.getReturnedValue((InvocableVertex) vertex).orElse(null);
        }

        if (resolved == null && vertex instanceof VariableExpressionVertex) {
            resolved = symbols.getApexValue((VariableExpressionVertex) vertex).orElse(null);
        } else if (vertex instanceof LiteralExpressionVertex
                || vertex instanceof BooleanExpressionVertex) {
            resolved = builder.valueVertex(vertex).build();
        }

        if (resolved == null) {
            resolved = getDefaultPropertyValue(vertex, symbols).orElse(null);
        }

        if (resolved == null) {
            resolved = builder.valueVertex(vertex).buildOptional().orElse(null);
        }

        if (negated && resolved instanceof ApexBooleanValue && resolved.isValuePresent()) {
            ApexBooleanValue apexBooleanValue = (ApexBooleanValue) resolved;
            resolved =
                    builder.returnedFrom(
                                    resolved.getReturnedFrom().orElse(null),
                                    resolved.getInvocable().orElse(null))
                            .valueVertex(vertex)
                            .buildBoolean(!apexBooleanValue.getValue().get());
        }

        return Optional.ofNullable(resolved);
    }

    /** Attempts to generate a default value if the vertex represents a property or a field. */
    public static Optional<ApexValue<?>> getDefaultPropertyValue(
            BaseSFVertex vertex, SymbolProvider symbols) {
        String name;
        if (vertex instanceof VariableExpressionVertex) {
            name = ((VariableExpressionVertex) vertex).getFullName();
        } else {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Vertex type not handled for getDefaultValue(). vertex=" + vertex);
            }
            return Optional.empty();
        }

        String[] keys = name.split("\\.");

        // Get apex value for name if available
        final ApexValue<?> result = symbols.getApexValue(keys[0]).orElse(null);

        if (keys.length > 1) {
            final String propertyName = keys[1];
            if (keys.length > 2) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(
                            "TODO: PathScopeVisitor.getApexValue() can currently only support chains of length 2 or lower. keySequence="
                                    + keys);
                }
            } else if (result instanceof ObjectProperties) {
                final ObjectProperties objectProperties = (ObjectProperties) result;
                final ApexValue<?> defaultValue =
                        objectProperties.getOrAddDefault(propertyName).orElse(null);
                return Optional.ofNullable(defaultValue);
            }
        }

        return Optional.empty();
    }
}
