package com.salesforce.graph.symbols;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.UserClassVertex;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Parses a class instance for variables, both final and non-final. Currently only final variables
 * are given access, but we need to track non-final in order to determine error conditions where an
 * uninitialized variable is referred to.
 */
public abstract class AbstractClassInstanceScope extends AbstractClassScope {
    private static final Logger LOGGER = LogManager.getLogger(AbstractClassInstanceScope.class);

    private final HashMap<InvocableVertex, Map<ChainedVertex, ChainedVertex>>
            resolvedParametersAtTimeOfMethodInvocation;

    protected AbstractClassInstanceScope(GraphTraversalSource g, UserClassVertex userClass) {
        super(g, userClass);
        this.resolvedParametersAtTimeOfMethodInvocation = new HashMap<>();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Created. name=" + getClassName());
        }
    }

    protected AbstractClassInstanceScope(AbstractClassInstanceScope other) {
        super(other);
        this.resolvedParametersAtTimeOfMethodInvocation =
                CloneUtil.cloneHashMap(other.resolvedParametersAtTimeOfMethodInvocation);
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public Optional<AbstractClassInstanceScope> getClosestClassInstanceScope() {
        return Optional.of(this);
    }

    @Override
    public Optional<ChainedVertex> getValue(String key) {
        // Any locally defined values will override final fields. Delegate to super first
        Optional<ChainedVertex> result = super.getValue(key);

        if (!result.isPresent() && finalFields.containsKey(key)) {
            result = Optional.ofNullable(finalFields.get(key));
        }

        return result;
    }

    /** Only resolve values that refer to instance variables. Do not defer to super */
    public Optional<ApexValue<?>> getInstanceApexValue(String key) {
        return Optional.ofNullable(apexValues.get(key));
    }

    public ApexValue<?> updateInstanceApexValue(String key, ApexValue<?> value) {
        if (fieldsWithSetterBlock.contains(key)) {
            // Fields with a defined method will set the value when the method is executed. It may
            // contain more complex
            // logic than a simple assignment
            return null;
        } else {
            return apexValues.put(key, value);
        }
    }

    @Override
    public ChainedVertex getValueAtTimeOfInvocation(InvocableVertex vertex, ChainedVertex value) {
        if (resolvedParametersAtTimeOfMethodInvocation.containsKey(vertex)) {
            Map<ChainedVertex, ChainedVertex> resolvedParameters =
                    resolvedParametersAtTimeOfMethodInvocation.get(vertex);
            if (resolvedParameters.containsKey(value)) {
                return resolvedParameters.get(value);
            }
        }
        return value;
    }

    @Override
    public ApexValue<?> updateVariable(
            String key, ApexValue<?> value, SymbolProvider currentScope) {
        ApexValue<?> oldValue;
        if (fieldsWithSetterBlock.contains(key)) {
            // Ignore this, it will be updated in #updateProperty
            oldValue = null;
        } else if (finalFieldsInitializedInline.contains(key)) {
            throw new UnexpectedException(
                    "Variable is final. key=" + key + ", value=" + getApexValue(key));
        } else if (finalFields.containsKey(key)) {
            ChainedVertex chainedVertex = value.getValueVertex().orElse(null);
            if (chainedVertex instanceof MethodCallExpressionVertex) {
                for (InvocableVertex invocable : ((InvocableVertex) chainedVertex).firstToList()) {
                    for (int i = 0; i < invocable.getParameters().size(); i++) {
                        ChainedVertex parameter = invocable.getParameters().get(i);
                        // Ask the current scope to resolve the value. This could be an inner scope
                        // that
                        // will be popped.
                        ChainedVertex resolved = currentScope.getValue(parameter).orElse(parameter);
                        if (!resolved.equals(parameter)) {
                            Map<ChainedVertex, ChainedVertex> resolvedParameters =
                                    resolvedParametersAtTimeOfMethodInvocation.computeIfAbsent(
                                            invocable, k -> new HashMap<>());
                            resolvedParameters.put(parameter, resolved);
                        }
                    }
                }
            }
            oldValue = apexValues.put(key, value);
        } else if (nonFinalFields.containsKey(key)) {
            oldValue = apexValues.put(key, value);
        } else if (getInheritedScope().get().getTypedVertex(key).isPresent()) {
            // This is a class static variable
            oldValue = getInheritedScope().get().updateVariable(key, value, currentScope);
        } else {
            // The code is overwriting a variable passed to the constructor
            oldValue = methodInvocationStack.peek().updateVariable(key, value, currentScope);
        }
        return oldValue;
    }

    @Override
    protected Optional<PathScopeVisitor> getInheritedScope() {
        // The inherited scope of a class instance is the static class
        return Optional.of(
                ContextProviders.CLASS_STATIC_SCOPE
                        .get()
                        .getClassStaticScope(getClassName())
                        .get());
    }
}
