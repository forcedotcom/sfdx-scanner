package com.salesforce.graph.symbols.apex.schema;

import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.MethodCallApexValueBuilder;
import com.salesforce.graph.symbols.VariableExpressionApexValueBuilder;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.SystemNames;
import com.salesforce.graph.symbols.apex.ValueStatus;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import java.util.List;
import java.util.Optional;

/** Generates {@link DescribeFieldResult}s from methods and variable expressions. */
public final class DescribeFieldResultFactory {
    public static final VariableExpressionApexValueBuilder VARIABLE_EXPRESSION_BUILDER_FUNCTION =
            vertex -> {
                List<String> chainedNames = vertex.getChainedNames();

                // Schema.SObjectType.Account.fields.Name
                // SObjectType.Account.fields.Name
                if (chainedNames.size() == 3 || chainedNames.size() == 4) {
                    int fieldsIndex = chainedNames.size() - 1;
                    int objectNameIndex = chainedNames.size() - 2;
                    int sObjectTypeIndex = chainedNames.size() - 3;
                    int schemaIndex = chainedNames.size() - 4;

                    String fields = chainedNames.get(fieldsIndex);
                    if (fields.equalsIgnoreCase(ApexStandardLibraryUtil.VariableNames.FIELDS)) {
                        String sObjectTypeString = chainedNames.get(sObjectTypeIndex);
                        if (sObjectTypeString.equalsIgnoreCase(
                                ApexStandardLibraryUtil.VariableNames.S_OBJECT_TYPE)) {
                            String schema = null;
                            if (chainedNames.size() == 4) {
                                chainedNames.get(schemaIndex);
                            }
                            if (schema == null
                                    || schema.equalsIgnoreCase(
                                            ApexStandardLibraryUtil.VariableNames.SCHEMA)) {
                                String objectName = chainedNames.get(objectNameIndex);
                                SObjectType sObjectType =
                                        ApexValueBuilder.getWithoutSymbolProvider()
                                                .buildSObjectType(objectName);
                                DescribeSObjectResult describeSObjectResult =
                                        ApexValueBuilder.getWithoutSymbolProvider()
                                                .buildDescribeSObjectResult(sObjectType);
                                ApexStringValue fieldName =
                                        ApexValueBuilder.getWithoutSymbolProvider()
                                                .buildString(vertex.getName());
                                return Optional.of(
                                        ApexValueBuilder.getWithoutSymbolProvider()
                                                .returnedFrom(describeSObjectResult, null)
                                                .buildDescribeFieldResult(
                                                        describeSObjectResult, fieldName));
                            }
                        }
                    }
                }

                return Optional.empty();
            };

    public static final MethodCallApexValueBuilder METHOD_CALL_BUILDER_FUNCTION =
            (g, vertex, symbols) -> {
                final String methodName = vertex.getMethodName();
                final List<String> chainedNames = vertex.getChainedNames();

                if (methodName.equalsIgnoreCase(SObjectType.METHOD_GET_DESCRIBE)
                        && chainedNames.size() == 2) {
                    // Account.Name.getDescribe()
                    final int fieldNameIndex = chainedNames.size() - 1;
                    final int objectNameIndex = chainedNames.size() - 2;

                    return getDescribeFieldResultValue(
                            vertex, chainedNames, fieldNameIndex, objectNameIndex);
                }
                if (methodName.equalsIgnoreCase(SObjectType.METHOD_GET_DESCRIBE)
                        && chainedNames.size() == 3) {
                    // TODO: Do we have to handle Schema.Account.
                    // Account.fields.Name.getDescribe()
                    final int fieldNameIndex = chainedNames.size() - 1;
                    final int fieldsIndex = chainedNames.size() - 2;
                    final int objectNameIndex = chainedNames.size() - 3;

                    final String fields = chainedNames.get(fieldsIndex);
                    if (fields.equalsIgnoreCase(ApexStandardLibraryUtil.VariableNames.FIELDS)) {
                        return getDescribeFieldResultValue(
                                vertex, chainedNames, fieldNameIndex, objectNameIndex);
                    }
                } else if (SystemNames.DML_FIELD_ACCESS_METHODS.contains(methodName)
                        && chainedNames.size() == 5) {
                    // Schema.SObjectType.Account.fields.Name.isCreateable()
                    int fieldNameIndex = chainedNames.size() - 1;
                    int fieldsIndex = chainedNames.size() - 2;
                    int objectNameIndex = chainedNames.size() - 3;
                    int sObjectTypeIndex = chainedNames.size() - 4;
                    int schemaIndex = chainedNames.size() - 5;
                    String fields = chainedNames.get(fieldsIndex);
                    if (fields.equalsIgnoreCase(ApexStandardLibraryUtil.VariableNames.FIELDS)) {
                        String sObjectTypeString = chainedNames.get(sObjectTypeIndex);
                        if (sObjectTypeString.equalsIgnoreCase(
                                ApexStandardLibraryUtil.VariableNames.S_OBJECT_TYPE)) {
                            String schema = chainedNames.get(schemaIndex);
                            if (schema.equalsIgnoreCase(
                                    ApexStandardLibraryUtil.VariableNames.SCHEMA)) {
                                String objectName = chainedNames.get(objectNameIndex);
                                SObjectType sObjectType =
                                        ApexValueBuilder.getWithoutSymbolProvider()
                                                .buildSObjectType(objectName);
                                DescribeSObjectResult describeSObjectResult =
                                        ApexValueBuilder.getWithoutSymbolProvider()
                                                .buildDescribeSObjectResult(sObjectType);
                                ApexStringValue fieldName =
                                        ApexValueBuilder.getWithoutSymbolProvider()
                                                .buildString(chainedNames.get(fieldNameIndex));
                                DescribeFieldResult describeFieldResult =
                                        ApexValueBuilder.getWithoutSymbolProvider()
                                                .buildDescribeFieldResult(
                                                        describeSObjectResult, fieldName);
                                return Optional.of(
                                        ApexValueBuilder.getWithoutSymbolProvider()
                                                .returnedFrom(describeFieldResult, vertex)
                                                .withStatus(ValueStatus.INDETERMINANT)
                                                .buildBoolean());
                            }
                        }
                    }
                }
                return Optional.empty();
            };

    private static Optional<ApexValue<?>> getDescribeFieldResultValue(
            MethodCallExpressionVertex vertex,
            List<String> chainedNames,
            int fieldNameIndex,
            int objectNameIndex) {
        final String objectName = chainedNames.get(objectNameIndex);
        final ApexStringValue fieldName =
                ApexValueBuilder.getWithoutSymbolProvider()
                        .buildString(chainedNames.get(fieldNameIndex));
        final SObjectType sObjectType =
                ApexValueBuilder.getWithoutSymbolProvider().buildSObjectType(objectName);
        final DescribeSObjectResult describeSObjectResult =
                ApexValueBuilder.getWithoutSymbolProvider()
                        .valueVertex(vertex)
                        .buildDescribeSObjectResult(sObjectType);
        return Optional.of(
                ApexValueBuilder.getWithoutSymbolProvider()
                        .valueVertex(vertex)
                        .returnedFrom(describeSObjectResult, vertex)
                        .buildDescribeFieldResult(describeSObjectResult, fieldName));
    }

    private DescribeFieldResultFactory() {}
}
