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

/**
 * Generates {@link DescribeSObjectResult}s from methods and variable expressions. This code has a
 * lot of redundancy from looking for "Schema.SObjectType" and "SObjectType".
 */
public final class DescribeSObjectResultFactory {
    private static final String NAME_PROPERTY = "Name";
    /**
     * Chained names contain the prefixes to the variable. The last name in the dotted chain is
     * considered the variable name (and hence, the vertex name) and the rest are stored in the
     * array. For example, Schema.SObjectType.Account becomes: <VariableExpression BeginColumn='38'
     * BeginLine='3' DefiningType='MyClass' EndLine='3' Image='Account' RealLoc='true'>
     * <ReferenceExpression BeginColumn='19' BeginLine='3' Context='' DefiningType='MyClass'
     * EndLine='3' Image='Schema' Names='[Schema, ObjectType]' RealLoc='true' ReferenceType='LOAD'
     * SafeNav='false' /> </VariableExpression> In the steps below, we look into the chain of names
     * to determine the corresponding ApexValue.
     *
     * <p>Possibilities covered so far:
     *
     * <ul>
     *   <li>SObjectType.Account
     *   <li>Schema.SObjectType.Account
     *   <li>SObjectType.MyObject__c.Name
     *   <li>Schema.SObjectType.MyObject__c.Name
     * </ul>
     */
    public static final VariableExpressionApexValueBuilder VARIABLE_EXPRESSION_BUILDER_FUNCTION =
            vertex -> {
                final List<String> chainedNames = vertex.getChainedNames();
                final String vertexName = vertex.getName();

                final Optional<String> sObjectName =
                        SObjectTypeChainUtil.getSObjectName(chainedNames, vertex);

                if (sObjectName.isPresent()) {
                    // SObjectType.MyObject__c.Name
                    // Schema.SObjectType.MyObject__c.Name
                    if (NAME_PROPERTY.equalsIgnoreCase(vertexName)) {
                        // TODO: how do we handle other fields with individual types?
                        //  Stretch goal when we get to parsing field definitions of SObjects
                        return getApexStringValue(sObjectName.get());
                    } else if (ApexStandardLibraryUtil.VariableNames.S_OBJECT_TYPE.equalsIgnoreCase(
                            vertexName)) {
                        // Schema.SObjectType.Account.SObjectType is a SObjectType value.
                        //  Reject this as a {@link DescribeSObjectResult}
                        // TODO: streamline all of this
                        return Optional.empty();
                    }

                    // SObjectType.Account
                    // Schema.SObjectType.Account
                    return getDescribeSObjectResult(sObjectName.get());
                }

                return Optional.empty();
            };

    /**
     * Helps create ApexValues based on {@link MethodCallExpressionVertex}'s method name. For
     * example, Account.SObjectType.getDescribe() would become: <MethodCallExpression
     * BeginColumn='72' BeginLine='3' DefiningType='MyClass' EndLine='3'
     * FullMethodName='Account.SObjectType.getDescribe' Image='' MethodName='getDescribe'
     * RealLoc='true'> <ReferenceExpression BeginColumn='48' BeginLine='3' Context=''
     * DefiningType='MyClass' EndLine='3' Image='Account' Names='[Account, SObjectType]'
     * RealLoc='true' ReferenceType='METHOD' SafeNav='false' /> </MethodCallExpression>
     *
     * <p>These are methods that can be executed to either return a {@link DescribeSObjectResult} or
     * methods that can be executed on a series of chained names that resolve to a {@link
     * DescribeSObjectResult}. Examples of what's covered here:
     *
     * <ul>
     *   <li>Account.SObjectType.getDescribe()
     *   <li>Schema.SObjectType.Account.isDeletable()
     *   <li>SObjectType.Account.isDeletable()
     * </ul>
     */
    public static final MethodCallApexValueBuilder METHOD_CALL_BUILDER_FUNCTION =
            (g, vertex, symbols) -> {
                String methodName = vertex.getMethodName();
                List<String> chainedNames = vertex.getChainedNames();

                Optional<String> sObjectName =
                        SObjectTypeChainUtil.getSObjectName(chainedNames, vertex);
                if (sObjectName.isPresent()) {
                    // Account.SObjectType.getDescribe()
                    if (methodName.equalsIgnoreCase(SObjectType.METHOD_GET_DESCRIBE)) {
                        return getDescribeSObjectResult(vertex, sObjectName.get());
                    } else if (SystemNames.DML_OBJECT_ACCESS_METHODS.contains(methodName)) {
                        // Schema.SObjectType.Account.isDeletable()
                        // SObjectType.Account.isDeletable()
                        return getDmlAccessValue(vertex, sObjectName.get());
                    }
                }

                return Optional.empty();
            };

    /**
     * Builds {@link com.salesforce.graph.symbols.apex.ApexBooleanValue} to be returned when a Dml
     * access value is needed.
     */
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

    /**
     * Provides {@link com.salesforce.graph.symbols.apex.ApexStringValue}
     *
     * @param objectName
     * @return
     */
    private static Optional<ApexValue<?>> getApexStringValue(String objectName) {
        SObjectType sObjectType =
                ApexValueBuilder.getWithoutSymbolProvider().buildSObjectType(objectName);
        DescribeSObjectResult describeSObjectResult =
                ApexValueBuilder.getWithoutSymbolProvider().buildDescribeSObjectResult(sObjectType);
        return Optional.of(
                ApexValueBuilder.getWithoutSymbolProvider()
                        .returnedFrom(describeSObjectResult, null)
                        .buildString(objectName));
    }

    /** Get a {@link DescribeSObjectResult} value for a given sObject type */
    private static Optional<ApexValue<?>> getDescribeSObjectResult(String SObjectTypeName) {
        SObjectType apexValueSObjectType =
                ApexValueBuilder.getWithoutSymbolProvider().buildSObjectType(SObjectTypeName);
        return Optional.of(
                ApexValueBuilder.getWithoutSymbolProvider()
                        .returnedFrom(apexValueSObjectType, null)
                        .buildDescribeSObjectResult(apexValueSObjectType));
    }

    /** Get {@link DescribeSObjectResult} as a return value for a method call. */
    private static Optional<ApexValue<?>> getDescribeSObjectResult(
            MethodCallExpressionVertex vertex, String sObjectName) {
        SObjectType sObjectType =
                ApexValueBuilder.getWithoutSymbolProvider()
                        .valueVertex(vertex)
                        .buildSObjectType(sObjectName);
        return Optional.of(
                ApexValueBuilder.getWithoutSymbolProvider()
                        .valueVertex(vertex)
                        .returnedFrom(sObjectType, vertex)
                        .buildDescribeSObjectResult(sObjectType));
    }

    private DescribeSObjectResultFactory() {}
}
