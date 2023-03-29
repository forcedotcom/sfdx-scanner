package com.salesforce.rules.unusedmethod;

import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.UserClassVertex;
import java.util.List;
import java.util.Optional;

public class StaticMethodCallValidator extends BaseMethodCallValidator {

    public StaticMethodCallValidator(MethodVertex targetMethod, RuleStateTracker ruleStateTracker) {
        super(targetMethod, ruleStateTracker);
    }

    /**
     * For static methods, an "internal" usage is one that is performed with an implicit reference
     * to the type that holds the methods, i.e. {@code someStaticMethod()}. Such usages can occur
     * in:
     *
     * <ol>
     *   <li>The class where the method is defined.
     *   <li>Any class that extends the class where the method is defined.
     *   <li>Inner classes of any of the above, unless they have another method with the same name
     *       and signature.
     * </ol>
     *
     * @return - True if an internal reference could be found, otherwise false.
     */
    @Override
    protected boolean internalUsageDetected() {
        return usageDetected(new InternalUsageDetector());
    }

    /**
     * For static methods, an "external" usage is one that explicitly references the type that holds
     * the method. E.g., {@code SomeType.someStaticMethod()}. The type in question could be:
     *
     * <ol>
     *   <li>The class that defines the method.
     *   <li>Any class that extends the class that defines the method.
     * </ol>
     *
     * @return True if external usage found, else false
     */
    @Override
    protected boolean externalUsageDetected() {
        return usageDetected(new ExternalUsageDetector());
    }

    private boolean usageDetected(UsageDetector detector) {
        // First, use the detector to check for usage via the host class.
        if (detector.detectsUsage(targetMethod.getDefiningType())) {
            return true;
        }

        // If the method is heritable, we also need to check for usage in subclasses.
        if (methodIsHeritable()) {
            // We'll start with a list of just the immediate subclasses of the host class.
            List<String> subclassNames =
                    ruleStateTracker.getSubclasses(targetMethod.getDefiningType());
            // Using a while-loop instead of a for-each loop lets us grow the list as we iterate
            // through it.
            int i = 0;
            while (i < subclassNames.size()) {
                String subclassName = subclassNames.get(i);
                // If we find a usage in that subclass, we're done.
                if (detector.detectsUsage(subclassName)) {
                    return true;
                }
                // Otherwise, if the subclass is itself extensible, then its own subclasses must be
                // added for consideration.
                subclassNames.addAll(ruleStateTracker.getSubclasses(subclassName));
                // Finally, increment the index to the next subclass.
                i += 1;
            }
        }
        // If we're here, then we've exhausted our options for this reference type, and can return
        // false.
        return false;
    }

    private abstract static class UsageDetector {
        abstract boolean detectsUsage(String definingType);
    }

    private class InternalUsageDetector extends UsageDetector {
        /**
         * Checks for internal usage of {@link #targetMethod} occurring in {@code definingType} or
         * any of its inner classes.
         *
         * @param definingType - A class name
         * @return True if found, else false
         */
        @Override
        protected boolean detectsUsage(String definingType) {
            // If the class itself has an internal usage, we're done.
            if (usageInClass(definingType)) {
                return true;
            }
            // Inner classes can make internal references to outer static methods, so we should
            // check those too.
            // Start by getting all inner classes.
            List<UserClassVertex> innerClasses = ruleStateTracker.getInnerClasses(definingType);
            for (UserClassVertex innerClass : innerClasses) {
                // If this inner class has/inherits a method with the same signature as the target
                // method,
                // then internal-style calls will be to that method instead of the target method.
                if (ruleStateTracker.classInheritsMatchingMethod(
                        innerClass.getDefiningType(), targetMethod.getSignature())) {
                    continue;
                }
                // Otherwise, check the inner class for calls.
                if (usageInClass(innerClass.getDefiningType())) {
                    return true;
                }
            }
            // If we're here, we couldn't find anything, so we're done.
            return false;
        }

        /**
         * Checks for internal usage of {@link #targetMethod} within {@code definingType}
         * specifically.
         *
         * @param definingType - A class name
         * @return True if found, else false
         */
        private boolean usageInClass(String definingType) {
            // Get all method call expressions in the target class.
            List<MethodCallExpressionVertex> potentialCalls =
                    ruleStateTracker.getMethodCallExpressionsByDefiningType(definingType);
            for (MethodCallExpressionVertex potentialCall : potentialCalls) {
                // If the call is anything other than an empty reference, it's not an internal call.
                if (!potentialCall.isEmptyReference()) {
                    continue;
                }
                // If the name is wrong, it's not a match.
                if (!potentialCall.getMethodName().equalsIgnoreCase(targetMethod.getName())) {
                    continue;
                }
                // If the parameters are wrong, it's not a match.
                if (!parametersAreValid(potentialCall)) {
                    continue;
                }
                // If all our checks passed, it's a valid call.
                return true;
            }
            // If we're here, we've exhausted all options.
            return false;
        }
    }

    private class ExternalUsageDetector extends UsageDetector {
        @Override
        protected boolean detectsUsage(String definingType) {
            // First, get method call that matches `definingType.methodName()`
            List<MethodCallExpressionVertex> potentialCalls =
                    ruleStateTracker.getInvocationsOnType(definingType, targetMethod.getName());
            for (MethodCallExpressionVertex potentialCall : potentialCalls) {
                // If the method's parameters are invalid, then it's not a match.
                // NOTE: The choice to check parameters first is because the sooner we can
                // disqualify a match, the better. Parameter validation is (at time of writing)
                // pretty unsophisticated, so it's cheaper to check that first. If parameter
                // validation gets more intelligent, it may be beneficial to reorder these checks.
                if (!parametersAreValid(potentialCall)) {
                    continue;
                }

                // If the reference appears to be for something other than the host class,
                // then it's not a match.
                Optional<BaseSFVertex> declaration =
                        ruleStateTracker.getDeclarationOfReferencedValue(potentialCall);
                if (!declaration.isPresent()) {
                    return true;
                }
            }
            // If we're here, none of the calls matched, i.e. we detected no usage.
            return false;
        }
    }
}
