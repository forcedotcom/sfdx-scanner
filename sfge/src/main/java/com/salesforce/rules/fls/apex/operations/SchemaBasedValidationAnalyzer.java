package com.salesforce.rules.fls.apex.operations;

import com.google.common.collect.Sets;
import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexBooleanValue;
import com.salesforce.graph.symbols.apex.ApexCustomValue;
import com.salesforce.graph.symbols.apex.ApexForLoopValue;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.schema.DescribeFieldResult;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BooleanExpressionVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InstanceOfExpressionVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import java.util.*;

/**
 * Checks if a given vertex is a Schema-based validation such as:
 * Schema.SObjectType.Account.Fields.Name.isCreateable() or
 * Schema.getGlobalDescribe('Account').getDescribe().isDeletable()
 */
public class SchemaBasedValidationAnalyzer {
    private final SymbolProvider symbols;

    public SchemaBasedValidationAnalyzer(SymbolProvider symbols) {
        this.symbols = symbols;
    }

    /**
     * @param vertex to analyze if we have a schema-based validation
     * @return a Set of validation info. If no schema-based validation was detected, returns an
     *     empty Set.
     */
    public Set<FlsValidationRepresentation.Info> checkForValidation(
            BaseSFVertex parent, BaseSFVertex vertex) {
        // Fetch ApexBooleanValue from vertex.
        if (!(vertex instanceof BooleanExpressionVertex)
                && !(vertex instanceof MethodCallExpressionVertex)
                && !(vertex instanceof InstanceOfExpressionVertex)
                && !(vertex instanceof LiteralExpressionVertex)
                && !(vertex instanceof VariableExpressionVertex)) {
            throw new TodoException(
                    "Vertex passed in to detect schema-based validation is not a BooleanExpressionVertex, MethodCallExpressionVertex, InstanceOfExpressionVertex, LiteralExpressionVertex, or VariableExpressionVertex: parent="
                            + parent
                            + ", vertex="
                            + vertex);
        }

        // Literals absolutely can't have validations.
        HashSet<FlsValidationRepresentation.Info> results = Sets.newHashSet();
        if (vertex instanceof LiteralExpressionVertex) {
            return results;
        }

        final Optional<ApexValue<?>> apexValueOptional =
                ScopeUtil.resolveToApexValue(symbols, (ChainedVertex) vertex);

        if (!apexValueOptional.isPresent()) {
            // If standard condition does not resolve to an ApexValue, there isn't much we can do
            return results;
        }

        ApexValue<?> apexValue = apexValueOptional.get();
        List<ApexBooleanValue> apexBooleanValues = getDerivedApexValue(parent, vertex, apexValue);

        for (ApexBooleanValue booleanValue : apexBooleanValues) {
            results.addAll(convert(booleanValue));
        }

        return results;
    }

    private List<ApexBooleanValue> getDerivedApexValue(
            BaseSFVertex parent, BaseSFVertex vertex, ApexValue<?> apexValue) {
        if (!(apexValue instanceof ApexBooleanValue)
                && !(apexValue instanceof ApexCustomValue)
                && !(apexValue instanceof ApexForLoopValue)
                && !(apexValue instanceof ApexSingleValue)) {
            throw new TodoException(
                    "What should I do if ApexValue from StandardCondition is not an ApexBooleanValue, ApexCustomValue, ApexForLoopValue, or ApexSingleValue: ApexValue="
                            + apexValue
                            + ", parent="
                            + parent
                            + ", vertex="
                            + vertex);
        }

        List<ApexBooleanValue> apexValues = new ArrayList<>();

        // Handle cases such as
        // if (myObject) { /*Do Something*/ }

        if (apexValue instanceof ApexBooleanValue) {
            apexValues.add((ApexBooleanValue) apexValue);
        }
        // Convert the value to a boolean that represents if the object was initialized
        if (apexValue instanceof ApexCustomValue) {
            apexValues.add(ApexValueBuilder.getWithoutSymbolProvider().buildBoolean());
        }

        if (apexValue instanceof ApexForLoopValue) {
            List<ApexValue<?>> forLoopValues = ((ApexForLoopValue) apexValue).getForLoopValues();
            for (ApexValue<?> value : forLoopValues) {
                if (value instanceof ApexBooleanValue) {
                    apexValues.add((ApexBooleanValue) value);
                } else {
                    apexValues.addAll(getDerivedApexValue(parent, vertex, value));
                }
            }
        }

        // Convert the value to a boolean that represents if the object was initialized
        if (apexValue instanceof ApexSingleValue) {
            apexValues.add(ApexValueBuilder.getWithoutSymbolProvider().buildBoolean());
        }
        return apexValues;
    }

    private Set<FlsValidationRepresentation.Info> convert(ApexBooleanValue apexBooleanValue) {
        FlsValidationRepresentation validationRep = new FlsValidationRepresentation();

        final Optional<ApexValue<?>> returnedFromOptional = apexBooleanValue.getReturnedFrom();

        final Optional<InvocableVertex> invocableOptional = apexBooleanValue.getInvocable();
        if (!invocableOptional.isPresent()) {
            // We don't have sufficient information to move forward
            return Sets.newHashSet();
        }

        if (!(invocableOptional.get() instanceof MethodCallExpressionVertex)) {
            throw new TodoException(
                    "How to find out validation type if invocable is not a Method? "
                            + invocableOptional.get());
        }

        final Optional<FlsValidationType> validationTypeOptional =
                getValidationType((MethodCallExpressionVertex) invocableOptional.get());
        if (!validationTypeOptional.isPresent()) {
            // We don't have an FLS validation method
            return Sets.newHashSet();
        }

        final FlsValidationType validationType = validationTypeOptional.get();

        Optional<DescribeSObjectResult> describeSObjectResultOptional;
        boolean isObjectLevelCheck = false;
        // Try extracting field result from returned value
        final Optional<DescribeFieldResult> describeFieldResultOptional =
                extractDescribeFieldResult(returnedFromOptional);
        if (describeFieldResultOptional.isPresent()) {
            // Try extracting DescribeSObjectResult from DescribeFieldResult chain
            describeSObjectResultOptional = extractDescribeSObjectType(describeFieldResultOptional);
        } else {
            // Try extracting DescribeSObjectResult from returned value
            describeSObjectResultOptional = extractDescribeSObjectType(returnedFromOptional);
            isObjectLevelCheck = true;
        }

        if (!describeSObjectResultOptional.isPresent()) {
            // We didn't detect a match. No validations to return
            return Sets.newHashSet();
        }

        if (!isCheckLevelValid(
                validationType, describeSObjectResultOptional.get(), isObjectLevelCheck)) {
            // If object expects object-level check and field-level check has been made,
            // or if object expects field-level check, but object-level check has been made,
            // don't go forward.
            return Sets.newHashSet();
        }

        validationRep.setValidationType(validationType);
        validationRep.setObject(describeSObjectResultOptional.get());
        if (describeFieldResultOptional.isPresent()) {
            validationRep.addField(describeFieldResultOptional.get());
        }

        return Sets.newHashSet(validationRep.getValidationInfo());
    }

    /**
     * @return true if the current level of check is the same as what's expected of the object
     */
    private boolean isCheckLevelValid(
            FlsValidationType validationType,
            DescribeSObjectResult describeSObjectResult,
            boolean isObjectLevelCheck) {
        final boolean isObjectLevelCheckExpected =
                ObjectBasedCheckUtil.isCrudCheckExpected(describeSObjectResult, validationType);

        // If objectLevelCheck and objectLevelCheckExpected don't match,
        // return as invalid.
        return isObjectLevelCheckExpected == isObjectLevelCheck;
    }

    private Optional<FlsValidationType> getValidationType(
            MethodCallExpressionVertex methodCallExpressionVertex) {
        final String methodName = methodCallExpressionVertex.getMethodName();
        return FlsValidationType.getValidationType(methodName);
    }

    private Optional<DescribeFieldResult> extractDescribeFieldResult(
            Optional<? extends ApexValue<?>> apexValueOptional) {
        if (!apexValueOptional.isPresent()) {
            return Optional.empty();
        }
        final ApexValue<?> apexValue = apexValueOptional.get();

        if (!(apexValue instanceof DescribeFieldResult)) {
            return Optional.empty();
        }

        final DescribeFieldResult describeFieldResult = (DescribeFieldResult) apexValue;
        return Optional.of(describeFieldResult);
    }

    private Optional<DescribeSObjectResult> extractDescribeSObjectType(
            Optional<? extends ApexValue<?>> apexValueOptional) {
        if (!apexValueOptional.isPresent()) {
            return Optional.empty();
        }
        final ApexValue<?> apexValue = apexValueOptional.get();
        DescribeSObjectResult describeSObjectResult;

        if (apexValue instanceof DescribeFieldResult) {
            final DescribeFieldResult describeFieldResult = (DescribeFieldResult) apexValue;
            final Optional<DescribeSObjectResult> describeSObjectResultOptional =
                    describeFieldResult.getDescribeSObjectResult();
            if (!describeSObjectResultOptional.isPresent()) {
                throw new UnexpectedException(
                        "DescribeFieldResult does not have an associated DescribeSObjectResult: "
                                + describeFieldResult);
            }
            describeSObjectResult = describeSObjectResultOptional.get();
        } else if (apexValue instanceof DescribeSObjectResult) {
            describeSObjectResult = (DescribeSObjectResult) apexValue;
        } else {
            // Unknown type, which probably means this is not a match
            return Optional.empty();
        }

        return Optional.of(describeSObjectResult);
    }
}
