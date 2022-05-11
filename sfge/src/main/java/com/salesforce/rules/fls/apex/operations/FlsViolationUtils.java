package com.salesforce.rules.fls.apex.operations;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.ops.ObjectFieldUtil;
import com.salesforce.graph.ops.SoqlParserUtil;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertex;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Utils class that deals with FLS-specific violations */
public final class FlsViolationUtils {
    static final String VIOLATION_MESSAGE_TEMPLATE =
            "%1$s validation is missing for [%2$s] operation on [%3$s]";
    private static final String FIELDS_MESSAGE_TEMPLATE = " with field(s) [%s]";
    private static final String FIELD_NAME_SEPARATOR = ",";
    private static final String FIELD_HANDLING_NOTICE =
            " - SFGE may not have parsed some objects/fields correctly. Please confirm if the objects/fields involved in these segments have FLS checks: [%s]";

    private static final String STRIP_INACCESSIBLE_READ_WARNING_TEMPLATE =
            "For stripInaccessible checks on READ operation, "
                    + "SFGE does not have the capability to verify that only sanitized data is used after the check."
                    + "Please ensure that unsanitized data is discarded for [%2$s]";

    static final String ALL_FIELDS = "ALL_FIELDS";

    private enum ViolationType {
        CRUD("CRUD"),
        FLS("FLS");

        String value;

        ViolationType(String value) {
            this.value = value;
        }
    }

    // Pattern to detect parse issues - any non-alphanumeric in a field/object name that isn't a dot
    // or underscore
    // will be considered a parse issue.
    private static final String SPECIAL_CHAR_PATTERN_STR = "[^a-zA-Z0-9\\._]";
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(SPECIAL_CHAR_PATTERN_STR);
    private static final String RELATIONAL_PATTERN_STR = "__r\\b";
    private static final Pattern RELATIONAL_PATTERN =
            Pattern.compile(RELATIONAL_PATTERN_STR, Pattern.CASE_INSENSITIVE);

    private FlsViolationUtils() {}

    /**
     * Consolidates a set of FlsViolationInfo to merge items that have the same source/sink/object
     * checked for the same operation.
     */
    public static HashSet<FlsViolationInfo> consolidateFlsViolations(
            HashSet<FlsViolationInfo> flsViolationInfos) {
        return ObjectFieldUtil.regroupByObject(flsViolationInfos);
    }

    /** @return String representation of the FlsViolationInfo that has information on FLS. */
    public static String constructMessage(FlsViolationInfo flsViolationInfo) {
        final Optional<SFVertex> sourceVertex = flsViolationInfo.getSourceVertex();
        if (sourceVertex.isPresent()) {
            if (sourceVertex.get() instanceof MethodVertex) {
                return constructMessageInternal(flsViolationInfo);
            } else {
                throw new TodoException(
                        "Handle scenarios where source vertex is not a MethodVertex. sourceVertex = "
                                + sourceVertex.get());
            }
        }
        return constructMessageInternal(flsViolationInfo);
    }

    /** @return User readable representation of FLS violations for the given FlsViolationInfo */
    @VisibleForTesting
    static String constructMessageInternal(FlsViolationInfo flsViolationInfo) {
        // ViolationType decides if the message is for CRUD or FLS
        final ViolationType violationType = getViolationType(flsViolationInfo);
        final TreeSet<String> fieldNames =
                getOverridingFieldNames(violationType, flsViolationInfo.getFields());
        final boolean allFields =
                getOverridingAllFields(violationType, flsViolationInfo.isAllFields());

        final String fieldInformation =
                getFieldInformation(fieldNames, allFields, flsViolationInfo.getObjectName());
        final String validationInformation =
                getValidationInformation(flsViolationInfo, violationType);

        // Return the full validation message
        return validationInformation + fieldInformation;
    }

    private static String getValidationInformation(
            FlsViolationInfo flsViolationInfo, ViolationType violationType) {
        // TODO: consider using enums to choose message template when there's more than two options
        final String messageTemplate =
                flsViolationInfo instanceof FlsStripInaccessibleWarningInfo
                        ? STRIP_INACCESSIBLE_READ_WARNING_TEMPLATE
                        : VIOLATION_MESSAGE_TEMPLATE;

        final String validationInformation =
                String.format(
                        messageTemplate,
                        violationType,
                        flsViolationInfo.getValidationType().name(),
                        flsViolationInfo.getObjectName());
        return validationInformation;
    }

    /** Build field information string to include in violation message */
    private static String getFieldInformation(
            TreeSet<String> fieldNames, boolean allFields, String objectName) {
        String fieldInformation = "";
        String fieldString = "";
        final TreeSet<String> complexSegments = CollectionUtil.newTreeSet();

        // Check if object name looks weird
        if (hasUnhandledSegment(objectName)) {
            complexSegments.add(objectName);
        }

        if (allFields) {
            fieldString = ALL_FIELDS;
        }

        if (!fieldNames.isEmpty()) {
            // Identify portions of fields that we may not have parsed correctly
            complexSegments.addAll(detectComplexSegments(fieldNames));
            fieldNames.removeAll(complexSegments);

            // Append to fieldString only if we haven't already populated it with ALL_FIELDS
            // Also, make sure there are fields to actually append
            if (!fieldNames.isEmpty() && !allFields) {
                fieldString = Joiner.on(FIELD_NAME_SEPARATOR).join(fieldNames);
            }
        }

        // Populate field information only if we have anything
        if (!"".equals(fieldString)) {
            fieldInformation = String.format(FIELDS_MESSAGE_TEMPLATE, fieldString);
        }

        // Add field notice if we have segments that may not have been parsed correctly
        if (!complexSegments.isEmpty()) {
            fieldInformation +=
                    String.format(
                            FIELD_HANDLING_NOTICE,
                            Joiner.on(FIELD_NAME_SEPARATOR).join(complexSegments));
        }
        return fieldInformation;
    }

    private static ViolationType getViolationType(FlsViolationInfo flsViolationInfo) {
        // Derive the analysis level of the violation from basics
        final FlsConstants.AnalysisLevel analysisLevel =
                ObjectBasedCheckUtil.getAnalysisLevel(
                        flsViolationInfo.getObjectName(), flsViolationInfo.getValidationType());
        ViolationType violationType;
        if (analysisLevel == FlsConstants.AnalysisLevel.FIELD_LEVEL) {
            violationType = ViolationType.FLS;
        } else if (analysisLevel == FlsConstants.AnalysisLevel.OBJECT_LEVEL) {
            violationType = ViolationType.CRUD;
        } else {
            throw new UnexpectedException(
                    "No violation should've been thrown for analysis level " + analysisLevel);
        }
        return violationType;
    }

    /** Get allFields value based on violation type - for CRUD, always return false */
    private static boolean getOverridingAllFields(
            ViolationType violationType, boolean isAllFields) {
        if (violationType == ViolationType.CRUD) {
            // Always return false
            return false;
        }

        return isAllFields;
    }

    /**
     * Get field names based on violation type - for CRUD, we don't need any field names - for FLS,
     * add an Unknown field even if there are no fields available
     */
    private static TreeSet<String> getOverridingFieldNames(
            ViolationType violationType, TreeSet<String> fields) {
        if (violationType == ViolationType.CRUD) {
            // Always return empty set
            return CollectionUtil.newTreeSet();
        }
        if (fields.isEmpty()) {
            return CollectionUtil.newTreeSetOf(SoqlParserUtil.UNKNOWN);
        }

        return fields;
    }

    /**
     * Detect portions that may not have been parsed fully. Especially when fields/object names are
     * derived from SOQL queries.
     */
    private static TreeSet<String> detectComplexSegments(TreeSet<String> fieldNames) {
        final List<String> complexFieldSegmentsList =
                fieldNames.stream()
                        .filter(fieldName -> hasUnhandledSegment(fieldName))
                        .collect(Collectors.toList());
        final TreeSet<String> complexFieldSegments = CollectionUtil.newTreeSet();
        complexFieldSegments.addAll(complexFieldSegmentsList);
        return complexFieldSegments;
    }

    /**
     * We want to proactively let users know when we see segments of query that we may not have
     * parsed correctly. This includes segments with fishy looking characters/whitespaces as well as
     * fields/objects that refer to relational types.
     */
    private static boolean hasUnhandledSegment(String input) {
        return RELATIONAL_PATTERN.matcher(input).find()
                || SPECIAL_CHAR_PATTERN.matcher(input).find();
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
