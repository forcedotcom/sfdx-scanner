package com.salesforce.rules.unusedmethod;

import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.MethodVertex;
import java.util.List;

/**
 * Abstract base class for validating that a given method is actually invoked. Used as a helper
 * class by {@link com.salesforce.rules.UnusedMethodRule}.
 */
public abstract class BaseMethodCallValidator {
    /** The method that we want to verify is being invoked. */
    protected final MethodVertex targetMethod;
    /** A helper object used to track state and cache data as the rule executes. */
    protected final RuleStateTracker ruleStateTracker;

    /**
     * @param targetMethod - The method for which we're seeking invocations
     * @param ruleStateTracker - Helper object provided by the rule
     */
    protected BaseMethodCallValidator(
            MethodVertex targetMethod, RuleStateTracker ruleStateTracker) {
        this.targetMethod = targetMethod;
        this.ruleStateTracker = ruleStateTracker;
    }

    /**
     * @return - True if the codebase contains something that could plausibly be an invocation of
     *     {@link #targetMethod}
     */
    public boolean usageDetected() {
        return internalUsageDetected() || externalUsageDetected();
    }

    /**
     * Check whether {@link #targetMethod} is invoked internally. The specifics of what an
     * "internal" usage can look like and where they can happen depend on the method being
     * inspected.
     *
     * @return - True if an internal usage is detected, otherwise false.
     */
    protected abstract boolean internalUsageDetected();

    /**
     * Check whether {@link #targetMethod} is invoked externally. The specifics of what an
     * "external" usage can look like and where they can happen depend on the method being
     * inspected, but generally anything that's not internal will be external.
     *
     * @return - True if an external usage is detected, otherwise false.
     */
    protected abstract boolean externalUsageDetected();

    /**
     * Indicates whether the parameters provided to {@code vertex} approximately match those
     * expected by {@link #targetMethod}.
     */
    protected boolean parametersAreValid(InvocableWithParametersVertex vertex) {
        // If the arity is wrong, then it's not a match, but rather a call to another overload
        // of the same method.
        // TODO: LONG-TERM, WE'LL WANT TO VALIDATE PARAMETER TYPES IN ADDITION TO PARAMETER
        //       COUNT.
        List<ChainedVertex> parameters = vertex.getParameters();
        return parameters.size() == targetMethod.getArity();
    }
}
