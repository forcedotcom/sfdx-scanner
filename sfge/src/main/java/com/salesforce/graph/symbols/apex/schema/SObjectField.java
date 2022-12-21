package com.salesforce.graph.symbols.apex.schema;

import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.DeepCloneableApexValue;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexForLoopValue;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexStandardValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.ApexValueVisitor;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Represents Schema.SObjectField See
 * https://developer.salesforce.com/docs/atlas.en-us.apexref.meta/apexref/apex_class_Schema_SObjectField.htm#apex_class_Schema_SObjectField
 */
public final class SObjectField extends ApexStandardValue<SObjectField>
        implements DeepCloneable<SObjectField>, DeepCloneableApexValue<SObjectField> {
    public static final String TYPE = ApexStandardLibraryUtil.Type.SCHEMA_S_OBJECT_FIELD;
    public static final String METHOD_GET_DESCRIBE = "getDescribe";

    private final SObjectType associatedObjectType;
    private final ApexValue<?> fieldName;

    /** Do not call directly. Use {@link ApexValueBuilder} */
    @SuppressWarnings("PMD.NullAssignment") // Final variables are set non-null values in the other
    // constructor
    public SObjectField(ApexValueBuilder builder) {
        super(TYPE, builder);
        this.associatedObjectType = null;
        this.fieldName = null;
    }

    /** Do not call directly. Use {@link ApexValueBuilder} */
    public SObjectField(
            SObjectType associatedObjectType, ApexStringValue fieldName, ApexValueBuilder builder) {
        super(TYPE, builder);
        this.associatedObjectType = associatedObjectType;
        this.fieldName = fieldName;
        if (associatedObjectType == null && fieldName != null
                || associatedObjectType != null && fieldName == null) {
            throw new UnexpectedException("Object and fields should be consistently defined");
        }
    }

    /** Do not call directly. Use {@link ApexValueBuilder} */
    public SObjectField(
            SObjectType associatedObjectType,
            ApexForLoopValue fieldName,
            ApexValueBuilder builder) {
        super(TYPE, builder);
        this.associatedObjectType = associatedObjectType;
        this.fieldName = fieldName;
        if (associatedObjectType == null && fieldName != null
                || associatedObjectType != null && fieldName == null) {
            throw new UnexpectedException("Object and fields should be consistently defined");
        }
    }

    /** Do not call directly. Use {@link ApexValueBuilder} */
    public SObjectField(
            SObjectType associatedObjectType, ApexSingleValue fieldName, ApexValueBuilder builder) {
        super(TYPE, builder);
        this.associatedObjectType = associatedObjectType;
        this.fieldName = fieldName;
        if (associatedObjectType == null && fieldName != null
                || associatedObjectType != null && fieldName == null) {
            throw new UnexpectedException("Object and fields should be consistently defined");
        }
    }

    private SObjectField(SObjectField other) {
        this(other, other.getReturnedFrom().orElse(null), other.getInvocable().orElse(null));
    }

    protected SObjectField(
            SObjectField other,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
        this.associatedObjectType = other.associatedObjectType;
        this.fieldName = other.fieldName;
    }

    @Override
    public SObjectField deepClone() {
        return new SObjectField(this);
    }

    @Override
    public SObjectField deepCloneForReturn(
            @Nullable ApexValue<?> returnedFrom, @Nullable InvocableVertex invocable) {
        return new SObjectField(this, returnedFrom, invocable);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    public Optional<SObjectType> getAssociatedObjectType() {
        return Optional.ofNullable(associatedObjectType);
    }

    public Optional<ApexValue<?>> getFieldname() {
        return Optional.ofNullable(fieldName);
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        ApexValueBuilder builder =
            ApexValueBuilder.get(symbols)
                .returnedFrom(this, vertex);
        String methodName = vertex.getMethodName();
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

        return _applyMethod(invocableExpression, builder, methodName);
    }

    private Optional<ApexValue<?>> _applyMethod(InvocableWithParametersVertex invocableExpression, ApexValueBuilder builder, String methodName) {
        if (METHOD_GET_DESCRIBE.equalsIgnoreCase(methodName)) {
            if (associatedObjectType != null && fieldName != null) {
                DescribeSObjectResult describeSObjectResult =
                        builder.deepClone().buildDescribeSObjectResult(associatedObjectType);
                return Optional.of(
                    builder.buildDescribeFieldResult(describeSObjectResult, fieldName));
            } else {
                return Optional.empty();
            }
        } else {
            throw new TodoException(invocableExpression);
        }
    }
}
