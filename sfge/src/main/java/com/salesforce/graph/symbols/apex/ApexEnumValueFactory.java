package com.salesforce.graph.symbols.apex;

import com.salesforce.apex.ApexEnum;
import com.salesforce.graph.MetadataInfoProvider;
import com.salesforce.graph.symbols.MethodCallApexValueBuilder;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.VariableExpressionApexValueBuilder;
import java.util.List;
import java.util.Optional;

public final class ApexEnumValueFactory {
    private static final String VALUES = "values";
    private static final String VALUE_OF = "valueOf";

    public static final VariableExpressionApexValueBuilder VARIABLE_EXPRESSION_BUILDER_FUNCTION =
            vertex -> {
                List<String> chainedNames = vertex.getChainedNames();

                // Examples
                // Schema.DisplayType.ADDRESS
                // DisplayType.ADDRESS
                if (chainedNames.size() != 1 && chainedNames.size() != 2) {
                    return Optional.empty();
                }

                // Schema.DisplayType, DisplayType
                final String enumName = String.join(".", chainedNames);
                ApexEnum apexEnum = MetadataInfoProvider.get().getEnum(enumName).orElse(null);
                if (apexEnum != null) {
                    // ADDRESS
                    final String enumValue = vertex.getName();
                    return Optional.of(
                            ApexValueBuilder.getWithoutSymbolProvider()
                                    .buildEnum(apexEnum, enumValue));
                }

                return Optional.empty();
            };

    public static final MethodCallApexValueBuilder METHOD_CALL_BUILDER_FUNCTION =
            (g, vertex, symbols) -> {
                final String methodName = vertex.getMethodName();
                final List<String> chainedNames = vertex.getChainedNames();

                if (VALUES.equalsIgnoreCase(methodName) || VALUE_OF.equalsIgnoreCase(methodName)) {
                    if (chainedNames.size() == 1) {
                        ApexValueBuilder builder =
                                ApexValueBuilder.get(symbols).returnedFrom(null, vertex);
                        final String enumName = chainedNames.get(0);
                        final ApexEnum apexEnum =
                                MetadataInfoProvider.get().getEnum(enumName).orElse(null);
                        if (apexEnum != null) {
                            if (VALUES.equalsIgnoreCase(methodName)) {
                                ApexValue.validateParameterSize(vertex, 0);
                                final ApexListValue listValue = builder.deepClone().buildList();
                                for (ApexEnum.Value value : apexEnum.getValues()) {
                                    listValue.add(
                                            builder.deepClone()
                                                    .buildEnum(apexEnum, value.getValueName()));
                                }
                                return Optional.of(listValue);
                            }

                            if (VALUE_OF.equalsIgnoreCase(methodName)) {
                                ApexValue.validateParameterSize(vertex, 1);
                                final ApexValue<?> value =
                                        ScopeUtil.resolveToApexValue(
                                                        symbols, vertex.getParameters().get(0))
                                                .orElse(null);
                                if (value instanceof ApexStringValue && value.isValuePresent()) {
                                    final String valueName =
                                            ((ApexStringValue) value).getValue().get();
                                    return Optional.of(builder.buildEnum(apexEnum, valueName));
                                } else {
                                    return Optional.of(
                                            builder.withStatus(ValueStatus.INDETERMINANT)
                                                    .buildEnum(apexEnum));
                                }
                            }
                        }
                    }
                }

                return Optional.empty();
            };

    private ApexEnumValueFactory() {}
}
