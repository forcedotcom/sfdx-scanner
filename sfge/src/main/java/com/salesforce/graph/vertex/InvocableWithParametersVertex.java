package com.salesforce.graph.vertex;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.MethodInvocationScope;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

// TODO: Reevaluate InvocableWithParametersVertex hierarchy to determine if subclasses other
// than MethodCallExpression need to return values for #toList, getLast
public abstract class InvocableWithParametersVertex extends ChainedVertex
        implements InvocableVertex {
    private static final Logger LOGGER = LogManager.getLogger(InvocableWithParametersVertex.class);

    private final LazyVertexList<ChainedVertex> parameters;

    /**
     * @param traversalParameterModifier A consumer that modifies the traversal in order to load the
     *     parameters
     */
    protected InvocableWithParametersVertex(
            Map<Object, Object> properties,
            Consumer<GraphTraversal<Vertex, Vertex>> traversalParameterModifier) {
        super(properties);
        this.parameters = _getInvocationParameters(traversalParameterModifier);
    }

    public Optional<InvocableVertex> getNext() {
        return Optional.empty();
    }

    @Override
    public List<InvocableVertex> firstToList() {
        return Collections.singletonList(this);
    }

    @Override
    public InvocableVertex getLast() {
        return this;
    }

    @Override
    public MethodInvocationScope resolveInvocationParameters(
            MethodVertex methodVertex, SymbolProvider symbols) {
        TreeMap<String, Pair<Typeable, ApexValue<?>>> apexValueParameters =
                CollectionUtil.newTreeMap();

        List<ParameterVertex> declaredParameters = methodVertex.getParameters();
        if (declaredParameters.size() != getParameters().size()) {
            // This method should only be called if the methods match
            throw new UnexpectedException(methodVertex);
        }

        BaseSFVertex parent = null;
        for (int i = 0; i < declaredParameters.size(); i++) {
            ParameterVertex declaredParameter = declaredParameters.get(i);
            ChainedVertex invokedParameter = getParameters().get(i);
            // Handle doSomething((Account)a);
            // The actual parameter is contained within the CastExpressionVertex
            if (invokedParameter instanceof CastExpressionVertex) {
                invokedParameter = ((CastExpressionVertex) invokedParameter).getCastedVertex();
            }

            String parameterName = declaredParameter.getName();
            // Use the type declared by the method. This is more reliable than the variable passed
            // in
            Typeable parameterType = declaredParameter;

            // Try to resolve the invocation parameter to something more specific
            ChainedVertex resolvedValue =
                    symbols.getValue(invokedParameter).orElse(invokedParameter);
            ApexValue<?> apexValue =
                    ScopeUtil.resolveToApexValue(symbols, invokedParameter).orElse(null);
            if (apexValue == null) {
                apexValue =
                        ApexValueBuilder.get(symbols)
                                .valueVertex(resolvedValue)
                                .buildOptional()
                                .orElse(null);
            }

            if (apexValue == null) {
                String symbolicName = invokedParameter.getSymbolicName().orElse(null);
                if (symbolicName != null) {
                    apexValue = symbols.getApexValue(symbolicName).orElse(null);
                }
            }

            if (parameterName == null) {
                if (parent == null) {
                    // Lazy load this one time if required
                    parent = methodVertex.getParent();
                }
                if (methodVertex.isConstructor() && parent instanceof UserExceptionMethodsVertex) {
                    // User exception constructors that are auto generated don't have parameter
                    // names. There is
                    // currently not enough information on the Jorje classes to fix this in the AST.
                    // Synthesize these,
                    // the name doesn't matter since they won't ever be used.
                    parameterName = "arg" + i;
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                                "Generating parameter name for exception constructor. method="
                                        + methodVertex);
                    }
                } else {
                    throw new UnexpectedException(
                            "Method parameter doesn't have a name. method="
                                    + methodVertex
                                    + ", i="
                                    + i);
                }
            }
            apexValueParameters.put(parameterName, Pair.of(parameterType, apexValue));
        }

        return new MethodInvocationScope(this, apexValueParameters);
    }

    @Override
    public List<ChainedVertex> getParameters() {
        return this.parameters.get();
    }

    /**
     * Parameters are dependent upon the concrete class. For instance NewObjectExpression is all
     * paramters, MethodCallExpression is all children > 0
     */
    private final LazyVertexList<ChainedVertex> _getInvocationParameters(
            Consumer<GraphTraversal<Vertex, Vertex>> traversalParameterModifier) {
        return new LazyVertexList<>(
                () -> {
                    GraphTraversal<Vertex, Vertex> traversal = g().V(getId());
                    // Call the consumer provided by the subclass. This consumer knows the
                    // particulars of the way it's parameters are defined
                    traversalParameterModifier.accept(traversal);
                    return traversal;
                });
    }
}
