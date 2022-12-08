package com.salesforce.rules.unusedmethod;

import com.salesforce.graph.vertex.MethodVertex;

public final class InnerClassCallValidator extends CallValidator {
    /**
     *
     * @param targetMethod - The method for which we're trying to find usages
     * @param ruleStateTracker - Helper object provided by the rule
     */
    public InnerClassCallValidator(MethodVertex targetMethod, RuleStateTracker ruleStateTracker) {
        super(targetMethod, ruleStateTracker);
    }

    /**
     * Seeks invocations of the provided method by inner classes of its host class.
     *
     * @return - True if such invocations could be found, else false.
     */
    public boolean methodUsedByInnerClass() {
        // TODO: IMPLEMENT THIS METHOD.
        return false;
    }

    /**
     * @return - true if the provided method is visible to inner classes of the class where it's
     *     defined. E.g., if it's a static method.
     */
    private boolean methodVisibleToInnerClasses() {
        // TODO: IMPLEMENT THIS METHOD:
        return false;
    }
}
