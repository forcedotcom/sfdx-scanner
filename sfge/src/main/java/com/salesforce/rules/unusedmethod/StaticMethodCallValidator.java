package com.salesforce.rules.unusedmethod;

import com.salesforce.graph.vertex.MethodVertex;

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
     *   <li>Inner classes of any of the above, unless they inherit another method with this name.
     * </ol>
     *
     * @return
     */
    @Override
    protected boolean internalUsageDetected() {
        // TODO: IMPLEMENT THIS METHOD.
        return false;
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
     * @return
     */
    @Override
    protected boolean externalUsageDetected() {
        // TODO: IMPLEMENT THIS METHOD.
        return false;
    }
}
