package com.salesforce.rules.unusedmethod;

import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.ThisMethodCallExpressionVertex;

public final class InternalCallValidator extends CallValidator {
    public InternalCallValidator(MethodVertex invokedMethod) {
        super(invokedMethod);
    }

    /**
     * Handler for method call expressions (e.g., `this.someMethod()`).
     *
     * @return True if method call could plausibly be of the target method.
     */
    @Override
    public Boolean visit(MethodCallExpressionVertex vertex) {
        // If the target method is a constructor, then this can't be an invocation.
        if (invokedMethod.isConstructor()) {
            return false;
        }
        // If the contained reference expression isn't a `this` expression or empty,
        // then this isn't an internal call, and therefore can't be an internal invocation
        // of the method.
        if (!vertex.isThisReference() && !vertex.isEmptyReference()) {
            return false;
        }

        // If the method's name is wrong, it's not a match.
        if (!vertex.getMethodName().equalsIgnoreCase(invokedMethod.getName())) {
            return false;
        }

        // The last check we do is whether the parameters are valid.
        return parametersAreValid(vertex);
    }

    /**
     * Handler for invocations of the `this()` constructor.
     *
     * @return - True if method call could plausibly be of the target method.
     */
    @Override
    public Boolean visit(ThisMethodCallExpressionVertex vertex) {
        // If the target method isn't a constructor, then this can't be an invocation.
        if (!invokedMethod.isConstructor()) {
            return false;
        }
        // The last check we do is whether the parameters are valid.
        return parametersAreValid(vertex);
    }
}
