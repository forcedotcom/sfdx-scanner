package com.salesforce.graph.symbols.apex;

import apex.jorje.semantic.symbol.type.TypeInfos;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.ApexStandardMapValue;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** {@code Map<String, Schema.FieldSet> } */
public final class ApexFieldSetDescribeMapValue
        extends ApexStandardMapValue<ApexFieldSetDescribeMapValue> implements Typeable {
    private static final Logger LOGGER = LogManager.getLogger(ApexFieldSetDescribeMapValue.class);

    private static final Typeable TYPE =
            SyntheticTypedVertex.get(
                    ApexStandardLibraryUtil.getMapDeclaration(
                            ApexStandardLibraryUtil.Type.STRING,
                            ApexStandardLibraryUtil.Type.SCHEMA_FIELD_SET));
    protected static final String METHOD_GET_FIELDS = "getFields";

    private final SObjectType sObjectType;

    public ApexFieldSetDescribeMapValue(SObjectType sObjectType, ApexValueBuilder builder) {
        super(
                TypeInfos.STRING.getApexName(),
                ApexStandardLibraryUtil.Type.SCHEMA_FIELD_SET,
                builder);
        this.sObjectType = sObjectType;
    }

    private ApexFieldSetDescribeMapValue(ApexFieldSetDescribeMapValue other) {
        this(other, other.getReturnedFrom().orElse(null), other.getInvocable().orElse(null));
    }

    private ApexFieldSetDescribeMapValue(
            ApexFieldSetDescribeMapValue other,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
        this.sObjectType = other.sObjectType;
    }

    @Override
    public ApexFieldSetDescribeMapValue deepClone() {
        return new ApexFieldSetDescribeMapValue(this);
    }

    @Override
    public ApexFieldSetDescribeMapValue deepCloneForReturn(
            @Nullable ApexValue<?> returnedFrom, @Nullable InvocableVertex invocable) {
        return new ApexFieldSetDescribeMapValue(this, returnedFrom, invocable);
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
        return builder.buildFieldSet(sObjectType, keyName);
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(this, vertex);
        String methodName = vertex.getMethodName();

        if (METHOD_GET_FIELDS.equalsIgnoreCase(methodName)) {
            // TODO: I think this is only needed on FieldSet
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(methodName + " was called");
            }
            validateParameterSize(vertex, 0);
            return Optional.of(
                    builder.withStatus(ValueStatus.INDETERMINANT)
                            // TODO: Is this correct?
                            .declarationVertex(
                                    SyntheticTypedVertex.get(
                                            ApexStandardLibraryUtil.getListDeclaration(
                                                    ApexStandardLibraryUtil.Type
                                                            .SCHEMA_FIELD_SET_MEMBER)))
                            .buildList());
        } else {
            return super.apply(vertex, symbols);
        }
    }
}
