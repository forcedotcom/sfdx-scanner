package com.salesforce.graph.symbols.apex.schema;

import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.MethodCallApexValueBuilder;
import com.salesforce.graph.symbols.VariableExpressionApexValueBuilder;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.SystemNames;
import java.util.List;
import java.util.Optional;

/** Generates {@link DescribeSObjectResult}s from methods and variable expressions. */
public final class DescribeSObjectResultFactory {
    private static final String NAME_PROPERTY = "Name";
    public static final VariableExpressionApexValueBuilder VARIABLE_EXPRESSION_BUILDER_FUNCTION =
            vertex -> {
                List<String> chainedNames = vertex.getChainedNames();

                if (chainedNames.size() == 2) {
                    {
                        // Schema.SObjectType.Account
                        // <VariableExpression BeginColumn='38' BeginLine='3' DefiningType='MyClass'
                        // EndLine='3' Image='Account' RealLoc='true'>
                        //    <ReferenceExpression BeginColumn='19' BeginLine='3' Context=''
                        // DefiningType='MyClass' EndLine='3' Image='Schema' Names='[Schema,
                        // SObjectType]' RealLoc='true' ReferenceType='LOAD' SafeNav='false' />
                        // </VariableExpression>
                        // TODO: Is SObjectType.Account valid?
                        int sObjectTypeIndex = chainedNames.size() - 1;
                        int schemaIndex = chainedNames.size() - 2;

                        String nameProperty = vertex.getName();
                        String sObjectType = chainedNames.get(sObjectTypeIndex);
                        String schema = chainedNames.get(schemaIndex);

                        if (sObjectType.equalsIgnoreCase(
                                ApexStandardLibraryUtil.VariableNames.S_OBJECT_TYPE)) {
                            if (schema.equalsIgnoreCase(
                                    ApexStandardLibraryUtil.VariableNames.SCHEMA)) {
                                SObjectType apexValueSObjectType =
                                        ApexValueBuilder.getWithoutSymbolProvider()
                                                .buildSObjectType(nameProperty);
                                return Optional.of(
                                        ApexValueBuilder.getWithoutSymbolProvider()
                                                .returnedFrom(apexValueSObjectType, null)
                                                .buildDescribeSObjectResult(apexValueSObjectType));
                            }
                        }
                    }

                    {
                        // SObjectType.MyObject__c.Name
                        int objectNameIndex = chainedNames.size() - 1;
                        int sObjectTypeIndex = chainedNames.size() - 2;

                        String nameProperty = vertex.getName();
                        String sObjectTypeString = chainedNames.get(sObjectTypeIndex);

                        if (NAME_PROPERTY.equalsIgnoreCase(nameProperty)) {
                            if (sObjectTypeString.equalsIgnoreCase(
                                    ApexStandardLibraryUtil.VariableNames.S_OBJECT_TYPE)) {
                                String objectName = chainedNames.get(objectNameIndex);
                                SObjectType sObjectType =
                                        ApexValueBuilder.getWithoutSymbolProvider()
                                                .buildSObjectType(objectName);
                                DescribeSObjectResult describeSObjectResult =
                                        ApexValueBuilder.getWithoutSymbolProvider()
                                                .buildDescribeSObjectResult(sObjectType);
                                return Optional.of(
                                        ApexValueBuilder.getWithoutSymbolProvider()
                                                .returnedFrom(describeSObjectResult, null)
                                                .buildString(objectName));
                            }
                        }
                    }
                }

                return Optional.empty();
            };

    public static final MethodCallApexValueBuilder METHOD_CALL_BUILDER_FUNCTION =
            (g, vertex, symbols) -> {
                String methodName = vertex.getMethodName();
                List<String> chainedNames = vertex.getChainedNames();

                if (methodName.equalsIgnoreCase(SObjectType.METHOD_GET_DESCRIBE)
                        && chainedNames.size() == 2) {
                    // Account.SObjectType.getDescribe()
                    // <MethodCallExpression BeginColumn='72' BeginLine='3' DefiningType='MyClass'
                    // EndLine='3' FullMethodName='Account.SObjectType.getDescribe' Image=''
                    // MethodName='getDescribe' RealLoc='true'>
                    //    <ReferenceExpression BeginColumn='48' BeginLine='3' Context=''
                    // DefiningType='MyClass' EndLine='3' Image='Account' Names='[Account,
                    // SObjectType]' RealLoc='true' ReferenceType='METHOD' SafeNav='false' />
                    // </MethodCallExpression>
                    if (chainedNames
                            .get(1)
                            .equalsIgnoreCase(
                                    ApexStandardLibraryUtil.VariableNames.S_OBJECT_TYPE)) {
                        SObjectType sObjectType =
                                ApexValueBuilder.getWithoutSymbolProvider()
                                        .valueVertex(vertex)
                                        .buildSObjectType(chainedNames.get(0));
                        return Optional.of(
                                ApexValueBuilder.getWithoutSymbolProvider()
                                        .valueVertex(vertex)
                                        .returnedFrom(sObjectType, vertex)
                                        .buildDescribeSObjectResult(sObjectType));
                    }
                } else if (SystemNames.DML_OBJECT_ACCESS_METHODS.contains(methodName)
                        && chainedNames.size() == 3) {
                    // Schema.SObjectType.Account.isDeletable()
                    if (chainedNames
                                    .get(0)
                                    .equalsIgnoreCase(ApexStandardLibraryUtil.VariableNames.SCHEMA)
                            && chainedNames
                                    .get(1)
                                    .equalsIgnoreCase(
                                            ApexStandardLibraryUtil.VariableNames.S_OBJECT_TYPE)) {
                        SObjectType sObjectType =
                                ApexValueBuilder.getWithoutSymbolProvider()
                                        .valueVertex(vertex)
                                        .buildSObjectType(chainedNames.get(2));
                        final DescribeSObjectResult describeSObjectResult =
                                ApexValueBuilder.getWithoutSymbolProvider()
                                        .valueVertex(vertex)
                                        .returnedFrom(sObjectType, vertex)
                                        .buildDescribeSObjectResult(sObjectType);
                        return Optional.of(
                                ApexValueBuilder.getWithoutSymbolProvider()
                                        .returnedFrom(describeSObjectResult, vertex)
                                        .buildBoolean());
                    }
                }
                return Optional.empty();
            };

    private DescribeSObjectResultFactory() {}
}
