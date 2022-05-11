package com.salesforce.graph.symbols.apex;

import apex.jorje.semantic.symbol.type.TypeInfos;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.ApexStandardMapValue;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * A specific map type returned from Schema.SObjectType.Account.fields.getMap. This class does not
 * support #put and returns synthetic values from #get See
 * https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_dynamic_field_tokens.htm
 */
public final class ApexFieldDescribeMapValue extends ApexStandardMapValue<ApexFieldDescribeMapValue>
        implements Typeable {
    private static final Typeable TYPE =
            SyntheticTypedVertex.get(
                    ApexStandardLibraryUtil.getMapDeclaration(
                            ApexStandardLibraryUtil.Type.STRING,
                            ApexStandardLibraryUtil.Type.SCHEMA_S_OBJECT_FIELD));

    private final SObjectType sObjectType;

    public ApexFieldDescribeMapValue(SObjectType sObjectType, ApexValueBuilder builder) {
        super(
                TypeInfos.STRING.getApexName(),
                ApexStandardLibraryUtil.Type.SCHEMA_S_OBJECT_FIELD,
                builder);
        this.sObjectType = sObjectType;
    }

    private ApexFieldDescribeMapValue(ApexFieldDescribeMapValue other) {
        this(other, other.getReturnedFrom().orElse(null), other.getInvocable().orElse(null));
    }

    private ApexFieldDescribeMapValue(
            ApexFieldDescribeMapValue other,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
        this.sObjectType = other.sObjectType;
    }

    @Override
    public ApexFieldDescribeMapValue deepClone() {
        return new ApexFieldDescribeMapValue(this);
    }

    @Override
    public ApexFieldDescribeMapValue deepCloneForReturn(
            @Nullable ApexValue<?> returnedFrom, @Nullable InvocableVertex invocable) {
        return new ApexFieldDescribeMapValue(this, returnedFrom, invocable);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    public Optional<SObjectType> getAssociatedObjectType() {
        return Optional.of(sObjectType);
    }

    @Override
    public String getCanonicalType() {
        return TYPE.getCanonicalType();
    }

    @Override
    protected ApexValue<?> getReturnValue(ApexValueBuilder builder, ApexValue<?> keyName) {
        return builder.buildSObjectField(sObjectType, keyName);
    }
}
