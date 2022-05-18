package com.salesforce.graph.symbols.apex;

import apex.jorje.semantic.symbol.type.TypeInfos;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * TODO: More methods
 * https://developer.salesforce.com/docs/atlas.en-us.apexref.meta/apexref/apex_methods_system_decimal.htm
 */
public final class ApexDecimalValue extends ApexNumberValue<ApexDecimalValue, BigDecimal>
        implements DeepCloneable<ApexDecimalValue> {
    public static final String TYPE = TypeInfos.DECIMAL.getApexName();
    private static final String METHOD_LONG_VALUE = "longValue";

    ApexDecimalValue(BigDecimal value, ApexValueBuilder builder) {
        super(TYPE, value, builder);
    }

    private ApexDecimalValue(ApexDecimalValue other) {
        super(other);
    }

    @Override
    public ApexDecimalValue deepClone() {
        return new ApexDecimalValue(this);
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
