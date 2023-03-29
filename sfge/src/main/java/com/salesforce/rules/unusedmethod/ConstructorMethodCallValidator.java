package com.salesforce.rules.unusedmethod;

import com.salesforce.graph.vertex.*;
import java.util.List;

/**
 * Helper class for {@link com.salesforce.rules.UnusedMethodRule}. Used for determining whether a
 * constructor is ever invoked, e.g., {@code this()}, {@code super()}, or {@code new Whatever()}.
 */
public class ConstructorMethodCallValidator extends BaseMethodCallValidator {

    public ConstructorMethodCallValidator(
            MethodVertex targetMethod, RuleStateTracker ruleStateTracker) {
        super(targetMethod, ruleStateTracker);
    }

    /**
     * For constructors, an "internal" invocation occurs when another constructor delegates to this
     * one.
     *
     * <ol>
     *   <li>Another constructor in the same class calling {@code this()}.
     *   <li>A constructor in a DIRECT subclass calling {@code super()}. Constructors only inherit
     *       one level.
     * </ol>
     *
     * @return - True if constructor is invoked internally.
     */
    @Override
    protected boolean internalUsageDetected() {
        // First, check for usage in the class where the target method is defined.
        List<ThisMethodCallExpressionVertex> ownClassPotentialCalls =
                ruleStateTracker.getThisMethodCallExpressionsByDefiningType(
                        targetMethod.getDefiningType());

        // For constructors on the same class, we're just looking for whether the parameters are
        // valid.
        for (ThisMethodCallExpressionVertex potentialCall : ownClassPotentialCalls) {
            if (parametersAreValid(potentialCall)) {
                return true;
            }
        }

        // If the constructor isn't heritable, we're done.
        if (!methodIsHeritable()) {
            return false;
        }
        // If the constructor is heritable, then we have to check for usage by subclasses via
        // `super()`.
        List<String> subclassNames = ruleStateTracker.getSubclasses(targetMethod.getDefiningType());
        for (String subclassName : subclassNames) {
            List<SuperMethodCallExpressionVertex> subclassPotentialCalls =
                    ruleStateTracker.getSuperMethodCallExpressionsByDefiningType(subclassName);
            for (SuperMethodCallExpressionVertex potentialCall : subclassPotentialCalls) {
                if (parametersAreValid(potentialCall)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * For constructors, an "external" invocation is any time the constructor is called via the
     * {@code new} keyword.
     *
     * @return - True if the constructor is invoked externally.
     */
    @Override
    protected boolean externalUsageDetected() {
        // Get all `new Whatever()` calls for the defining type.
        List<NewObjectExpressionVertex> potentialExternalCalls =
                ruleStateTracker.getConstructionsOfType(targetMethod.getDefiningType());
        // Test each one to see whether its parameters match.
        for (NewObjectExpressionVertex potentialExternalCall : potentialExternalCalls) {
            if (parametersAreValid(potentialExternalCall)) {
                return true;
            }
        }
        // If we're here, it's because we couldn't find any matches.
        return false;
    }
}
