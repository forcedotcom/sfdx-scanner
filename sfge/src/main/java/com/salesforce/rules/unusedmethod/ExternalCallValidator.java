package com.salesforce.rules.unusedmethod;

import com.salesforce.graph.vertex.MethodVertex;

public final class ExternalCallValidator extends CallValidator {
    /**
     *
     * @param targetMethod - The method for which we're trying to find usages
     * @param ruleStateTracker - Helper object provided by the rule
     */
    public ExternalCallValidator(MethodVertex targetMethod, RuleStateTracker ruleStateTracker) {
        super(targetMethod, ruleStateTracker);
    }

    /**
     * Seeks invocations of the provided method in a context wholly external to its host class.
     *
     * @return - True if such an invocation could be found. Else false.
     */
    public boolean methodUsedExternally() {
        // TODO: IMPLEMENT THIS METHOD.
        return false;
    }

    /**
     * @return - True if this method is visible externally, e.g., it's a public method.
     */
    private boolean methodVisibleExternally() {
        // TODO: IMPLEMENT THIS METHOD:
        return false;
    }
}
