package com.salesforce.graph.symbols.apex;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/** Represents a simple value such as a String or Integer. */
public abstract class ApexSimpleValue<T extends ApexSimpleValue<?, ?>, V> extends ApexValue<T>
        implements Typeable {
    protected final String apexType;
    protected final V value;

    ApexSimpleValue(String apexType, V value, ApexValueBuilder builder) {
        super(builder);
        this.apexType = apexType;
        this.value = value;
    }

    protected ApexSimpleValue(ApexSimpleValue<T, V> other) {
        super(other);
        this.apexType = other.apexType;
        this.value = other.value;
    }

    @Override
    public boolean isValuePresent() {
        return getValue().isPresent();
    }

    @Override
    public final String getCanonicalType() {
        return apexType;
    }

    @Override
    public Optional<Typeable> getTypeVertex() {
        Optional<Typeable> result = super.getTypeVertex();
        if (!result.isPresent()) {
            result = Optional.of(this);
        }
        return result;
    }

    @Override
    public final Optional<String> getDefiningType() {
        return Optional.of(getCanonicalType());
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        throw new UnexpectedException(vertex);
    }

    public Optional<V> getValue() {
        return Optional.ofNullable(value);
    }

    // IMPORTANT: This method was modified to make apexType case insensitive
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApexSimpleValue<?, ?> that = (ApexSimpleValue<?, ?>) o;
        return Objects.equals(
                        StringUtils.toRootLowerCase(apexType),
                        StringUtils.toRootLowerCase(that.apexType))
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(StringUtils.toRootLowerCase(apexType), value);
    }
}
