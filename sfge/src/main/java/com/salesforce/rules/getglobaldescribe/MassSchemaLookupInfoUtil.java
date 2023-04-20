package com.salesforce.rules.getglobaldescribe;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SFVertex;

/** Utility to help with violation message creation on AvoidMassSchemaLookupRule */
public final class MassSchemaLookupInfoUtil {
    private MassSchemaLookupInfoUtil() {}

    public static String getMessage(MassSchemaLookupInfo info) {
        return getMessage(
                info.getSinkVertex().getFullMethodName(),
                info.getRepetitionType(),
                getOccurrenceInfoValue(info.getRepetitionType(), info.getRepetitionVertex()),
                info.getRepetitionVertex().getDefiningType(),
                info.getRepetitionVertex().getBeginLine());
    }

    public static String getMessage(
            String sinkMethodName,
            RuleConstants.RepetitionType repetitionType,
            String occurrenceInfoValue,
            String occurrenceClassName,
            int occurrenceLine) {
        final String occurrenceMessage = getOccurrenceMessage(repetitionType, occurrenceInfoValue);

        return String.format(
                UserFacingMessages.AvoidExcessiveSchemaLookupsTemplates.MESSAGE_TEMPLATE,
                sinkMethodName,
                occurrenceMessage,
                occurrenceClassName,
                occurrenceLine);
    }

    private static String getOccurrenceInfoValue(
            RuleConstants.RepetitionType repetitionType, SFVertex repetitionVertex) {
        if (RuleConstants.RepetitionType.MULTIPLE.equals(repetitionType)) {
            // Use method name on template message
            return ((MethodCallExpressionVertex) repetitionVertex).getFullMethodName();
        } else {
            // Use Loop type on template message
            return repetitionVertex.getLabel();
        }
    }

    private static String getOccurrenceMessage(
            RuleConstants.RepetitionType repetitionType, String value) {
        return repetitionType.getMessage(value);
    }
}
