package com.salesforce.graph.symbols.apex.schema;

import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.TypeableUtil;
import com.salesforce.graph.symbols.VariableExpressionApexValueBuilder;
import com.salesforce.graph.symbols.apex.ApexSoqlValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import java.util.List;
import java.util.Optional;

/** Generates {@link ApexSoqlValue}s from variable expressions. */
@SuppressWarnings(
        "PMD.RedundantFieldInitializer") // SCHEMA_INDEX is intentionally zero to provide context
public final class SObjectFieldFactory {

    public static final VariableExpressionApexValueBuilder VARIABLE_EXPRESSION_BUILDER_FUNCTION =
            vertex -> {
                List<String> chainedNames = vertex.getChainedNames();

                // TODO: use DescribeSObjectResult to construct SObjectType portion
                // Schema.<objectName>.fields.<fieldName>
                if (chainedNames.size() == 3) {
                    final int schemaIndex = 0;
                    final int objectNameIndex = 1;
                    final int fieldsIndex = 2;
                    if (ApexStandardLibraryUtil.VariableNames.SCHEMA.equalsIgnoreCase(
                                    chainedNames.get(schemaIndex))
                            && ApexStandardLibraryUtil.VariableNames.FIELDS.equalsIgnoreCase(
                                    chainedNames.get(fieldsIndex))) {

                        return getSObjectFieldValue(
                                chainedNames.get(objectNameIndex), vertex.getName());
                    }
                } else if (chainedNames.size() == 1) {
                    // Account.Name
                    final int objectNameIndex = 0;
                    final String fieldNameCandidate = vertex.getName();
                    if (TypeableUtil.isDataObject(chainedNames.get(objectNameIndex))
                            && !ApexStandardLibraryUtil.VariableNames.S_OBJECT_TYPE
                                    .equalsIgnoreCase(vertex.getName())) {
                        return getSObjectFieldValue(
                                chainedNames.get(objectNameIndex), fieldNameCandidate);
                    }
                }

                return Optional.empty();
            };

    private static Optional<ApexValue<?>> getSObjectFieldValue(
            String associatedObjectType, String fieldNameString) {
        final SObjectType objectType =
                ApexValueBuilder.getWithoutSymbolProvider().buildSObjectType(associatedObjectType);
        final ApexStringValue fieldName =
                ApexValueBuilder.getWithoutSymbolProvider().buildString(fieldNameString);

        return Optional.of(
                ApexValueBuilder.getWithoutSymbolProvider()
                        .buildSObjectField(objectType, fieldName));
    }

    private SObjectFieldFactory() {}
}
