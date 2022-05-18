package com.salesforce.graph.symbols.apex;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Represents an Apex value that is returned from a standard Apex method, but the method is stubbed
 * out and doesn't have a real vertex. An example would be MyObject__c.SObjectType.getDescribe();
 *
 * <p>The #getDescribeMethod is an empty stub. The class {@link
 * com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult} is used in place of the vertex
 * that would normally represent this value.
 */
public abstract class ApexStandardValue<T extends ApexStandardValue<?>> extends ApexValue<T>
        implements Typeable {
    private final String apexType;

    protected ApexStandardValue(String apexType, ApexValueBuilder builder) {
        super(builder);
        this.apexType = apexType;
    }

    protected ApexStandardValue(ApexStandardValue<?> other) {
        this(other, other.getReturnedFrom().orElse(null), other.getInvocable().orElse(null));
    }

    protected ApexStandardValue(
            ApexStandardValue other,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
        this.apexType = other.apexType;
    }

    @Override
    public final String getCanonicalType() {
        return apexType;
    }

    @Override
    public final Optional<String> getDefiningType() {
        return Optional.of(getCanonicalType());
    }

    /** Return the value implied by the method call. */
    public abstract Optional<ApexValue<?>> executeMethod(
            InvocableWithParametersVertex invocableExpression,
            MethodVertex method,
            SymbolProvider symbols);

    @Override
    public String toString() {
        return "ApexStandardValue{" + "apexType='" + apexType + '\'' + "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ApexStandardValue<?> that = (ApexStandardValue<?>) o;
        return Objects.equals(apexType, that.apexType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), apexType);
    }
}
