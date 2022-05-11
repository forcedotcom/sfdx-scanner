package com.salesforce.graph.symbols.apex;

import com.salesforce.graph.symbols.MethodCallApexValueBuilder;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.VariableExpressionApexValueBuilder;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.ClassRefExpressionVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Generates {@link ApexStringValue}s from methods and variable expressions. */
public final class ApexStringValueFactory {
    public static final String JSON_SERIALIZE = "JSON.serialize";
    public static final String STRING_FORMAT = "String.format";
    public static final String STRING_JOIN = "String.join";
    public static final String STRING_VALUE_OF = "String.valueOf";
    public static final String UNRESOLVED_ARGUMENT_PREFIX = "SFGE_Unresolved_Argument_";

    public static final MethodCallApexValueBuilder METHOD_CALL_BUILDER_FUNCTION =
            (g, vertex, symbols) -> {
                final String methodName = vertex.getMethodName();
                final String fullMethodName = vertex.getFullMethodName();

                final ApexValueBuilder builder =
                        ApexValueBuilder.get(symbols).returnedFrom(null, vertex);

                // MyClass.class.getName();
                ClassRefExpressionVertex classRefExpression =
                        vertex.getClassRefExpression().orElse(null);
                if (classRefExpression != null) {
                    if (methodName.equalsIgnoreCase(SystemNames.METHOD_GET_NAME)) {
                        return Optional.of(
                                builder.valueVertex(vertex)
                                        .buildString(classRefExpression.getCanonicalType()));
                    }
                }

                // JSON.serialize
                if (JSON_SERIALIZE.equalsIgnoreCase(fullMethodName)) {
                    return jsonSerialize(builder, vertex);
                }

                // String.format
                if (STRING_FORMAT.equalsIgnoreCase(fullMethodName)) {
                    return stringFormat(builder, vertex, symbols);
                }

                // String.join
                if (STRING_JOIN.equalsIgnoreCase(fullMethodName)) {
                    return stringJoin(builder, vertex, symbols);
                }

                // String.valueOf
                if (STRING_VALUE_OF.equalsIgnoreCase(fullMethodName)) {
                    return stringValueOf(builder, vertex, symbols);
                }

                return Optional.empty();
            };

    public static final VariableExpressionApexValueBuilder VARIABLE_EXPRESSION_BUILDER_FUNCTION =
            vertex -> {
                final List<String> chainedNames = vertex.getChainedNames();

                // obj.TheField__r.QualifiedApiName
                if (chainedNames.size() >= 2) {
                    final int fieldNameNameIndex = chainedNames.size() - 1;

                    final String qualifiedApiName = vertex.getName();
                    final String fieldName = chainedNames.get(fieldNameNameIndex);

                    if (SystemNames.VARIABLE_QUALIFIED_API_NAME.equalsIgnoreCase(
                            qualifiedApiName)) {
                        if (fieldName
                                .toLowerCase(Locale.ROOT)
                                .endsWith(SystemNames.SUFFIX_RELATION)) {
                            return Optional.of(
                                    ApexValueBuilder.getWithoutSymbolProvider()
                                            .valueVertex(vertex)
                                            .buildString(fieldName));
                        }
                    }
                }

                return Optional.empty();
            };

    private static Optional<ApexValue<?>> jsonSerialize(
            ApexValueBuilder builder, MethodCallExpressionVertex vertex) {
        return Optional.of(builder.valueVertex(vertex).buildString());
    }

    private static Optional<ApexValue<?>> stringFormat(
            ApexValueBuilder builder, MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        final List<ChainedVertex> parameters = vertex.getParameters();
        ApexValue.validateParameterSize(vertex, 2);

        final ApexValue<?> formatString =
                ScopeUtil.resolveToApexValue(symbols, parameters.get(0)).orElse(null);
        if (formatString instanceof ApexStringValue
                && ((ApexStringValue) formatString).getValue().isPresent()) {
            final List<Object> formatList = new ArrayList<>();
            final ApexValue<?> formattingArguments =
                    ScopeUtil.resolveToApexValue(symbols, parameters.get(1)).orElse(null);
            if (formattingArguments instanceof ApexListValue) {
                final ApexListValue apexListValue = (ApexListValue) formattingArguments;
                for (int i = 0; i < apexListValue.getValues().size(); i++) {
                    final ApexValue<?> apexValue = apexListValue.get(i);
                    if (apexValue instanceof ApexSimpleValue && apexValue.isDeterminant()) {
                        final ApexSimpleValue apexSimpleValue = (ApexSimpleValue) apexValue;
                        final Object value = apexSimpleValue.getValue().orElse(null);
                        formatList.add(value);
                    } else {
                        // TODO: It would be more precise to return an indeterminant string here,
                        // however
                        // this currently leads to FLS rules declaring a path safe when it should
                        // not.
                        // Consider the following changes
                        // 1. change FLS rules to not declare an indeterminant string as safe
                        // 2. Add a new property to indeterminant strings which is something like
                        // "best-guess" that may contain a mix of determinant and indeterminant
                        // portions of
                        // the string.
                        formatList.add(UNRESOLVED_ARGUMENT_PREFIX + i);
                    }
                }
            }
            final String format = ((ApexStringValue) formatString).getValue().get();
            return Optional.of(
                    builder.valueVertex(vertex)
                            .buildString(MessageFormat.format(format, formatList.toArray())));
        }

        return Optional.empty();
    }

    private static Optional<ApexValue<?>> stringJoin(
            ApexValueBuilder builder, MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        final List<ChainedVertex> parameters = vertex.getParameters();
        ApexValue.validateParameterSize(vertex, 2);
        final ApexValue<?> iterable =
                ScopeUtil.resolveToApexValue(symbols, parameters.get(0)).orElse(null);
        final ApexValue<?> separatorString =
                ScopeUtil.resolveToApexValue(symbols, parameters.get(1)).orElse(null);

        if (iterable instanceof ApexListValue
                && iterable.isDeterminant()
                && separatorString instanceof ApexStringValue
                && separatorString.isDeterminant()) {
            final ApexListValue apexListValue = (ApexListValue) iterable;
            final List<String> joinList = new ArrayList<>();
            for (int i = 0; i < apexListValue.getValues().size(); i++) {
                final ApexValue<?> apexValue = apexListValue.get(i);
                if (apexValue instanceof ApexSimpleValue && apexValue.isDeterminant()) {
                    final ApexSimpleValue apexSimpleValue = (ApexSimpleValue) apexValue;
                    final Object value = apexSimpleValue.getValue().orElse(null);
                    joinList.add(String.valueOf(value));
                } else {
                    // TODO: It would be more precise to return an indeterminant string here
                    joinList.add(UNRESOLVED_ARGUMENT_PREFIX + i);
                }
            }
            final String separator = ((ApexStringValue) separatorString).getValue().get();
            return Optional.of(
                    builder.valueVertex(vertex).buildString(String.join(separator, joinList)));
        }

        return Optional.empty();
    }

    private static Optional<ApexValue<?>> stringValueOf(
            ApexValueBuilder builder, MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        final List<ChainedVertex> parameters = vertex.getParameters();
        ApexValue.validateParameterSize(vertex, 1);
        final ApexValue<?> apexValue =
                ScopeUtil.resolveToApexValue(symbols, parameters.get(0)).orElse(null);

        if (apexValue instanceof ApexSimpleValue<?, ?> && apexValue.isDeterminant()) {
            final ApexSimpleValue<?, ?> apexSimpleValue = (ApexSimpleValue<?, ?>) apexValue;
            return Optional.of(
                    builder.valueVertex(vertex)
                            .buildString(apexSimpleValue.getValue().get().toString()));
        } else {
            return Optional.of(
                    builder.valueVertex(vertex)
                            .withStatus(ValueStatus.INDETERMINANT)
                            .buildString());
        }
    }

    private ApexStringValueFactory() {}
}
