package com.salesforce.rules.fls.apex.operations;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.TodoException;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/** Handles creation of various types of {@link FlsViolationInfo} objects */
public final class FlsViolationCreatorUtil {

    private FlsViolationCreatorUtil() {}

    static Set<FlsViolationInfo> createStripInaccessibleWarningViolations(
            ApexValue<?> apexValue, Collection<FlsValidationRepresentation> validationReps) {
        final Set<FlsViolationInfo> warningViolations = new HashSet<>();
        final ChainedVertex vertex =
                apexValue
                        .getValueVertex()
                        .orElse((ChainedVertex) apexValue.getInvocable().orElse(null));

        if (vertex == null) {
            throw new TodoException("No related vertex found for apex value: " + apexValue);
        }
        for (FlsValidationRepresentation validationRep : validationReps) {
            for (FlsValidationRepresentation.Info validationInfo :
                    validationRep.getValidationInfo()) {
                final FlsStripInaccessibleWarningInfo warningInfo =
                        getFlsStripInaccessibleWarningInfo(validationInfo);

                warningInfo.setSinkVertex(vertex);
                warningViolations.add(warningInfo);
            }
        }
        return warningViolations;
    }

    static FlsStripInaccessibleWarningInfo getFlsStripInaccessibleWarningInfo(
            FlsValidationRepresentation.Info validationRepInfo) {
        final String userFriendlyObjectName =
                ApexValueUtil.deriveUserFriendlyDisplayName(validationRepInfo.getObjectValue())
                        .orElse(validationRepInfo.getObjectName());
        final TreeSet<String> combinedFieldNames =
                combineFieldNamesAndValues(
                        validationRepInfo.getFieldNames(),
                        validationRepInfo.getFieldValues(),
                        validationRepInfo.getValidationType().analysisLevel);

        return new FlsStripInaccessibleWarningInfo(
                validationRepInfo.getValidationType(),
                userFriendlyObjectName,
                combinedFieldNames,
                validationRepInfo.isAllFields());
    }

    static FlsViolationInfo getFlsViolationInfo(
            FlsValidationRepresentation.Info validationRepInfo,
            Set<String> missingFields,
            Set<ApexValue<?>> missingFieldValues) {
        // Create a single set with both field names and field vertices. For field vertices, we use
        // the name on the image.
        final TreeSet<String> combinedMissingFields =
                combineFieldNamesAndValues(
                        missingFields,
                        missingFieldValues,
                        validationRepInfo.getValidationType().analysisLevel);
        // Use object vertex image name when object vertex is available, else use object name.
        final String userFriendlyObjectName =
                ApexValueUtil.deriveUserFriendlyDisplayName(validationRepInfo.getObjectValue())
                        .orElse(validationRepInfo.getObjectName());

        final FlsViolationInfo flsViolationInfo =
                new FlsViolationInfo(
                        validationRepInfo.getValidationType(),
                        userFriendlyObjectName,
                        combinedMissingFields,
                        validationRepInfo.isAllFields());

        return flsViolationInfo;
    }

    static FlsViolationInfo createUnresolvedCrudFlsViolation(
        FlsConstants.FlsValidationType validationType,
        MethodCallExpressionVertex sinkVertex) {
        final FlsViolationInfo violationInfo = new UnresolvedCrudFlsViolationInfo(validationType);
        violationInfo.setSinkVertex(sinkVertex);

        return violationInfo;
    }

    private static TreeSet<String> combineFieldNamesAndValues(
            Set<String> fieldNames,
            Set<ApexValue<?>> fieldValues,
            FlsConstants.AnalysisLevel analysisLevel) {
        final TreeSet<String> combinedFieldNames = CollectionUtil.newTreeSet();

        if (FlsConstants.AnalysisLevel.FIELD_LEVEL.equals(analysisLevel)) {
            combinedFieldNames.addAll(fieldNames);
            fieldValues.forEach(
                    fieldValue -> {
                        combinedFieldNames.add(
                                ApexValueUtil.deriveUserFriendlyDisplayName(fieldValue)
                                        .orElse("UNKNOWN_FIELD"));
                    });
        }
        return combinedFieldNames;
    }
}
