package com.salesforce.rules.unusedmethod;

import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.ThisMethodCallExpressionVertex;
import java.util.List;

public final class InternalCallValidator extends CallValidator {
    /**
     *
     * @param targetMethod - The method for which we're trying to find usages
     * @param ruleStateTracker - Helper object provided by the rule
     */
    public InternalCallValidator(MethodVertex targetMethod, RuleStateTracker ruleStateTracker) {
        super(targetMethod, ruleStateTracker);
    }

    /**
     * Returns true if the validator's target method could plausibly called within the class where
     * it's defined. E.g., as `this.method()` or simply `method()`.
     */
    public boolean methodUsedInternally() {
        // Get every method call in the class where the target method is defined.
        List<InvocableWithParametersVertex> potentialInternalCallers =
                ruleStateTracker.getMethodCallsByDefiningType(targetMethod.getDefiningType());
        // Check for usage amongst those calls.
        return validatorDetectsUsage(potentialInternalCallers);
    }

    /**
     * Handler for method call expressions (e.g., `this.someMethod()`).
     *
     * @return True if method call could plausibly be of the target method.
     */
    @Override
    public Boolean visit(MethodCallExpressionVertex vertex) {
        // If the target method is a constructor, then this can't be an invocation.
        if (targetMethod.isConstructor()) {
            return false;
        }
        // If the contained reference expression isn't a `this` expression or empty,
        // then this isn't an internal call, and therefore can't be an internal invocation
        // of the method.
        if (!vertex.isThisReference() && !vertex.isEmptyReference()) {
            return false;
        }

        // If the method's name is wrong, it's not a match.
        if (!vertex.getMethodName().equalsIgnoreCase(targetMethod.getName())) {
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
        if (!targetMethod.isConstructor()) {
            return false;
        }
        // The last check we do is whether the parameters are valid.
        return parametersAreValid(vertex);
    }
}
