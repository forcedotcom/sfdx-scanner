package com.salesforce.graph.ops;

import com.salesforce.graph.ops.directive.EngineDirective;
import com.salesforce.graph.symbols.ContextProviders;
import com.salesforce.graph.symbols.EngineDirectiveContext;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.rules.AbstractRule;

/** Determine the state of EngineDirectives based on static and dynamic state */
public final class EngineDirectiveUtil {
    public static boolean isRuleEnabled(BaseSFVertex vertex, AbstractRule rule) {
        return !isRuleDisabled(vertex, rule);
    }

    /**
     * @return return true if {@code vertex} has been disabled by a directive. The directive could
     *     be a static one such as "sfge-disable-next-line" or one that is contextual based on the
     *     path that is being traversed
     */
    public static boolean isRuleDisabled(BaseSFVertex vertex, AbstractRule rule) {
        return matchRuleDirective(vertex, rule);
    }

    private static boolean matchRuleDirective(BaseSFVertex vertex, AbstractRule rule) {
        AbstractRule.Descriptor descriptor = rule.getDescriptor();

        // Find any directives that have been pushed onto the stack as methods are invoked
        EngineDirectiveContext engineDirectiveContextProvider =
                ContextProviders.ENGINE_DIRECTIVE_CONTEXT.get().getEngineDirectiveContext();
        for (EngineDirective engineDirective :
                engineDirectiveContextProvider.getEngineDirectives()) {
            if (engineDirective.isDisableStack()) {
                if (engineDirective.matchesRule(descriptor.getName())) {
                    return true;
                }
            }
        }

        /*
         Find any directives that are statically defined in the AST hierarchy. Any disable command that is in the
         hierarchy will statically disable the vertex.
         The reasons for this are the following:
         engine-disable on UserClassVertex - The whole class is disabled
         engine-disable-stack on MethodVertex - All vertices in the method will be disabled when the method is entered
         engine-disable-next-line on Any vertex - This disables the vertex passed into this method
        */
        for (EngineDirective engineDirective : vertex.getAllEngineDirectives()) {
            if (engineDirective.isAnyDisable()) {
                if (engineDirective.matchesRule(descriptor.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private EngineDirectiveUtil() {}
}
