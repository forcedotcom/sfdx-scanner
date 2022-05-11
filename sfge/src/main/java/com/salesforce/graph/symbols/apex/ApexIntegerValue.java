package com.salesforce.graph.symbols.apex;

import apex.jorje.semantic.symbol.type.TypeInfos;
import com.salesforce.graph.DeepCloneable;

/**
 * https://developer.salesforce.com/docs/atlas.en-us.apexref.meta/apexref/apex_methods_system_integer.htm
 */
public final class ApexIntegerValue extends ApexNumberValue<ApexIntegerValue, Integer>
        implements DeepCloneable<ApexIntegerValue> {
    public static final String TYPE = TypeInfos.INTEGER.getApexName();

    ApexIntegerValue(Integer value, ApexValueBuilder builder) {
        super(TYPE, value, builder);
    }

    private ApexIntegerValue(ApexIntegerValue other) {
        super(other);
    }

    @Override
    public ApexIntegerValue deepClone() {
        return new ApexIntegerValue(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }
}
