package com.salesforce.graph.symbols.apex;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import java.text.NumberFormat;
import java.util.Optional;

/** Special case for number values that have common methods. */
abstract class ApexNumberValue<T extends ApexNumberValue<?, ?>, V extends Number>
        extends ApexSimpleValue<T, V> {
    private static final String METHOD_FORMAT = "format";
    private static final String METHOD_INT_VALUE = "intValue";

    ApexNumberValue(String apexType, V value, ApexValueBuilder builder) {
        super(apexType, value, builder);
    }

    protected ApexNumberValue(ApexSimpleValue<T, V> other) {
        super(other);
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        final ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(this, vertex);
        final String methodName = vertex.getMethodName();

        if (METHOD_FORMAT.equalsIgnoreCase(methodName)) {
            if (isValueNotPresent() || isIndeterminant()) {
                return Optional.of(builder.withStatus(ValueStatus.INDETERMINANT).buildString());
            } else {
                final Number number = getValue().get();
                final NumberFormat numberFormat = NumberFormat.getNumberInstance();
                return Optional.of(builder.buildString(numberFormat.format(number)));
            }
        } else if (METHOD_INT_VALUE.equalsIgnoreCase(methodName)) {
            if (isValueNotPresent() || isIndeterminant()) {
                return Optional.of(builder.withStatus(ValueStatus.INDETERMINANT).buildInteger());
            } else {
                return Optional.of(builder.buildInteger(getValue().get().intValue()));
            }
        } else {
            return super.apply(vertex, symbols);
        }
    }
}
