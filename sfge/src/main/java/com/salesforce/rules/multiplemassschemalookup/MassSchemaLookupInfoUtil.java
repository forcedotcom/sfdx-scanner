package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.rules.AvoidMultipleMassSchemaLookups;
import com.salesforce.rules.ops.OccurrenceInfo;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Utility to help with violation message creation on {@link AvoidMultipleMassSchemaLookups} */
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
        return repetitionType.getViolationMessage(
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
        return repetitionType.getOccurrenceMessage(value);
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
}
