package com.salesforce.graph.symbols.apex;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.symbols.ObjectProperties;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

/** Abstract class for ApexValues that support the "put" method. */
public abstract class ApexPropertiesValue<T extends ApexPropertiesValue<?>>
        extends AbstractSanitizableValue<T> implements ComplexAssignable, ObjectProperties {
    private static final String METHOD_PUT = "put";

    /**
     * The set of values assigned to the ApexValue. For instance the following example would have an
     * ObjectPropertiesHolder object with two properties(Name, Phone)
     *
     * <p>Account a = new Account(Name = 'Acme Inc.'); a.Phone = '415-555-1212';
     */
    protected final ObjectPropertiesHolder objectPropertiesHolder;

    ApexPropertiesValue(ApexValueBuilder builder) {
        super(builder);
        this.objectPropertiesHolder = new ObjectPropertiesHolder();
    }

    ApexPropertiesValue(
            ApexPropertiesValue other,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
        this.objectPropertiesHolder = CloneUtil.clone(other.objectPropertiesHolder);
    }

    @Override
    public void putConstrainedApexValue(ApexValue<?> key, ApexValue<?> value) {
        if (getApexValue(key).isPresent()) {
            throw new UnexpectedException(key);
        }
        putApexValue(key, value);
    }

    @Override
    public void putApexValue(ApexValue<?> key, ApexValue<?> value) {
        ApexValueUtil.assertNotCircular(this, key, null);
        ApexValueUtil.assertNotCircular(this, value, null);
        objectPropertiesHolder.put(key, value);
    }

    @Override
    public void assign(ChainedVertex lhs, ChainedVertex rhs, SymbolProvider symbols) {
        putApexValue(lhs, rhs, symbols);
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        if (METHOD_PUT.equalsIgnoreCase(vertex.getMethodName())) {
            validateParameterSize(vertex, 2);
            List<ChainedVertex> parameters = vertex.getParameters();
            ChainedVertex keyParameter = parameters.get(0);
            ChainedVertex valueParameter = parameters.get(1);
            ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(null, vertex);

            ApexValue<?> key = ScopeUtil.resolveToApexValueOrBuild(builder, keyParameter);
            ApexValue<?> value = ScopeUtil.resolveToApexValueOrBuild(builder, valueParameter);

            // Add ApexValues to ApexValueProperties
            putApexValue(key, value);
        }
        // TODO: Does this return the previous value?
        return Optional.empty();
    }

    /**
     * Handles #put methods such as
     *
     * <p>Account a = new Account(); a.put('Name', 'Acme. Inc.');
     *
     * <p>or
     *
     * <p>a.Name = 'Acme Inc.';
     */
    private ApexValue<?> putApexValue(
            ChainedVertex key, ChainedVertex value, SymbolProvider symbolProvider) {
        return this.objectPropertiesHolder.putApexValue(this, key, value, symbolProvider);
    }

    @Override
    public Optional<ApexValue<?>> getApexValue(ApexValue<?> key) {
        ApexValueUtil.assertNotCircular(this, key, null);
        return objectPropertiesHolder.getApexValue(key);
    }

    @Override
    public Optional<ApexValue<?>> getApexValue(String key) {
        return objectPropertiesHolder.getApexValue(key);
    }

    @Override
    public Map<ApexValue<?>, ApexValue<?>> getApexValueProperties() {
        return this.objectPropertiesHolder.getApexValueProperties();
    }

    @Override
    public Optional<String> getDefiningType() {
        return Optional.empty();
    }

    /**
     * Get the most specific available type. This is used for instanceof use cases. It will return
     * Account in the following case SObject obj = new Account();
     */
    public Optional<Typeable> getMostSpecificType() {
        if (getValueVertex().orElse(null) instanceof Typeable) {
            return Optional.of((Typeable) getValueVertex().get());
        } else if (getDeclarationVertex().orElse(null) != null) {
            return getDeclarationVertex();
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApexPropertiesValue<?> that = (ApexPropertiesValue<?>) o;
        return Objects.equals(objectPropertiesHolder, that.objectPropertiesHolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectPropertiesHolder);
    }
}
