package com.salesforce.rules.unusedmethod;

import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.visitor.TypedVertexVisitor;
import java.util.List;

/**
 * Abstract base class for validating that a given method is actually executed. Used as a helper
 * class by {@link com.salesforce.rules.UnusedMethodRule}.
 */
public abstract class AbstractCallValidator extends TypedVertexVisitor.DefaultNoOp<Boolean> {
    /** The method that we want to verify is actually invoked. */
    protected final MethodVertex targetMethod;

    /** A helper object used to track state and caching as the rule executes. */
    protected final RuleStateTracker ruleStateTracker;

    /**
     * @param targetMethod - The method for which we're trying to find usages
     * @param ruleStateTracker - Helper object provided by the rule
     */
    protected AbstractCallValidator(MethodVertex targetMethod, RuleStateTracker ruleStateTracker) {
        this.targetMethod = targetMethod;
        this.ruleStateTracker = ruleStateTracker;
    }

    /**
     * Override the default visit method to return false, since vertex types we're not prepared to
     * deal with should not be interpreted as usage of the target method.
     */
    @Override
    public Boolean defaultVisit(BaseSFVertex vertex) {
        return false;
    }

    /**
     * @return True if the provided method call could plausibly be a call of our target method.
     */
    private boolean isValidCall(InvocableWithParametersVertex vertex) {
        // Submitting a CallValidator to a vertex will cause the vertex to be visited using the
        // methods implemented in the relevant CallValidator subclass.
        return vertex.accept(this);
    }

    /**
     * Indicates whether the parameters provided to the given method call approximately match those
     * expects by our target method.
     */
    protected boolean parametersAreValid(InvocableWithParametersVertex vertex) {
        // If the arity is wrong, then it's not a match, but rather a call to another overload of
        // the same method.
        // TODO: Long-term, we'll want to validate the parameters' types in addition to their count.
        List<ChainedVertex> parameters = vertex.getParameters();
        return parameters.size() == targetMethod.getArity();
    }

    /** Checks all provided method calls to see if any could be a call of the target method. */
    protected boolean validatorDetectsUsage(List<InvocableWithParametersVertex> potentialCallers) {
        // For each call...
        for (InvocableWithParametersVertex potentialCaller : potentialCallers) {
            // Use our visitor to determine whether this invocation is of the target method.
            if (isValidCall(potentialCaller)) {
                // If our checks are satisfied, then this method call appears to be an invocation of
                // the target method.
                return true;
            }
        }
        // If we're here, then we exited the loop without finding a call.
        return false;
    }
}
