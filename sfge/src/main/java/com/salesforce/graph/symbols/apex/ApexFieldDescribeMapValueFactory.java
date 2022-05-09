package com.salesforce.graph.symbols.apex;

import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.MethodCallApexValueBuilder;
import com.salesforce.graph.symbols.apex.schema.DescribeFieldResult;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import java.util.List;
import java.util.Optional;

/** Generates {@link ApexFieldDescribeMapValue}s from methods. */
public final class ApexFieldDescribeMapValueFactory {
    public static final MethodCallApexValueBuilder METHOD_CALL_BUILDER_FUNCTION =
            (g, vertex, symbols) -> {
                String methodName = vertex.getMethodName();
                List<String> chainedNames = vertex.getChainedNames();
                if (methodName.equalsIgnoreCase(DescribeFieldResult.METHOD_GET_MAP)) {
                    // Schema.SObjectType.Account.fields.getMap()
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
                                if (chainedNames.size() == 5) {
                                    schema = chainedNames.get(schemaIndex);
                                }
                                if (schema == null
                                        || schema.equalsIgnoreCase(
                                                ApexStandardLibraryUtil.VariableNames.SCHEMA)) {
                                    ApexStringValue objectName =
                                            ApexValueBuilder.getWithoutSymbolProvider()
                                                    .buildString(chainedNames.get(objectNameIndex));
                                    SObjectType sObjectType =
                                            ApexValueBuilder.getWithoutSymbolProvider()
                                                    .valueVertex(vertex)
                                                    .buildSObjectType(objectName);
                                    return Optional.of(
                                            ApexValueBuilder.getWithoutSymbolProvider()
                                                    .valueVertex(vertex)
                                                    .buildApexFieldDescribeMapValue(sObjectType));
                                }
                            }
                        }
                    }
                }
                return Optional.empty();
            };

    private ApexFieldDescribeMapValueFactory() {}
}
