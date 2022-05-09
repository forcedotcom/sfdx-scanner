package com.salesforce.graph.symbols.apex;

import apex.jorje.semantic.symbol.type.TypeInfos;
import com.salesforce.graph.DeepCloneable;
import java.util.Optional;

public final class ApexBooleanValue extends ApexSimpleValue<ApexBooleanValue, Boolean>
        implements DeepCloneable<ApexBooleanValue> {
    public static final String TYPE = TypeInfos.BOOLEAN.getApexName();

    ApexBooleanValue(Boolean value, ApexValueBuilder builder) {
        super(TYPE, value, builder);
        setValueVertex(builder.getValueVertex(), builder.getSymbolProvider());
    }

    private ApexBooleanValue(ApexBooleanValue other) {
        super(other);
    }

    @Override
    public ApexBooleanValue deepClone() {
        return new ApexBooleanValue(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Optional<Boolean> asTruthyBoolean() {
        return getValue();
    }
}
