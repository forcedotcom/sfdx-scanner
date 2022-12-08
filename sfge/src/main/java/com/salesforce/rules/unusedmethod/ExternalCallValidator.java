package com.salesforce.rules.unusedmethod;

import com.salesforce.graph.vertex.MethodVertex;

/**
 * Helper class for {@link com.salesforce.rules.UnusedMethodRule}. Used for determining whether a
 * method is called in contexts wholly external to its host class. E.g., `someInstance.method()`.
 */
public final class ExternalCallValidator extends AbstractCallValidator {
    /**
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
