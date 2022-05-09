package com.salesforce.graph.symbols.apex;

import com.salesforce.graph.symbols.MethodCallApexValueBuilder;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/** Supports #valueOf for ApexSimpleValue derived classes */
public final class ApexSimpleValueFactory {
    private static final String VALUE_OF = "valueOf";
    public static final MethodCallApexValueBuilder METHOD_CALL_BUILDER_FUNCTION =
            (g, vertex, symbols) -> {
                final String methodName = vertex.getMethodName();

                if (VALUE_OF.equals(methodName)) {
                    return valueOf(vertex, symbols);
                }

                return Optional.empty();
            };
    private static final String BOOLEAN_VALUE_OF = "Boolean.valueOf";
    private static final String DECIMAL_VALUE_OF = "Decimal.valueOf";
    private static final String DOUBLE_VALUE_OF = "Double.valueOf";
    private static final String INTEGER_VALUE_OF = "Integer.valueOf";
    private static final String LONG_VALUE_OF = "Long.valueOf";

    /** Generic support for #valueOf for simple values */
    private static Optional<ApexValue<?>> valueOf(
            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        final String fullMethodName = vertex.getFullMethodName();
        final ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(null, vertex);

        final List<ChainedVertex> parameters = vertex.getParameters();

        // Supplier that generates an indeterminant value of the correct type
        final Supplier<ApexValue<?>> indeterminantSupplier;
        // Function that generates a determinant value using the parameter passed to #valueOf
        final Function<Object, ApexValue<?>> determinantFunction;
        // Function that converts the parameter passed to #valueOf, to the Java type, this is then
        // passed to the determinantFunction
        final Function<String, Object> valueOfFunction;

        if (BOOLEAN_VALUE_OF.equalsIgnoreCase(fullMethodName)) {
            indeterminantSupplier = builder::buildBoolean;
            determinantFunction = o -> builder.buildBoolean((Boolean) o);
            valueOfFunction = Boolean::valueOf;
        } else if (DECIMAL_VALUE_OF.equalsIgnoreCase(fullMethodName)) {
            indeterminantSupplier = builder::buildDecimal;
            determinantFunction = o -> builder.buildDecimal((BigDecimal) o);
            valueOfFunction = s -> new BigDecimal(s);
        } else if (DOUBLE_VALUE_OF.equalsIgnoreCase(fullMethodName)) {
            indeterminantSupplier = builder::buildDouble;
            determinantFunction = o -> builder.buildDouble((Double) o);
            valueOfFunction = Double::valueOf;
        } else if (INTEGER_VALUE_OF.equalsIgnoreCase(fullMethodName)) {
            indeterminantSupplier = builder::buildInteger;
            determinantFunction = o -> builder.buildInteger((Integer) o);
            valueOfFunction = Integer::valueOf;
        } else if (LONG_VALUE_OF.equalsIgnoreCase(fullMethodName)) {
            indeterminantSupplier = builder::buildLong;
            determinantFunction = o -> builder.buildLong((Long) o);
            valueOfFunction = Long::valueOf;
        } else {
            return Optional.empty();
        }

        ApexValue.validateParameterSize(vertex, 1);
        final ApexValue<?> apexValue =
                ScopeUtil.resolveToApexValue(symbols, parameters.get(0)).orElse(null);
        if (apexValue instanceof ApexSimpleValue<?, ?> && apexValue.isDeterminant()) {
            // This will throw an exception that excludes the path if a null value is passed to
            // #valueOf. The Apex
            // runtime would throw an NPE. Th
            apexValue.checkForUseAsNullParameter(vertex);

            // This if statement is checking for #isValuePresent because of class instance values. A
            // controller might
            // have a public property where the aura/vf page sets this value, but the sfge engine is
            // currently unaware
            // that this value has been set. Normally we might throw a NullValueAccessedException,
            // since we aren't 100%
            // sure, we return an indeterminant instead of throwing an exception
            if (apexValue.isValuePresent()) {
                // Convert the value to a string, and then to the correct type
                final ApexSimpleValue<?, ?> apexSimpleValue = (ApexSimpleValue<?, ?>) apexValue;
                final Object valueOf =
                        valueOfFunction.apply(apexSimpleValue.getValue().get().toString());
                builder.valueVertex(vertex);
                return Optional.of(determinantFunction.apply(valueOf));
            }
        }

        // Build an indeterminant type
        builder.withStatus(ValueStatus.INDETERMINANT);
        return Optional.of(indeterminantSupplier.get());
    }

    private ApexSimpleValueFactory() {}
}
