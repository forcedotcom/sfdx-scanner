package com.salesforce.graph.symbols.apex;

import apex.jorje.semantic.symbol.type.TypeInfos;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.ApexStandardMapValue;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import com.salesforce.graph.vertex.Typeable;
import javax.annotation.Nullable;

/**
 * A specific map type returned from Schema#getGlobalDescribe. This class does not support #put and
 * returns synthetic values from #get See
 * https://developer.salesforce.com/docs/atlas.en-us.apexref.meta/apexref/apex_methods_system_schema.htm
 */
public final class ApexGlobalDescribeMapValue
        extends ApexStandardMapValue<ApexGlobalDescribeMapValue> implements Typeable {
    private static final Typeable TYPE =
            SyntheticTypedVertex.get(
                    ApexStandardLibraryUtil.getMapDeclaration(
                            ApexStandardLibraryUtil.Type.STRING,
                            ApexStandardLibraryUtil.Type.SCHEMA_S_OBJECT_TYPE));

    public ApexGlobalDescribeMapValue(ApexValueBuilder builder) {
        super(
                TypeInfos.STRING.getApexName(),
                ApexStandardLibraryUtil.Type.SCHEMA_S_OBJECT_TYPE,
                builder);
    }

    private ApexGlobalDescribeMapValue(ApexGlobalDescribeMapValue other) {
        this(other, other.getReturnedFrom().orElse(null), other.getInvocable().orElse(null));
    }

    private ApexGlobalDescribeMapValue(
            ApexGlobalDescribeMapValue other,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
    }

    @Override
    public ApexGlobalDescribeMapValue deepClone() {
        return new ApexGlobalDescribeMapValue(this);
    }

    @Override
    public ApexGlobalDescribeMapValue deepCloneForReturn(
            @Nullable ApexValue<?> returnedFrom, @Nullable InvocableVertex invocable) {
        return new ApexGlobalDescribeMapValue(this, returnedFrom, invocable);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String getCanonicalType() {
        return TYPE.getCanonicalType();
    }

    @Override
    protected ApexValue<?> getReturnValue(ApexValueBuilder builder, ApexValue<?> keyName) {
        return builder.buildSObjectType(keyName);
    }
}
