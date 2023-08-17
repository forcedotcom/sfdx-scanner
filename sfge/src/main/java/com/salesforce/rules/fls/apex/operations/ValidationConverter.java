package com.salesforce.rules.fls.apex.operations;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ops.SoqlParserUtil;
import com.salesforce.graph.symbols.ObjectProperties;
import com.salesforce.graph.symbols.apex.ApexCustomValue;
import com.salesforce.graph.symbols.apex.ApexForLoopValue;
import com.salesforce.graph.symbols.apex.ApexListValue;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexSoqlValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.SoqlQueryInfo;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.vertex.SoqlExpressionVertex;
import com.salesforce.rules.fls.apex.operations.FlsConstants.AnalysisLevel;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.rules.fls.apex.operations.FlsConstants.ProcessFields;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Converts vertices into validation object(s) that are expected in the given scenario. */
public class ValidationConverter {
    private static final Logger LOGGER = LogManager.getLogger(ValidationConverter.class);
    private final FlsValidationType validationType;
    private final boolean userModeOverride;

    public ValidationConverter(FlsValidationType validationType) {
        this(validationType, false);
    }

    /**
     * @param validationType the {@link FlsValidationType} to check for
     * @param isUserModeOverride indicates whether the operation is accompanied by a User Mode
     *     access level
     */
    public ValidationConverter(FlsValidationType validationType, boolean isUserModeOverride) {
        this.validationType = validationType;
        this.userModeOverride = isUserModeOverride;
    }

    /** Converts SoqlExpressions to Validation representations */
    public Set<FlsValidationRepresentation> convert(SoqlExpressionVertex vertex) {
        // detect if validation was checked on these fields
        final ValidationHolder holder = new ValidationHolder();
        final ProcessFields defaultProcessFields = ProcessFields.INDIVIDUAL;
        final HashSet<SoqlQueryInfo> queryInfos = vertex.getQueryInfo();
        convertSoqlQueryInfo(holder, defaultProcessFields, queryInfos);

        return holder.getValidationReps();
    }

    /** Converts given apexValue to Validation Representations. */
    public Set<FlsValidationRepresentation> convertToExpectedValidations(ApexValue<?> apexValue) {
        final ValidationHolder holder =
                this.getHolder(apexValue, validationType.getProcessFields());
        return holder.getValidationReps();
    }

    private ValidationHolder getHolder(ApexValue<?> childApexValue, ProcessFields processFields) {
        final ValidationHolder holder = new ValidationHolder();

        if (childApexValue instanceof ApexListValue) {
            final ApexListValue apexListValue = (ApexListValue) childApexValue;
            final List<ApexValue<?>> apexValues = apexListValue.getValues();
            if (apexValues.isEmpty() && apexListValue.getListType().isPresent()) {
                // Fetch type based on type declaration
                // TODO: we don't know yet what fields will be added. Passing an empty list for now.
                addNecessaryValidationsToHolder(
                        holder,
                        processFields,
                        apexListValue.getListType().get().getCanonicalType(),
                        null,
                        CollectionUtil.newTreeSet(),
                        new HashSet<>());
            } else {
                extractFromList(processFields, holder, apexValues);
            }
        } else if (childApexValue instanceof ApexSoqlValue) {
            extractFromSoqlValue((ApexSoqlValue) childApexValue, processFields, holder);
        } else if (childApexValue instanceof ApexStringValue) {
            extractFromStringValue((ApexStringValue) childApexValue, processFields, holder);
        } else if (childApexValue instanceof ApexCustomValue) {
            // For custom settings, we always want to process at object level
            extractSObjectInformation(childApexValue, ProcessFields.NONE, holder);
        } else if (childApexValue instanceof ApexSingleValue) {
            extractSObjectInformation(childApexValue, processFields, holder);
        } else if (childApexValue instanceof ApexForLoopValue) {
            extractFromList(
                    processFields, holder, ((ApexForLoopValue) childApexValue).getForLoopValues());
        } else {
            // Only a Single SObject or a List of SObjects can be handled by DmlStatementVertex.
            throw new TodoException(
                    "ApexValue is not handled for converting into validations: " + childApexValue);
        }

        return holder;
    }

    private void extractFromList(
            ProcessFields processFields, ValidationHolder holder, List<ApexValue<?>> apexValues) {
        for (ApexValue apexValue : apexValues) {
            if (apexValue instanceof ApexSoqlValue) {
                extractFromSoqlValue((ApexSoqlValue) apexValue, processFields, holder);
            } else if (apexValue instanceof ApexCustomValue) {
                // For custom settings, we always want to process at object level
                extractSObjectInformation(apexValue, ProcessFields.NONE, holder);
            } else if (apexValue instanceof ObjectProperties) {
                extractSObjectInformation(apexValue, processFields, holder);
            } else {
                // DML statements don't take list of lists and other such complex objects
                throw new UnexpectedException(apexValue);
            }
        }
    }

    private void extractFromSoqlValue(
            ApexSoqlValue soqlValue, ProcessFields processFields, ValidationHolder holder) {
        final HashSet<SoqlQueryInfo> processedQueries = soqlValue.getProcessedQueries();

        if (processedQueries.isEmpty()) {
            throw new UnexpectedException(
                    "Did not expect empty query information. soqlValue=" + soqlValue);
        }

        convertSoqlQueryInfo(holder, processFields, processedQueries);

        // Add fields access through key/value properties
        // For example, we want to add Id, Name, Description in:
        //
        // Account a = [SELECT Id, Name FROM Account LIMIT 1];
        // a.Description = 'some description';
        //
        // Adding these keys only to the outermost query in Soql since the overall
        // type is based on the outermost query's type.
        final Set<ApexValue<?>> propertyKeys = soqlValue.getApexValueProperties().keySet();
        addNecessaryValidationsToHolder(
                holder,
                processFields,
                SoqlParserUtil.getObjectName(processedQueries),
                null,
                CollectionUtil.newTreeSet(),
                propertyKeys);
    }

    private void extractFromStringValue(
            ApexStringValue apexStringValue, ProcessFields processFields, ValidationHolder holder) {
        if (apexStringValue.isValuePresent()) {
            final HashSet<SoqlQueryInfo> queryInfos =
                    SoqlParserUtil.parseQuery(apexStringValue.getValue().get());
            convertSoqlQueryInfo(holder, processFields, queryInfos);
        }
    }

    private void extractSObjectInformation(
            ApexValue apexValue, ProcessFields processFields, ValidationHolder holder) {

        final Set<ApexValue<?>> fieldValues = getFieldValues(apexValue);
        final Optional<String> objectNameOptional = getObjectName(apexValue);
        final ApexValue<?> objectValue = getObjectValue(apexValue);

        addNecessaryValidationsToHolder(
                holder,
                processFields,
                objectNameOptional.orElse(null),
                objectValue,
                CollectionUtil.newTreeSet(),
                fieldValues);
    }

    private Set<ApexValue<?>> getFieldValues(ApexValue<?> apexValue) {
        final Set<ApexValue<?>> returnValue = new HashSet<>();

        if (apexValue instanceof ObjectProperties) {
            final Map<ApexValue<?>, ApexValue<?>> apexValueProperties =
                    ((ObjectProperties) apexValue).getApexValueProperties();
            returnValue.addAll(apexValueProperties.keySet());
            if (apexValue.isIndeterminant()) {
                // When apex value is indeterminant, we won't know all fields that have been set on
                // the object, include
                // Unknown as one of the fields
                returnValue.add(buildUnknownFieldValue());
            }
            return returnValue;
        }

        throw new TodoException(
                "What happens if ApexValue does not store properties? apexValue=" + apexValue);
    }

    private ApexStringValue buildUnknownFieldValue() {
        return ApexValueBuilder.getWithoutSymbolProvider().buildString(SoqlParserUtil.UNKNOWN);
    }

    private Optional<String> getObjectName(ApexValue<?> apexValue) {
        final Optional<String> valueVertexType = apexValue.getValueVertexType();
        if (valueVertexType.isPresent()) {
            return valueVertexType;
        }

        return apexValue.getDeclaredType();
    }

    private ApexValue<?> getObjectValue(ApexValue<?> apexValue) {
        final Optional<ApexValue<?>> returnedFromOptional = apexValue.getReturnedFrom();
        if (returnedFromOptional.isPresent()) {
            final ApexValue<?> returnedFrom = returnedFromOptional.get();
            if (returnedFrom instanceof SObjectType) {
                SObjectType sObjectType = (SObjectType) returnedFrom;
                return sObjectType.getType().get();
            }
        }
        return apexValue;
    }

    /** check if validation is necessary. if so, add it to holder. */
    private void convertSoqlQueryInfo(
            ValidationHolder holder,
            ProcessFields defaultProcessFields,
            HashSet<SoqlQueryInfo> queryInfos) {
        for (SoqlQueryInfo queryInfo : queryInfos) {
            if (SoqlParserUtil.isCountQuery(queryInfo)) {
                // TODO: confirm with a security expert
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            "Count-only query does not need FLS check since no fields are looked up",
                            queryInfo);
                }
                continue;
            }

            if (queryInfo.isSecurityEnforced()) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            "A query that has \"WITH SECURITY_ENFORCED\" clause is inherently protected",
                            queryInfo);
                }
                continue;
            }

            if (queryInfo.isUserMode()) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            "A query that has \"WITH USER_MODE\" clause is inherently protected",
                            queryInfo);
                }
                continue;
            }

            if (this.userModeOverride) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            "A method in the Database class with \"USER_MODE\" accessLevel is inherently protected",
                            queryInfo);
                }
                continue;
            }

            if (ObjectBasedCheckUtil.isSpecialObject(queryInfo.getObjectName())) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("A non-FLS object does not need FLS check", queryInfo);
                }
                continue;
            }

            if (!queryInfo.getFieldsRequireAccessCheck()) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("None of the fields require access check", queryInfo);
                }
                continue;
            }

            ProcessFields processFields =
                    queryInfo.isAllFields() ? ProcessFields.ALL : defaultProcessFields;
            addNecessaryValidationsToHolder(
                    holder,
                    processFields,
                    queryInfo.getObjectName(),
                    null,
                    queryInfo.getFields(),
                    new HashSet<>());
        }
    }

    private void addNecessaryValidationsToHolder(
            ValidationHolder holder,
            ProcessFields processFields,
            @Nullable String objectName,
            @Nullable ApexValue<?> objectValue,
            TreeSet<String> fields,
            Set<ApexValue<?>> fieldValues) {
        // Determine the deepest possible check level required for this object type.
        AnalysisLevel requiredAnalysisLevel;
        if (objectName != null) {
            requiredAnalysisLevel =
                    ObjectBasedCheckUtil.getAnalysisLevel(objectName, validationType);
        } else if (objectValue != null) {
            requiredAnalysisLevel =
                    ObjectBasedCheckUtil.getAnalysisLevel(objectValue, validationType);
        } else {
            throw new UnexpectedException("objectName and objectValue cannot both be null");
        }

        ProcessFields updatedProcessFields = processFields;
        if (requiredAnalysisLevel == AnalysisLevel.NONE) {
            // If no analysis is required, we can just be done.
            return;
        } else if (requiredAnalysisLevel == AnalysisLevel.OBJECT_LEVEL) {
            // If only object-level analysis is required, override processFields to reflect this.
            updatedProcessFields = ProcessFields.NONE;
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "Overriding processField value ({}) to {} since object requires object-level access: objectName {} / objectValue {}",
                        processFields,
                        updatedProcessFields,
                        objectName,
                        objectValue);
            }
        }

        if (objectName != null) {
            holder.addByObjectName(updatedProcessFields, objectName, fields, fieldValues);
        } else {
            holder.addByObjectValue(updatedProcessFields, objectValue, fields, fieldValues);
        }
    }

    /**
     * This layer is to ensure that we reuse the same FlsValidationRepresentation object to add
     * fields for the same sobject type. The complexity is heightened due to the fact that both
     * objects and fields could be Strings or ChainedVertices. And also occur in any combination.
     */
    private class ValidationHolder {

        private final Map<String, FlsValidationRepresentation> objNameBasedValidationReps =
                new HashMap<>();
        private final Map<ApexValue<?>, FlsValidationRepresentation> objValueBasedValidationReps =
                new HashMap<>();

        void addByObjectName(
                ProcessFields processFields,
                String objectName,
                TreeSet<String> fields,
                Set<ApexValue<?>> fieldValues) {
            FlsValidationRepresentation validationRep;
            // If a validation rep for this object already exists, we can reuse it.
            if (objNameBasedValidationReps.containsKey(objectName)) {
                validationRep = objNameBasedValidationReps.get(objectName);
            } else {
                validationRep = new FlsValidationRepresentation();
                validationRep.setValidationType(validationType);
                validationRep.setObject(objectName);
                objNameBasedValidationReps.put(objectName, validationRep);
            }
            setFields(processFields, fields, fieldValues, validationRep);
        }

        void addByObjectValue(
                ProcessFields processFields,
                ApexValue<?> objectValue,
                TreeSet<String> fields,
                Set<ApexValue<?>> fieldValues) {
            FlsValidationRepresentation validationRep;
            // If a validation rep for this object already exists, we can reuse it.
            if (objValueBasedValidationReps.containsKey(objectValue)) {
                validationRep = objValueBasedValidationReps.get(objectValue);
            } else {
                validationRep = new FlsValidationRepresentation();
                validationRep.setValidationType(validationType);
                validationRep.setObject(objectValue);
                objValueBasedValidationReps.put(objectValue, validationRep);
            }
            setFields(processFields, fields, fieldValues, validationRep);
        }

        void setFields(
                ProcessFields processFields,
                TreeSet<String> fields,
                Set<ApexValue<?>> fieldValues,
                FlsValidationRepresentation validationRep) {
            switch (processFields) {
                case ALL:
                    validationRep.setAllFields();
                    break;
                case INDIVIDUAL:
                    if (!fields.isEmpty()) validationRep.addFields(fields);
                    if (!fieldValues.isEmpty()) validationRep.addFields(fieldValues);
                    break;
                default:
                    break;
            }
        }

        Set<FlsValidationRepresentation> getValidationReps() {
            final Set<FlsValidationRepresentation> validations = new HashSet<>();
            validations.addAll(objNameBasedValidationReps.values());
            validations.addAll(objValueBasedValidationReps.values());

            return validations;
        }
    }
}
