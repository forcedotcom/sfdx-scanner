package com.salesforce.graph.ops.expander;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;

public final class RecursionDetectedException extends ApexPathExpanderException {
    /**
     * The path that contains the fork. This can be different than the topLevelPath of the
     * ApexPathExpander.
     */
    private final ApexPath pathWithRecursion;

    /** The vertex that invokes the method with multiple paths */
    private final MethodCallExpressionVertex methodCallExpression;

    public RecursionDetectedException(
            ApexPath pathWithRecursion, MethodCallExpressionVertex methodCallExpression) {
        this.pathWithRecursion = pathWithRecursion;
        this.methodCallExpression = methodCallExpression;
    }

    public ApexPath getPathWithRecursion() {
        return pathWithRecursion;
    }

    public MethodCallExpressionVertex getMethodCallExpression() {
        return methodCallExpression;
    }
}
