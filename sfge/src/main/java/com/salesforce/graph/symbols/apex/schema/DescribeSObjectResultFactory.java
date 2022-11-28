package com.salesforce.graph.symbols.apex.schema;

import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.MethodCallApexValueBuilder;
import com.salesforce.graph.symbols.VariableExpressionApexValueBuilder;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.SystemNames;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import java.util.List;
import java.util.Optional;

/** Generates {@link DescribeSObjectResult}s from methods and variable expressions. */
public final class DescribeSObjectResultFactory {
    private static final String NAME_PROPERTY = "Name";
    public static final VariableExpressionApexValueBuilder VARIABLE_EXPRESSION_BUILDER_FUNCTION =
            vertex -> {
                List<String> chainedNames = vertex.getChainedNames();

                if (chainedNames.size() == 1) {
                    // SObjectType.Account
                    // <VariableExpression BeginColumn='38' BeginLine='3' DefiningType='MyClass'
                    // EndLine='3' Image='Account' RealLoc='true'>
                    //    <ReferenceExpression BeginColumn='19' BeginLine='3' Context=''
                    // DefiningType='MyClass' EndLine='3' Image='Schema' Names='[
                    // SObjectType]' RealLoc='true' ReferenceType='LOAD' SafeNav='false' />
                    // </VariableExpression>

                    final int sObjectTypeIndex = 0;

                    final String sObjectType = chainedNames.get(sObjectTypeIndex);
                    final String nameProperty = vertex.getName();

                    return getSObjectTypeValue(sObjectType, nameProperty);
                } else if (chainedNames.size() == 2) {
                    {
                        // Schema.SObjectType.Account
                        // <VariableExpression BeginColumn='38' BeginLine='3' DefiningType='MyClass'
                        // EndLine='3' Image='Account' RealLoc='true'>
                        //    <ReferenceExpression BeginColumn='19' BeginLine='3' Context=''
                        // DefiningType='MyClass' EndLine='3' Image='Schema' Names='[Schema,
                        // SObjectType]' RealLoc='true' ReferenceType='LOAD' SafeNav='false' />
                        // </VariableExpression>

                        final int sObjectTypeIndex = chainedNames.size() - 1;
                        final int schemaIndex = chainedNames.size() - 2;

                        final String nameProperty = vertex.getName();
                        final String sObjectType = chainedNames.get(sObjectTypeIndex);
                        final String schema = chainedNames.get(schemaIndex);

                        if (schema.equalsIgnoreCase(
                            ApexStandardLibraryUtil.VariableNames.SCHEMA)) {
                            return getSObjectTypeValue(sObjectType, nameProperty);
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

    private static Optional<ApexValue<?>> getSObjectTypeValue(String sObjectType, String nameProperty) {
        if (sObjectType.equalsIgnoreCase(
            ApexStandardLibraryUtil.VariableNames.S_OBJECT_TYPE)) {
            SObjectType apexValueSObjectType =
                    ApexValueBuilder.getWithoutSymbolProvider()
                            .buildSObjectType(nameProperty);
            return Optional.of(
                ApexValueBuilder.getWithoutSymbolProvider()
                    .returnedFrom(apexValueSObjectType, null)
                    .buildDescribeSObjectResult(apexValueSObjectType));
        }
        return Optional.empty();
    }

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
                } else if (SystemNames.DML_OBJECT_ACCESS_METHODS.contains(methodName)) {
                    // Schema.SObjectType.Account.isDeletable()
                    if (chainedNames.size() == 3
                            && chainedNames
                                    .get(0)
                                    .equalsIgnoreCase(ApexStandardLibraryUtil.VariableNames.SCHEMA)
                            && chainedNames
                                    .get(1)
                                    .equalsIgnoreCase(
                                            ApexStandardLibraryUtil.VariableNames.S_OBJECT_TYPE)) {
                        return getDmlAccessValue(vertex, chainedNames.get(2));

                    // SObjectType.Account.isDeletable()
                    } else if (chainedNames.size() == 2
                            && chainedNames
                                    .get(0)
                                    .equalsIgnoreCase(
                                            ApexStandardLibraryUtil.VariableNames.S_OBJECT_TYPE)) {
                        return getDmlAccessValue(vertex, chainedNames.get(1));
                    }
                }
                return Optional.empty();
            };

    private static Optional<ApexValue<?>> getDmlAccessValue(
            MethodCallExpressionVertex vertex, String associatedObjectType) {
        SObjectType sObjectType =
                ApexValueBuilder.getWithoutSymbolProvider()
                        .valueVertex(vertex)
                        .buildSObjectType(associatedObjectType);
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

    private DescribeSObjectResultFactory() {}
}
