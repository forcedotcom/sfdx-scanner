package com.salesforce.rules.multiplemassschemalookup;

import com.google.common.collect.ImmutableSet;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;

public class RuleConstants {

    static final String METHOD_SCHEMA_GET_GLOBAL_DESCRIBE = "schema.getglobaldescribe";
    static final String METHOD_SCHEMA_DESCRIBE_SOBJECTS = "schema.describesobjects";

    private static final ImmutableSet<String> EXPENSIVE_METHODS =
            ImmutableSet.of(METHOD_SCHEMA_GET_GLOBAL_DESCRIBE, METHOD_SCHEMA_DESCRIBE_SOBJECTS);

    /**
     * @param vertex to check
     * @return true if the method call is a known expensive schema call.
     */
    static boolean isSchemaExpensiveMethod(MethodCallExpressionVertex vertex) {
        final String fullMethodName = vertex.getFullMethodName();
        return EXPENSIVE_METHODS.contains(fullMethodName.toLowerCase());
    }

    /** Enum to indicate the type of repetition the method call was subjected. */
    public enum RepetitionType {
        LOOP(UserFacingMessages.MultipleMassSchemaLookupRuleTemplates.OCCURRENCE_LOOP_TEMPLATE),
        MULTIPLE(
                UserFacingMessages.MultipleMassSchemaLookupRuleTemplates
                        .OCCURRENCE_MULTIPLE_TEMPLATE);

        String messageTemplate;

        RepetitionType(String messageTemplate) {
            this.messageTemplate = messageTemplate;
        }

        public String getMessage(String... params) {
            return String.format(messageTemplate, params);
        }
    }
}
