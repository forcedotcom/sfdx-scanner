package com.salesforce.graph.symbols.apex;

import apex.jorje.semantic.symbol.type.TypeInfos;
import com.salesforce.graph.DeepCloneable;

/**
 * https://developer.salesforce.com/docs/atlas.en-us.apexref.meta/apexref/apex_methods_system_long.htm
 */
public final class ApexLongValue extends ApexNumberValue<ApexLongValue, Long>
        implements DeepCloneable<ApexLongValue> {
    public static final String TYPE = TypeInfos.LONG.getApexName();

    ApexLongValue(Long value, ApexValueBuilder builder) {
        super(TYPE, value, builder);
    }

    private ApexLongValue(ApexLongValue other) {
        super(other);
    }

    @Override
    public ApexLongValue deepClone() {
        return new ApexLongValue(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }
}
