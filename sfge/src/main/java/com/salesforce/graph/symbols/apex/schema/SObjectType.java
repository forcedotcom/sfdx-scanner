package com.salesforce.graph.symbols.apex.schema;

import com.salesforce.exception.TodoException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.DeepCloneableApexValue;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexStandardValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.ApexValueVisitor;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents Schema.SObjectType See
 * https://developer.salesforce.com/docs/atlas.en-us.apexref.meta/apexref/apex_class_Schema_SObjectType.htm#apex_class_Schema_SObjectType
 */
public final class SObjectType extends ApexStandardValue<SObjectType>
        implements DeepCloneable<SObjectType>, DeepCloneableApexValue<SObjectType> {
    public static final String TYPE = ApexStandardLibraryUtil.Type.SCHEMA_S_OBJECT_TYPE;

    public static final String METHOD_GET_DESCRIBE = "getDescribe";
    public static final String METHOD_NEW_S_OBJECT = "newSObject";

    /** The type of the object, MyObject__c.SObjectType has a type of MyObject__c */
    private final ApexValue<?> type;

    public SObjectType(ApexValueBuilder builder) {
        super(ApexStandardLibraryUtil.Type.SCHEMA_S_OBJECT_TYPE, builder);
        this.type = null;
    }
    /** Do not call directly. Use {@link ApexValueBuilder} */
    public SObjectType(String type, ApexValueBuilder builder) {
        this(ApexValueBuilder.getWithoutSymbolProvider().buildString(type), builder);
    }

    /** Do not call directly. Use {@link ApexValueBuilder} */
    public SObjectType(ApexValue<?> type, ApexValueBuilder builder) {
        super(ApexStandardLibraryUtil.Type.SCHEMA_S_OBJECT_TYPE, builder);
        this.type = type;
    }

    private SObjectType(SObjectType other) {
        this(other, other.getReturnedFrom().orElse(null), other.getInvocable().orElse(null));
    }

    private SObjectType(
            SObjectType other,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
        this.type = other.type;
    }

    @Override
    public SObjectType deepCloneForReturn(
            @Nullable ApexValue<?> returnedFrom, @Nullable InvocableVertex invocable) {
        return new SObjectType(this, returnedFrom, invocable);
    }

    @Override
    public SObjectType deepClone() {
        return new SObjectType(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    public Optional<Typeable> getTypeVertex() {
        Optional<Typeable> result = super.getTypeVertex();
        if (!result.isPresent() && type instanceof ApexStringValue) {
            String type = getStringForComparison();
            if (type != null) {
                result = Optional.of(SyntheticTypedVertex.get(type));
            }
        }
        return result;
    }

    public static boolean matches(VariableExpressionVertex vertex) {
        return vertex.getName()
                .equalsIgnoreCase(ApexStandardLibraryUtil.VariableNames.S_OBJECT_TYPE);
    }

    public Optional<ApexValue<?>> getType() {
        return Optional.ofNullable(type);
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        final String methodName = vertex.getMethodName();
        ApexValueBuilder builder = ApexValueBuilder.get(symbols)
            .returnedFrom(this, vertex);

        return _applyMethod(vertex, builder, methodName);
    }

    @Override
    public Optional<ApexValue<?>> executeMethod(
            InvocableWithParametersVertex invocableExpression,
            @Nullable MethodVertex method,
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
            return Optional.of(builder.buildDescribeSObjectResult(this));
        } else if (METHOD_NEW_S_OBJECT.equalsIgnoreCase(methodName)) {
            validateParameterSize(invocableExpression, 0);
            return Optional.of(
                builder.valueVertex(invocableExpression)
                    .buildSObjectInstance(
                        SyntheticTypedVertex.get(
                            ApexStandardLibraryUtil.Type.S_OBJECT)));
        } else {
            throw new TodoException(invocableExpression);
        }
    }

    private String getStringForComparison() {
        return StringUtils.toRootLowerCase(((ApexStringValue) type).getValue().orElse(null));
    }

    /** IMPORTANT: Equality and hashcode are case-insensitive. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SObjectType that = (SObjectType) o;
        if (type instanceof ApexStringValue && that.type instanceof ApexStringValue) {
            return Objects.equals(getStringForComparison(), that.getStringForComparison());
        } else {
            return Objects.equals(type, that.type);
        }
    }

    @Override
    public int hashCode() {
        if (type instanceof ApexStringValue) {
            return Objects.hash(getStringForComparison());
        } else {
            return Objects.hash(type);
        }
    }

    @Override
    public String toString() {
        return "SObjectType{" + "associatedObjectType='" + type + '\'' + "} " + super.toString();
    }
}
