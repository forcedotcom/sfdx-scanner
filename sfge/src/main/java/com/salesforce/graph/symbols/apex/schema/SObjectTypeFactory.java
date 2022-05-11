package com.salesforce.graph.symbols.apex.schema;

import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.VariableExpressionApexValueBuilder;
import com.salesforce.graph.symbols.apex.ApexSoqlValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import java.util.List;
import java.util.Optional;

/** Generates {@link ApexSoqlValue}s from variable expressions. */
public final class SObjectTypeFactory {
    public static final VariableExpressionApexValueBuilder VARIABLE_EXPRESSION_BUILDER_FUNCTION =
            vertex -> {
                List<String> chainedNames = vertex.getChainedNames();

                if (!chainedNames.isEmpty()) {
                    // TODO: This might be too inclusive, test edge cases with names that conflict.
                    // This should only resolve
                    // if there isn't an item in scope with the same name
                    if (ApexStandardLibraryUtil.VariableNames.S_OBJECT_TYPE.equalsIgnoreCase(
                            vertex.getName())) {
                        // MyObject__c.SObjectType
                        // <VariableExpression BeginColumn='38' BeginLine='3' DefiningType='MyClass'
                        // EndLine='3' Image='SObjectType' RealLoc='true'>
                        //    <ReferenceExpression BeginColumn='19' BeginLine='3' Context=''
                        // DefiningType='MyClass' EndLine='3' Image='Schema' Names='[MyObject__c]'
                        // RealLoc='true' ReferenceType='LOAD' SafeNav='false' />
                        // </VariableExpression>
                        return Optional.of(
                                ApexValueBuilder.getWithoutSymbolProvider()
                                        .buildSObjectType(
                                                chainedNames.get(chainedNames.size() - 1)));
                    }
                }

                return Optional.empty();
            };

    private SObjectTypeFactory() {}
}
