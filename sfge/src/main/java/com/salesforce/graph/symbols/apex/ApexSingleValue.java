package com.salesforce.graph.symbols.apex;

import com.salesforce.graph.DeepCloneableApexValue;
import com.salesforce.graph.ops.ObjectPropertiesUtil;
import com.salesforce.graph.ops.TypeableUtil;
import com.salesforce.graph.symbols.DeepCloneContextProvider;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Represents a value that is assigned to a variable. It can be directly assigned, or returned from
 * a method.
 */
// TODO: Refactor common code between ApexSingleValue and ApexSoqlValue into a common base class
public final class ApexSingleValue extends ApexPropertiesValue<ApexSingleValue>
        implements DeepCloneableApexValue<ApexSingleValue> {
    private static final String SCHEMA_GET_GLOBAL_DESCRIBE = "Schema.getGlobalDescribe";
    private static final String GET = "get";
    /** The type of the object, MyObject__c.SObjectType has a type of MyObject__c */
    private final Typeable typeable;

    /**
     * The object type that was instantiated. This may be a more specific value than that of
     * declarationVertex and valueVertex. This is typically set via a new object creation such as
     *
     * <p>SObject obj = new Account();
     */
    private ChainedVertex sObjectType;

    // TODO: This fails if this.typeable = builder#getDeclarationVertex
    ApexSingleValue(ApexValueBuilder builder) {
        this(null, builder);
    }

    ApexSingleValue(Typeable typeable, ApexValueBuilder builder) {
        super(builder);
        this.typeable = typeable;
        this.sObjectType = _getSObjectType();
        this.objectPropertiesHolder.setValues(this, valueVertex, builder.getSymbolProvider());
    }

    private ApexSingleValue(ApexSingleValue other) {
        this(other, other.getReturnedFrom().orElse(null), other.getInvocable().orElse(null));
    }

    private ApexSingleValue(
            ApexSingleValue other,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
        this.typeable = other.typeable;
        this.sObjectType = other.sObjectType;
    }

    @Override
    public ApexSingleValue deepClone() {
        return DeepCloneContextProvider.cloneIfAbsent(this, () -> new ApexSingleValue(this));
    }

    @Override
    public ApexSingleValue deepCloneForReturn(
            @Nullable ApexValue<?> returnedFrom, @Nullable InvocableVertex invocable) {
        return new ApexSingleValue(this, returnedFrom, invocable);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean isNull() {
        return super.isNull() && sObjectType == null && typeable == null;
    }

    @Override
    public Optional<Typeable> getTypeVertex() {
        Optional<Typeable> result = super.getTypeVertex();

        if (!result.isPresent()) {
            result = Optional.ofNullable(typeable);
        }

        return result;
    }

    @Override
    public Optional<String> getDefiningType() {
        if (sObjectType instanceof Typeable) {
            Typeable typeable = (Typeable) sObjectType;
            return Optional.of(typeable.getCanonicalType());
        } else if (getValueVertexType().isPresent()) {
            return getValueVertexType();
        } else if (getDeclaredType().isPresent()) {
            return getDeclaredType();
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ApexValue<?>> getOrAddDefault(String key) {
        // Check if we know about the key.
        final Optional<ApexValue<?>> valueOptional = getApexValue(key);

        if (!valueOptional.isPresent()) {
            if (isIndeterminant()) {
                // For example:
                // public void foo(Account acc) {
                // 	System.debug(acc.name);
                // }
                // Here, we don't know what acc was initialized with.
                // And we can't be sure if name was initialized or not. We assume it was
                // initialized but we assume it is indeterminant.
                return Optional.of(
                        ObjectPropertiesUtil.getDefaultIndeterminantValue(
                                key, objectPropertiesHolder));
            } else if (isNotNull() && isDataObject()) {
                // If this is a data object that was initialized but the key was not explicitly set,
                // for example:
                // Account acc = new Account();
                // System.debug(acc.name);
                // Here, we can see that account was initialized, but name field was not.
                // Return default null value since we know that the key was not set and is yet
                // looked up.
                return Optional.of(
                        ObjectPropertiesUtil.getDefaultNullValue(key, objectPropertiesHolder));
            }
        }

        return valueOptional;
    }

    public Optional<String> getSymbolicName() {
        if (valueVertex != null) {
            return valueVertex.getSymbolicName();
        } else {
            return Optional.empty();
        }
    }

    private boolean isDataObject() {
        final Optional<Typeable> typeVertex = getTypeVertex();
        if (typeVertex.isPresent()) {
            final String type = typeVertex.get().getCanonicalType();
            return TypeableUtil.isDataObject(type);
        }
        // Return false for everything else
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ApexSingleValue that = (ApexSingleValue) o;
        return Objects.equals(typeable, that.typeable)
                && Objects.equals(sObjectType, that.sObjectType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), typeable, sObjectType);
    }

    // TODO: This is hard coded, needs to handle more scenarios
    @Nullable
    protected ChainedVertex _getSObjectType() {
        if (!(valueVertex instanceof MethodCallExpressionVertex)) {
            return null;
        }

        MethodCallExpressionVertex methodVertex = (MethodCallExpressionVertex) valueVertex;
        List<MethodCallExpressionVertex> vertices =
                methodVertex.firstToList().stream()
                        .map(mv -> (MethodCallExpressionVertex) mv)
                        .collect(Collectors.toList());
        List<ChainedVertex> parameters;
        MethodCallExpressionVertex vertex;
        ChainedVertex parameter;

        if (vertices.size() < 2) {
            return null;
        }

        vertex = vertices.get(0);
        if (!SCHEMA_GET_GLOBAL_DESCRIBE.equalsIgnoreCase(vertex.getFullName())) {
            return null;
        }

        vertex = vertices.get(1);
        if (!GET.equalsIgnoreCase(vertex.getFullName())) {
            return null;
        }

        parameters = vertex.getParameters();
        if (parameters.size() != 1) {
            return null;
        }

        parameter = parameters.get(0);
        parameter = getValue(parameter).orElse(parameter);

        return parameter;
    }
}
