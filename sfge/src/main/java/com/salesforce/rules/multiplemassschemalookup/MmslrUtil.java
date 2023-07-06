package com.salesforce.rules.multiplemassschemalookup;

import com.google.common.collect.ImmutableSet;
import com.salesforce.config.UserFacingMessages.MultipleMassSchemaLookupRuleTemplates;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import java.util.Locale;

public final class MmslrUtil {

    private MmslrUtil() {}

    static final String METHOD_SCHEMA_GET_GLOBAL_DESCRIBE = "Schema.getGlobalDescribe";
    static final String METHOD_SCHEMA_DESCRIBE_SOBJECTS = "Schema.describeSObjects";

    private static final ImmutableSet<String> EXPENSIVE_METHODS =
            ImmutableSet.of(
                    METHOD_SCHEMA_GET_GLOBAL_DESCRIBE.toLowerCase(Locale.ROOT),
                    METHOD_SCHEMA_DESCRIBE_SOBJECTS.toLowerCase(Locale.ROOT));

    /**
     * @param vertex to check
     * @return true if the method call is a known expensive schema call.
     */
    static boolean isSchemaExpensiveMethod(MethodCallExpressionVertex vertex) {
        final String fullMethodName = vertex.getFullMethodName();
        return EXPENSIVE_METHODS.contains(fullMethodName.toLowerCase(Locale.ROOT));
    }

    /** Enum to indicate the type of repetition the method call was subjected. */
    public enum RepetitionType {
        LOOP(
                MultipleMassSchemaLookupRuleTemplates.LOOP_MESSAGE_TEMPLATE,
                MultipleMassSchemaLookupRuleTemplates.MESSAGE_TEMPLATE),
        PRECEDED_BY(
                MultipleMassSchemaLookupRuleTemplates.PRECEDED_BY_MESSAGE_TEMPLATE,
                MultipleMassSchemaLookupRuleTemplates.METHODLESS_MESSAGE_TEMPLATE),
        CALL_STACK(
                MultipleMassSchemaLookupRuleTemplates.CALL_STACK_TEMPLATE,
                MultipleMassSchemaLookupRuleTemplates.MESSAGE_TEMPLATE);

        final String occurrenceTemplate;
        final String messageTemplate;

        RepetitionType(String occurrenceTemplate, String messageTemplate) {
            this.occurrenceTemplate = occurrenceTemplate;
            this.messageTemplate = messageTemplate;
        }

        public String getOccurrenceMessage(String... params) {
            return String.format(occurrenceTemplate, (Object[]) params);
        }

        public String getViolationMessage(String... params) {
            return String.format(messageTemplate, (Object[]) params);
        }
    }
}
