package com.salesforce.rules.unusedmethod;

import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SuperMethodCallExpressionVertex;

public final class SubclassCallValidator extends CallValidator {
    public SubclassCallValidator(MethodVertex invokedMethod) {
        super(invokedMethod);
    }

    /**
     * Handler for method call expressions (e.g., `x.someMethod()`).
     *
     * @return - True if this could plausibly be an invocation of the target method.
     */
    @Override
    public Boolean visit(MethodCallExpressionVertex vertex) {
        // TODO: IMPLEMENT THIS METHOD
        return false;
    }

    /**
     * Handler for super constructor invocation (e.g., `super()).
     *
     * @return - True if this could plausibly be an invocation of the target method.
     */
    @Override
    public Boolean visit(SuperMethodCallExpressionVertex vertex) {
        // If the target method isn't a constructor, then this can't be an invocation of it.
        if (!invokedMethod.isConstructor()) {
            return false;
        }
        return parametersAreValid(vertex);
    }
}
