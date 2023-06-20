package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
import com.salesforce.graph.vertex.SFVertex;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Utility to help with violation message creation on MultipleMassSchemaLookupRule */
public final class MassSchemaLookupInfoUtil {
    private MassSchemaLookupInfoUtil() {}

    public static String getMessage(MultipleMassSchemaLookupInfo info) {
        return getMessage(
                info.getSinkVertex().getFullMethodName(),
                info.getRepetitionType(),
                getOccurrenceInfo(info.getRepetitionType(), info.getRepetitionVertices()));
    }

    public static String getMessage(
            String sinkMethodName,
            MmslrUtil.RepetitionType repetitionType,
            List<OccurrenceInfo> occurrenceInfos) {

        return String.format(
                UserFacingMessages.MultipleMassSchemaLookupRuleTemplates.MESSAGE_TEMPLATE,
                sinkMethodName,
                getOccurrenceMessage(repetitionType, ""),
                getConsolidatedOccurrenceInfo(occurrenceInfos));
    }

    private static String getOccurrenceVertexLabel(
            MmslrUtil.RepetitionType repetitionType, SFVertex repetitionVertex) {
        if (MmslrUtil.RepetitionType.PRECEDED_BY.equals(repetitionType)) {
            // Use method name on template message
            return ((MethodCallExpressionVertex) repetitionVertex).getFullMethodName();
        } else if (MmslrUtil.RepetitionType.CALL_STACK.equals(repetitionType)) {
            if (repetitionVertex instanceof MethodCallExpressionVertex) {
                return ((MethodCallExpressionVertex) repetitionVertex).getFullMethodName();
            } else if (repetitionVertex instanceof NewObjectExpressionVertex) {
                return ((NewObjectExpressionVertex) repetitionVertex).getCanonicalType();
            } else {
                return repetitionVertex.getLabel();
            }
        } else {
            // Use Loop type on template message
            return repetitionVertex.getLabel();
        }
    }

    private static String getOccurrenceMessage(
            MmslrUtil.RepetitionType repetitionType, String value) {
        return repetitionType.getMessage(value);
    }

    private static List<OccurrenceInfo> getOccurrenceInfo(
            MmslrUtil.RepetitionType repetitionType, SFVertex[] repetitionVertices) {
        return Arrays.stream(repetitionVertices)
                .map(repVertex -> getOccurrenceInfo(repetitionType, repVertex))
                .collect(Collectors.toList());
    }

    private static String getConsolidatedOccurrenceInfo(List<OccurrenceInfo> occurrenceInfos) {
        final List<String> occurrenceStringList =
                Stream.of(occurrenceInfos)
                        .map(occurrenceInfo -> occurrenceInfo.toString())
                        .collect(Collectors.toList());
        // Sorting list to ensure order of results to help with testing.
        occurrenceStringList.sort(String.CASE_INSENSITIVE_ORDER);
        return occurrenceStringList.stream().collect(Collectors.joining(","));
    }

    private static OccurrenceInfo getOccurrenceInfo(
            MmslrUtil.RepetitionType repetitionType, SFVertex repetitionVertex) {
        final String occurrenceVertexValue =
                getOccurrenceVertexLabel(repetitionType, repetitionVertex);

        return new OccurrenceInfo(
                occurrenceVertexValue,
                repetitionVertex.getDefiningType(),
                repetitionVertex.getBeginLine());
    }

    /**
     * Internal representation of an occurrence info. TODO: Consider moving this and its related
     * methods to their own class if it's used outside MMSLR.
     */
    public static class OccurrenceInfo {
        final String label;
        final String definingType;
        final int lineNum;

        public OccurrenceInfo(String label, String definingType, int lineNum) {
            this.label = label;
            this.definingType = definingType;
            this.lineNum = lineNum;
        }

        @Override
        public String toString() {
            return String.format(
                    UserFacingMessages.MultipleMassSchemaLookupRuleTemplates.OCCURRENCE_TEMPLATE,
                    label,
                    definingType,
                    lineNum);
        }
    }
}
