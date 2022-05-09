package com.salesforce.rules.fls.apex.operations;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.ops.SoqlParserUtil;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.schema.DescribeFieldResult;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.rules.fls.apex.operations.FlsConstants.AnalysisLevel;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class FlsValidationRepresentation {
    private final List<Info> infoList;

    public FlsValidationRepresentation() {
        this.infoList = new ArrayList<>();
        this.infoList.add(new Info());
    }

    public List<Info> getValidationInfo() {
        return infoList;
    }

    public void setValidationType(FlsValidationType validationType) {
        if (infoList.size() != 1) {
            throw new UnexpectedException("Unexpected count of info items to handle: " + infoList);
        }
        final Info existingInfo = infoList.get(0);
        if (!existingInfo.fieldNames.isEmpty() || !existingInfo.fieldValues.isEmpty()) {
            if (AnalysisLevel.OBJECT_LEVEL.equals(validationType.analysisLevel)) {
                throw new UnexpectedException(
                        "Cannot set Object-level validation type for a validationInfo with field information: "
                                + existingInfo);
            }
        }

        if (FlsValidationType.UPSERT.equals(validationType)) {
            // Upsert requires an Update and a Insert validation. Accordingly, we'll create two Info
            // objects
            final Info copyInfo = new Info(existingInfo);
            existingInfo.setValidationType(FlsValidationType.INSERT);
            copyInfo.setValidationType(FlsValidationType.UPDATE);
            infoList.add(copyInfo);
        } else {
            // For all other validation types, we can modify the existing info value.
            existingInfo.setValidationType(validationType);
        }
    }

    public void setObject(String objectName) {
        infoList.forEach(info -> info.setObjectName(objectName));
    }

    public void setAllFields() {
        infoList.forEach(info -> info.setAllFields());
    }

    public void addField(String fieldName) {
        infoList.forEach(info -> info.addFieldName(fieldName));
    }

    public void setObject(DescribeSObjectResult sObjectResult) {
        final Optional<SObjectType> sObjectTypeOptional = sObjectResult.getSObjectType();
        if (!sObjectTypeOptional.isPresent()) {
            throw new UnexpectedException(
                    "SObjectType is not available in DescribeSObjectResult: " + sObjectResult);
        }
        final Optional<ApexValue<?>> associatedObjectTypeOptional =
                sObjectTypeOptional.get().getType();
        if (!associatedObjectTypeOptional.isPresent()) {
            throw new UnexpectedException(
                    "SObjectType is not available in DescribeSObjectResult: " + sObjectResult);
        }
        setObject(associatedObjectTypeOptional.get());
    }

    public void setObject(ApexValue<?> objectApexValue) {
        final TreeSet<String> stringValues =
                ApexValueUtil.convertApexValueToString(objectApexValue);
        for (String stringValue : stringValues) {
            infoList.forEach(info -> info.setObjectName(stringValue));
        }
        // If we didn't find any string equivalents, add the apex values directly
        // TODO: do we lose values that weren't converted in for loop?
        if (stringValues.isEmpty()) {
            infoList.forEach(info -> info.setObjectValue(objectApexValue));
        }
    }

    public void addField(DescribeFieldResult describeFieldResult) {
        final Optional<ApexValue<?>> fieldNameValueOptional = describeFieldResult.getFieldName();
        if (!fieldNameValueOptional.isPresent()) {
            throw new TodoException(
                    "DescribeFieldResult does not hold a field name value: " + describeFieldResult);
        }

        final ApexValue<?> fieldNameValue = fieldNameValueOptional.get();

        addField(fieldNameValue);
    }

    public void addFields(Set<ApexValue<?>> fieldValues) {
        fieldValues.forEach(fieldValue -> addField(fieldValue));
    }

    public void addField(ApexValue<?> fieldNameValue) {
        final TreeSet<String> stringValues = ApexValueUtil.convertApexValueToString(fieldNameValue);
        for (String stringValue : stringValues) {
            infoList.forEach(info -> info.addFieldName(stringValue));
        }
        // If we didn't find any string equivalents, add the apex values directly
        // TODO: do we lose values that weren't converted in for loop?
        if (stringValues.isEmpty()) {
            infoList.forEach(info -> info.addFieldValue(fieldNameValue));
        }
    }

    public void addFields(Collection<String> fields) {
        fields.forEach(field -> this.addField(field));
    }

    /**
     * Compares expected validation with existing validation and creates violations when existing
     * validation does not meet expectations.
     *
     * @return Optional of string that can be used to construct a Violation.
     */
    public Set<FlsViolationInfo> compareWithExistingValidation(
            Collection<Info> existingValidations) {
        final Set<FlsViolationInfo> violations = new HashSet<>();
        infoList.forEach(
                info -> {
                    Optional<FlsViolationInfo> violationOptional =
                            ValidationVerifier.verifyValidations(info, existingValidations);
                    if (violationOptional.isPresent()) {
                        violations.add(violationOptional.get());
                    }
                });
        return violations;
    }

    /**
     * Representation of an FLS Validation. Instances of this class should be managed through the
     * outer class FlsValidationRepresentation.
     */
    public static class Info implements DeepCloneable<Info> {
        private String objectName;
        private ApexValue<?> objectValue;
        private TreeSet<String> fieldNames = CollectionUtil.newTreeSet();
        private HashSet<ApexValue<?>> fieldValues = new HashSet<>();
        private boolean allFields; // defaults to false
        private FlsValidationType validationType;

        private static final String ALREADY_OCCUPIED_ERROR =
                "Invalid attempt to set value of %1$s to %2$s while %1$s has already been set to %3$s";

        private Info() {}

        /* used only in unit tests*/
        Info(String objectName, String fieldName, FlsValidationType validationType) {
            this.objectName = objectName;
            this.fieldNames.add(fieldName);
            this.validationType = validationType;
        }

        Info(Info other) {
            this.validationType = other.validationType;
            this.objectName = other.objectName;
            this.objectValue = CloneUtil.cloneApexValue(other.objectValue);
            this.allFields = other.allFields;
            this.fieldNames = CloneUtil.cloneTreeSet(other.fieldNames);
            this.fieldValues = CloneUtil.cloneHashSet(other.fieldValues);
        }

        private void setObjectName(String objectName) {
            if (StringUtils.isNotEmpty(this.objectName)) {
                throw new UnexpectedException(
                        String.format(
                                ALREADY_OCCUPIED_ERROR, "objectName", objectName, this.objectName));
            }
            this.objectName = objectName;
        }

        private void setObjectValue(ApexValue<?> objectValue) {
            if (this.objectValue != null) {
                throw new UnexpectedException(
                        String.format(
                                ALREADY_OCCUPIED_ERROR,
                                "objectValue",
                                objectValue,
                                this.objectValue));
            }
            this.objectValue = objectValue;
        }

        @SuppressWarnings("PMD.UnusedPrivateMethod") // Used via lambda call
        private void setAllFields() {
            this.allFields = true;
        }

        private void addFieldName(String fieldName) {
            // Filter out fields that don't require access check
            if (!SoqlParserUtil.ALWAYS_ACCESSIBLE_FIELDS.contains(fieldName)) {
                fieldNames.add(fieldName);
            }
        }

        private void addFieldValue(ApexValue<?> fieldValue) {
            // We want to filter out fields that don't require access check,
            // but this is kind of a long shot.
            final Optional<String> stringRepresentationOpt =
                    ApexValueUtil.deriveUserFriendlyDisplayName(fieldValue);

            if (fieldRequiresAccessCheck(stringRepresentationOpt)) {
                // TODO: This is kind of risky since the value may resolve to a variable name
                //  that happens to have a filtered field name.
                fieldValues.add(fieldValue);
            }
        }

        private boolean fieldRequiresAccessCheck(Optional<String> stringRepresentationOpt) {
            return !(stringRepresentationOpt.isPresent()
                    && SoqlParserUtil.ALWAYS_ACCESSIBLE_FIELDS.contains(
                            stringRepresentationOpt.get()));
        }

        @SuppressWarnings("PMD.UnusedPrivateMethod") // Used in multiple places
        private void setValidationType(FlsValidationType validationType) {
            if (this.validationType != null) {
                throw new UnexpectedException(
                        String.format(
                                ALREADY_OCCUPIED_ERROR,
                                "validationType",
                                validationType,
                                this.validationType));
            }
            this.validationType = validationType;
        }

        public String getObjectName() {
            return objectName;
        }

        public ApexValue<?> getObjectValue() {
            return objectValue;
        }

        public TreeSet<String> getFieldNames() {
            return fieldNames;
        }

        public HashSet<ApexValue<?>> getFieldValues() {
            return fieldValues;
        }

        public boolean isAllFields() {
            return allFields;
        }

        public FlsValidationType getValidationType() {
            return validationType;
        }

        /**
         * Handwritten equals() and hashcode() so that object name can be compared case
         * insensitively
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Info)) return false;
            Info that = (Info) o;
            return (this.fieldNames.containsAll(that.fieldNames))
                    && (Objects.equals(this.fieldValues, that.fieldValues))
                    && (Objects.equals(this.objectValue, that.objectValue))
                    && (StringUtils.equalsIgnoreCase(this.objectName, that.objectName))
                    && (Objects.equals(this.validationType, that.validationType))
                    && this.allFields == that.allFields;
        }

        @Override
        public int hashCode() {
            final List<String> fieldNamesList =
                    fieldNames.stream().map(String::toLowerCase).collect(Collectors.toList());
            return Objects.hash(
                    StringUtils.toRootLowerCase(objectName),
                    objectValue,
                    fieldValues,
                    validationType,
                    fieldNamesList,
                    allFields);
        }

        @Override
        public String toString() {
            return "Info{"
                    + "objectName='"
                    + objectName
                    + '\''
                    + ", objectValue="
                    + objectValue
                    + ", fieldNames="
                    + fieldNames
                    + ", fieldValues="
                    + fieldValues
                    + ", allFields="
                    + allFields
                    + ", validationType="
                    + validationType
                    + '}';
        }

        @Override
        public Info deepClone() {
            return new Info(this);
        }
    }
}
