package com.salesforce.rules.fls.apex.operations;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.symbols.apex.ApexValue;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * Helps {@link FlsValidationRepresentation} compare and verify validations. The comparison is
 * typically to tally existing validations and expected validations.
 */
class ValidationVerifier {
    private static final String ID_FIELD = "Id";

    /**
     * Compares expected validation with existing validation and creates violation message when
     * existing validation does not meet expectations. The complication comes from the fact that a
     * single expected validation can be covered by an overlap of more than one existing validation.
     *
     * @return Optional of string that can be used to construct a Violation.
     */
    static Optional<FlsViolationInfo> verifyValidations(
            FlsValidationRepresentation.Info expectedValidation,
            Collection<FlsValidationRepresentation.Info> existingValidations) {

        final Set<String> missingFields = CollectionUtil.newTreeSet();
        final Set<ApexValue<?>> missingFieldVertices = new HashSet<>();

        missingFields.addAll(expectedValidation.getFieldNames());
        missingFieldVertices.addAll(expectedValidation.getFieldValues());

        // Remove Id field from missing fields since we don't expect Id field to be validated
        missingFields.remove(ID_FIELD);

        // TODO: how do we know which vertex is Id field in fieldVertices?
        // Doing a foreach here since expected fields could be spread across more than one existing
        // validation
        boolean isMatch = false;
        for (FlsValidationRepresentation.Info existingValidation : existingValidations) {
            // Applying existing isMatch value as an OR so that if we already have a match, we don't
            // get mislead by
            // remaining existing validations that are not relevant to the current expected
            // validation.
            isMatch =
                    verifyValidations(
                                    expectedValidation,
                                    existingValidation,
                                    missingFields,
                                    missingFieldVertices)
                            || isMatch;

            // We can't break out of the loop even if isMatch is true since we still need to loop
            // through all the existingValidations to filter out as many expected matched fields as
            // possible.
        }

        // For object-level validation, we don't need to compare checks for individual fields
        if (isMatch && isObjectLevelValidation(expectedValidation)) {
            // we have an object level match.
            return Optional.empty();
        }

        // For field-level validation, we need to make sure all fields were addressed.
        if (isMatch
                && isFieldLevelValidation(expectedValidation)
                && missingFields.isEmpty()
                && missingFieldVertices.isEmpty()) {
            // everything is matching! No violation found.
            return Optional.empty();
        }

        return Optional.of(
                FlsViolationUtils.getFlsViolationInfo(
                        expectedValidation, missingFields, missingFieldVertices));
    }

    /**
     * @param expectedValidation
     * @param existingValidation validation to compare this.info with
     * @param missingFields starts with all the fields. This list is modified as and when fields are
     *     matched
     * @param missingFieldValues starts with all the field vertices. This list is modified as and
     *     when fields are matched
     * @return true if we have a match
     */
    private static boolean verifyValidations(
            FlsValidationRepresentation.Info expectedValidation,
            FlsValidationRepresentation.Info existingValidation,
            Set<String> missingFields,
            Set<ApexValue<?>> missingFieldValues) {
        // We need to compare expectedValidation with existingValidation to see if it is already
        // handled.

        if (!Objects.equals(
                expectedValidation.getValidationType(), existingValidation.getValidationType())) {
            // If validationType doesn't match, we already know this is not a match
            return false;
        }

        if (StringUtils.isEmpty(expectedValidation.getObjectName())
                && expectedValidation.getObjectValue() == null) {
            // Object information is missing and we can't proceed further
            return false;
        }

        // If we have an object value, compare it with existingValidation.objectValue
        if (expectedValidation.getObjectValue() != null
                && !expectedValidation
                        .getObjectValue()
                        .equals(existingValidation.getObjectValue())) {

            // Give another try to compare object value with object name
            if (!compare(expectedValidation.getObjectValue(), existingValidation.getObjectName())) {
                // object values don't match. This is not a match.
                return false;
            }
        }

        // If we have an objectName value, compare it with existingValidation.objectName
        if (StringUtils.isNotEmpty(expectedValidation.getObjectName())
                && !StringUtils.equalsIgnoreCase(
                        expectedValidation.getObjectName(), existingValidation.getObjectName())) {

            // Give another try to compare object name with object value
            if (!compare(existingValidation.getObjectValue(), expectedValidation.getObjectName())) {
                // objectNames don't match. This is not a match.
                return false;
            }
        }

        // If expected validation requires validation for all fields and
        // existing validation handles an individual field, we don't have a way to guess if all the
        // fields
        // in existing validation ever make the full list. Unless existing validation is for all
        // fields as well,
        // we'll call this a no-match.
        if (expectedValidation.getValidationType().isFieldCheckNeeded()
                && expectedValidation.isAllFields()
                && !existingValidation.isAllFields()) {
            return false;
        }

        // If we are here, validationTypes and either objectNames or objectVertices should've
        // matched.

        // If existing validation covers all fields, remove all missing fields
        if (existingValidation.isAllFields()) {
            missingFields.clear();
            missingFieldValues.clear();
        }

        removeMatchedFields(
                existingValidation, expectedValidation, missingFields, missingFieldValues);

        return true;
    }

    /** Modifies missingFields and missingFieldValues based on fields that are matched */
    private static void removeMatchedFields(
            FlsValidationRepresentation.Info existingValidation,
            FlsValidationRepresentation.Info expectedValidation,
            Set<String> missingFields,
            Set<ApexValue<?>> missingFieldValues) {
        // Compare with fieldnames in expectedValidation
        for (String expectedFieldName : expectedValidation.getFieldNames()) {
            // Match fields in String type
            if (existingValidation.getFieldNames().contains(expectedFieldName)) {
                missingFields.remove(expectedFieldName);
            }

            // Match fields in ApexValue type
            for (ApexValue<?> existingFieldValue : existingValidation.getFieldValues()) {
                if (compare(existingFieldValue, expectedFieldName)) {
                    missingFields.remove(expectedFieldName);
                }
            }
        }

        // Compare with fieldValues in expectedValidation
        for (ApexValue<?> expectedFieldValue : expectedValidation.getFieldValues()) {
            // Match fields in ApexValue type
            if (existingValidation.getFieldValues().contains(expectedFieldValue)) {
                missingFieldValues.remove(expectedFieldValue);
            }

            // Match fields in String type
            for (String existingFieldName : existingValidation.getFieldNames()) {
                if (compare(expectedFieldValue, existingFieldName)) {
                    missingFieldValues.remove(expectedFieldValue);
                }
            }
        }
    }

    private static boolean compare(ApexValue<?> apexValue, String stringValue) {
        if (apexValue != null && StringUtils.isNotEmpty(stringValue)) {
            final String stringEquivalent =
                    ApexValueUtil.deriveUserFriendlyDisplayName(apexValue).orElse("UNKNOWN");
            return stringEquivalent.equalsIgnoreCase(stringValue);
        }
        return false;
    }

    private static boolean isFieldLevelValidation(FlsValidationRepresentation.Info myInfo) {
        return FlsConstants.AnalysisLevel.FIELD_LEVEL.equals(
                myInfo.getValidationType().analysisLevel);
    }

    private static boolean isObjectLevelValidation(FlsValidationRepresentation.Info myInfo) {
        return FlsConstants.AnalysisLevel.OBJECT_LEVEL.equals(
                myInfo.getValidationType().analysisLevel);
    }
}
