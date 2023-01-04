package com.salesforce.graph.symbols.apex.schema;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexForLoopValue;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexStandardValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.ApexValueVisitor;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import java.util.Objects;
import java.util.Optional;

public final class FieldSet extends ApexStandardValue<FieldSet> implements DeepCloneable<FieldSet> {
    public static final String TYPE = ApexStandardLibraryUtil.Type.SCHEMA_FIELD_SET;

    private static final String METHOD_GET_FIELDS = "getFields";
    private static final String METHOD_GET_S_OBJECT_TYPE = "getSObjectType";

    private final SObjectType sObjectType;
    private final ApexValue<?> fieldSetName;

    /** Do not call directly. Use {@link ApexValueBuilder} */
    @SuppressWarnings("PMD.NullAssignment") // Final variables are set non-null values in the other
    // constructor
    public FieldSet(ApexValueBuilder builder) {
        super(TYPE, builder);
        this.sObjectType = null;
        this.fieldSetName = null;
    }

    /** Do not call directly. Use {@link ApexValueBuilder} */
    public FieldSet(
            SObjectType sObjectType, ApexStringValue fieldSetName, ApexValueBuilder builder) {
        super(TYPE, builder);
        this.sObjectType = sObjectType;
        this.fieldSetName = fieldSetName;
        if (sObjectType == null && fieldSetName != null
                || sObjectType != null && fieldSetName == null) {
            throw new UnexpectedException("Object and fields should be consistently defined");
        }
    }

    /** Do not call directly. Use {@link ApexValueBuilder} */
    public FieldSet(
            SObjectType sObjectType, ApexForLoopValue fieldSetName, ApexValueBuilder builder) {
        super(TYPE, builder);
        this.sObjectType = sObjectType;
        this.fieldSetName = fieldSetName;
        if (sObjectType == null && fieldSetName != null
                || sObjectType != null && fieldSetName == null) {
            throw new UnexpectedException("Object and fields should be consistently defined");
        }
    }

    /** Do not call directly. Use {@link ApexValueBuilder} */
    public FieldSet(
            SObjectType sObjectType, ApexSingleValue fieldSetName, ApexValueBuilder builder) {
        super(TYPE, builder);
        this.sObjectType = sObjectType;
        this.fieldSetName = fieldSetName;
        if (sObjectType == null && fieldSetName != null
                || sObjectType != null && fieldSetName == null) {
            throw new UnexpectedException("Object and fields should be consistently defined");
        }
    }

    private FieldSet(FieldSet other) {
        super(other);
        this.sObjectType = other.sObjectType;
        this.fieldSetName = other.fieldSetName;
    }

    @Override
    public FieldSet deepClone() {
        return new FieldSet(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    public Optional<SObjectType> getSObjectType() {
        return Optional.ofNullable(sObjectType);
    }

    public Optional<ApexValue<?>> getFieldSetName() {
        return Optional.ofNullable(fieldSetName);
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(this, vertex);
        final String methodName = vertex.getMethodName();

        return _applyMethod(vertex, builder, methodName);
    }

    @Override
    public Optional<ApexValue<?>> executeMethod(
            InvocableWithParametersVertex invocableExpression,
            MethodVertex method,
            SymbolProvider symbols) {
        ApexValueBuilder builder =
                ApexValueBuilder.get(symbols)
                        .returnedFrom(this, invocableExpression)
                        .methodVertex(method);

        String methodName = method.getName();

        ApexValue<?> apexValue =
                _applyMethod(invocableExpression, builder, methodName).orElse(null);

        if (apexValue == null) {
            apexValue = ApexValueUtil.synthesizeReturnedValue(builder, method);
        }
        return Optional.ofNullable(apexValue);
    }

    private Optional<ApexValue<?>> _applyMethod(
            InvocableWithParametersVertex invocableExpression,
            ApexValueBuilder builder,
            String methodName) {
        if (METHOD_GET_FIELDS.equalsIgnoreCase(methodName)) {
            builder.declarationVertex(SyntheticTypedVertex.get(FieldSetMember.TYPE));
            return Optional.of(builder.buildFieldSetList(this));
        } else if (METHOD_GET_S_OBJECT_TYPE.equalsIgnoreCase(methodName)) {
            if (sObjectType != null) {
                return Optional.of(sObjectType.deepCloneForReturn(this, invocableExpression));
            } else {
                return Optional.of(builder.buildSObjectType());
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FieldSet fieldSet = (FieldSet) o;
        return Objects.equals(sObjectType, fieldSet.sObjectType)
                && Objects.equals(fieldSetName, fieldSet.fieldSetName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sObjectType, fieldSetName);
    }
}
