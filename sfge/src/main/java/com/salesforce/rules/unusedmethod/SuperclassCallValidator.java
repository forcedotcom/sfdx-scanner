package com.salesforce.rules.unusedmethod;

import com.salesforce.graph.vertex.MethodVertex;

public final class SuperclassCallValidator extends CallValidator {
    /**
     *
     * @param targetMethod - The method for which we're trying to find usages
     * @param ruleStateTracker - Helper object provided by the rule
     */
    public SuperclassCallValidator(MethodVertex targetMethod, RuleStateTracker ruleStateTracker) {
        super(targetMethod, ruleStateTracker);
    }

    /**
     * Seeks invocations of the provided method that occur within superclasses of the class where
     * it's defined. E.g., the parent class declares the method as abstract and then another method
     * invokes it.
     *
     * @return true if such an invocation could be found, else false.
     */
    public boolean methodUsedBySuperclass() {
        // TODO: IMPLEMENT THIS METHOD.
        return false;
    }

    /**
     * @return - True if the provided method is visible to superclasses of the class where it's
     *     defined. E.g., as an implementation of an inherited abstract method.
     */
    private boolean methodVisibleToSuperClasses(MethodVertex methodVertex) {
        // TODO: IMPLEMENT THIS METHOD.
        return false;
    }
}
