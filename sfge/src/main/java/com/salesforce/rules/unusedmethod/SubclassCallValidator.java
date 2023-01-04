package com.salesforce.rules.unusedmethod;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.vertex.*;
import java.util.List;

/**
 * Helper class for {@link com.salesforce.rules.UnusedMethodRule}. Used for determining whether a
 * method is called in within a subclass of its host class. E.g., `this.method()` if not overridden,
 * or `super.method()` if overridden.
 */
public final class SubclassCallValidator extends AbstractCallValidator {
    /**
     * @param targetMethod - The method for which we're trying to find usages
     * @param ruleStateTracker - Helper object provided by the rule
     */
    public SubclassCallValidator(MethodVertex targetMethod, RuleStateTracker ruleStateTracker) {
        super(targetMethod, ruleStateTracker);
    }

    /**
     * Seeks invocations of the provided method that occur within subclasses of the class where it's
     * defined. E.g., `this.method()` if not overridden, or `super.method()` if overridden.
     *
     * @return - True if such an invocation is found, else false.
     */
    // TODO: Consider optimizing this method to handle entire classes instead of individual methods.
    public boolean methodUsedBySubclass() {
        // If the method isn't visible to subclasses, it obviously can't be used by them.
        if (!methodVisibleToSubclasses()) {
            return false;
        }
        // Get a list of each subclass of the method's host class.
        List<String> subclasses = ruleStateTracker.getSubclasses(targetMethod.getDefiningType());
        // Put the check in a loop, so we can recursively process subclasses if needed.
        while (!subclasses.isEmpty()) {
            // For each of the subclasses...
            for (String subclass : subclasses) {
                // Get every method call in that class.
                List<InvocableWithParametersVertex> potentialSubclassCallers =
                        ruleStateTracker.getMethodCallsByDefiningType(subclass);
                // If we can find a usage in those subclasses, we're good.
                if (validatorDetectsUsage(potentialSubclassCallers)) {
                    return true;
                }
            }
            // If we're here, then we've checked all subclasses at this level of inheritance.
            // Whether we continue depends on the type of the target method.
            if (targetMethod.isConstructor()) {
                // If the target method is a constructor, we're done, since constructors are only
                // visible to immediate children.
                break;
            } else {
                // For non-constructor methods, nested calls are possible, so we should get every
                // subclass of the subclasses and keep going.
                subclasses = ruleStateTracker.getSubclasses(subclasses.toArray(new String[] {}));
            }
        }
        // If we're here, then we analyzed the entire subclass inheritance tree and found no
        // potential invocations.
        return false;
    }

    /**
     * @return True if the target method is visible to subclasses of the class where it's defined.
     */
    private boolean methodVisibleToSubclasses() {
        // Private methods are not visible to subclasses.
        if (targetMethod.isPrivate()) {
            return false;
        }
        // Other methods are visible to subclasses, but if the host class is neither abstract nor
        // virtual, then subclasses can't even exist.
        UserClassVertex classVertex =
                targetMethod
                        .getParentClass()
                        .orElseThrow(() -> new UnexpectedException(targetMethod));
        return classVertex.isAbstract() || classVertex.isVirtual();
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
        if (!targetMethod.isConstructor()) {
            return false;
        }
        return parametersAreValid(vertex);
    }
}
