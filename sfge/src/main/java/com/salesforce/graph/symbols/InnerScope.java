package com.salesforce.graph.symbols;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
import com.salesforce.graph.vertex.ReturnStatementVertex;
import com.salesforce.graph.vertex.ThisVariableExpressionVertex;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** Represents any type of inner scope such as a BlockStatement, ForStatement etc. */
public final class InnerScope extends PathScopeVisitor implements DeepCloneable<InnerScope> {
    private static final Logger LOGGER = LogManager.getLogger(InnerScope.class);

    /** The current ReturnStatementVertex if we are processing a return statement */
    private ReturnStatementVertex returnStatement;

    /**
     * The ApexValue that is returned from a new ObjectExpression. These are transient. So they must
     * be captured.
     */
    private final HashMap<NewObjectExpressionVertex, ApexValue<?>> newObjectExpressions;

    /** Maintained to aid in debugging */
    private final BaseSFVertex firstVertex;

    public InnerScope(
            GraphTraversalSource g, PathScopeVisitor inheritedScope, BaseSFVertex firstVertex) {
        super(g, inheritedScope);
        this.firstVertex = firstVertex;
        this.newObjectExpressions = new HashMap<>();
    }

    private InnerScope(InnerScope other) {
        super(other);
        this.firstVertex = other.firstVertex;
        this.returnStatement = other.returnStatement;
        this.newObjectExpressions = CloneUtil.cloneHashMap(other.newObjectExpressions);
    }

    @Override
    public InnerScope deepClone() {
        return DeepCloneContextProvider.cloneIfAbsent(this, () -> new InnerScope(this));
    }

    /**
     * Handle situations such as public static SomeClass getInstance() { return new SomeClass(); }
     */
    @Override
    public void afterVisit(NewObjectExpressionVertex vertex) {
        if (returnStatement != null) {
            this.newObjectExpressions.put(vertex, getChainedApexValue(vertex).orElse(null));
        }
        super.afterVisit(vertex);
    }

    /** Fully resolve any returned values and return set them as the return value. */
    @Override
    public boolean visit(ReturnStatementVertex vertex) {
        trackVisited(vertex);
        if (returnStatement != null) {
            throw new UnexpectedException(this);
        }
        returnStatement = vertex;
        return true;
    }

    @Override
    public void afterVisit(ReturnStatementVertex vertex) {
        // This scope can be many layers deep. Traverse the stack to find the closest method
        // invocation. This is the
        // scope where the return value is set
        MethodInvocationScope methodInvocationScope =
                getClosestMethodInvocationScope().orElseThrow(() -> new UnexpectedException(this));

        ApexValueBuilder builder = ApexValueBuilder.get(this);

        // Provide a hint to the builder in case the returned value is not specific enough to
        // determine the type. This
        // can happen when returning an explicit null
        String returnType = vertex.getParentMethod().get().getReturnType();
        if (!returnType.equalsIgnoreCase(ASTConstants.TYPE_VOID)) {
            builder.methodReturnType(returnType);
        }

        ApexValue<?> returnValue = null;
        ChainedVertex returnVertex = vertex.getReturnValue().orElse(null);
        if (returnVertex != null) {
            if (returnVertex instanceof NewObjectExpressionVertex) {
                returnValue = newObjectExpressions.get(returnVertex);
            } else if (returnVertex instanceof ThisVariableExpressionVertex) {
                returnValue =
                        builder.buildApexClassInstanceValue(
                                getClosestClassInstanceScope()
                                        .orElseThrow(() -> new UnexpectedException(vertex)));
            } else {
                returnValue = ScopeUtil.resolveToApexValue(builder, returnVertex).orElse(null);
            }

            if (returnValue == null) {
                // We were unable to find a specific value. Create a generic one
                returnValue = builder.valueVertex(returnVertex).build();
            }
        }

        if (returnValue != null) {
            // Only set the return value if there is something to set. This can be null in the case
            // of returning early
            // from a method that returns void
            methodInvocationScope.setReturnValue(returnValue);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "Setting return value. vertex="
                            + vertex
                            + ", returnedApexValue="
                            + returnValue);
        }
    }

    @Override
    public String toString() {
        return "InnerScope{"
                + "firstVertex="
                + firstVertex
                + ", returnStatement="
                + returnStatement
                + ", newObjectExpressions="
                + newObjectExpressions
                + '}';
    }
}
