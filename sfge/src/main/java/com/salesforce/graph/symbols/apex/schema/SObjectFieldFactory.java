package com.salesforce.graph.symbols.apex.schema;

import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.VariableExpressionApexValueBuilder;
import com.salesforce.graph.symbols.apex.ApexSoqlValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import java.util.List;
import java.util.Optional;

/** Generates {@link ApexSoqlValue}s from variable expressions. */
@SuppressWarnings(
        "PMD.RedundantFieldInitializer") // SCHEMA_INDEX is intentionally zero to provide context
public final class SObjectFieldFactory {
    // Schema.<objectName>.fields.<fieldName>
    private static int SCHEMA_INDEX = 0;
    private static int OBJECT_NAME_INDEX = 1;
    private static int FIELDS_INDEX = 2;

    public static final VariableExpressionApexValueBuilder VARIABLE_EXPRESSION_BUILDER_FUNCTION =
            vertex -> {
                List<String> chainedNames = vertex.getChainedNames();

                // TODO: use DescribeSObjectResult to construct SObjectType portion
                if (chainedNames.size() == 3) {
                    if (ApexStandardLibraryUtil.VariableNames.SCHEMA.equalsIgnoreCase(
                                    chainedNames.get(SCHEMA_INDEX))
                            && ApexStandardLibraryUtil.VariableNames.FIELDS.equalsIgnoreCase(
                                    chainedNames.get(FIELDS_INDEX))) {
                        final SObjectType objectType =
                                ApexValueBuilder.getWithoutSymbolProvider()
                                        .buildSObjectType(chainedNames.get(OBJECT_NAME_INDEX));
                        final ApexStringValue fieldName =
                                ApexValueBuilder.getWithoutSymbolProvider()
                                        .buildString(vertex.getName());

                        return Optional.of(
                                ApexValueBuilder.getWithoutSymbolProvider()
                                        .buildSObjectField(objectType, fieldName));
                    }
                }

                return Optional.empty();
            };

    private SObjectFieldFactory() {}
}
