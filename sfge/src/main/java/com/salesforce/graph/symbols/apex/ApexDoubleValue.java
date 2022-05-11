package com.salesforce.graph.symbols.apex;

import apex.jorje.semantic.symbol.type.TypeInfos;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import java.util.Optional;

/**
 * TODO: More methods
 * https://developer.salesforce.com/docs/atlas.en-us.apexref.meta/apexref/apex_methods_system_double.htm
 */
public final class ApexDoubleValue extends ApexNumberValue<ApexDoubleValue, Double>
        implements DeepCloneable<ApexDoubleValue> {
    public static final String TYPE = TypeInfos.DOUBLE.getApexName();
    private static final String METHOD_LONG_VALUE = "longValue";

    ApexDoubleValue(Double value, ApexValueBuilder builder) {
        super(TYPE, value, builder);
    }

    private ApexDoubleValue(ApexDoubleValue other) {
        super(other);
    }

    @Override
    public ApexDoubleValue deepClone() {
        return new ApexDoubleValue(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(this, vertex);
        String methodName = vertex.getMethodName();

        if (METHOD_LONG_VALUE.equalsIgnoreCase(methodName)) {
            if (isValueNotPresent() || isIndeterminant()) {
                return Optional.of(builder.withStatus(ValueStatus.INDETERMINANT).buildLong());
            } else {
                return Optional.of(builder.buildLong(getValue().get().longValue()));
            }
        } else {
            return super.apply(vertex, symbols);
        }
    }
}
