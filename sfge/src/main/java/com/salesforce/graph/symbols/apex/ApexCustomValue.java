package com.salesforce.graph.symbols.apex;

import com.salesforce.graph.DeepCloneableApexValue;
import com.salesforce.graph.ops.ObjectPropertiesUtil;
import com.salesforce.graph.symbols.DeepCloneContextProvider;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Covers CustomSettings TODO: Expand to CustomObject?
 *
 * <p>TODO: ApexCustomValue shouldn't be a subtype of AbstractSanitizableValue since it only takes
 * CRUD checks, while stripInaccessible() is for FLS checks. For now, this is handled by {@link
 * com.salesforce.rules.fls.apex.operations.SanitizationChecker}
 */
public final class ApexCustomValue extends ApexPropertiesValue<ApexCustomValue>
        implements DeepCloneableApexValue<ApexCustomValue> {
    /** The type of the object, MyObject__c.SObjectType has a type of MyObject__c */
    private final Typeable typeable;

    ApexCustomValue(Typeable typeable, ApexValueBuilder builder) {
        super(builder);
        this.typeable = typeable;
    }

    ApexCustomValue(ApexCustomValue other) {
        this(other, other.getReturnedFrom().orElse(null), other.getInvocable().orElse(null));
    }

    public ApexCustomValue(
            ApexCustomValue other,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
        this.typeable = other.typeable;
    }

    @Override
    public ApexCustomValue deepClone() {
        return DeepCloneContextProvider.cloneIfAbsent(this, () -> new ApexCustomValue(this));
    }

    @Override
    public ApexCustomValue deepCloneForReturn(
            @Nullable ApexValue<?> returnedFrom, @Nullable InvocableVertex invocable) {
        // This doesn't need a cloneIfAbsent because we are generating a new value
        return new ApexCustomValue(this, returnedFrom, invocable);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Optional<String> getDeclaredType() {
        if (typeable != null) {
            return Optional.of(typeable.getCanonicalType());
        } else {
            return super.getDeclaredType();
        }
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
    public Optional<ApexValue<?>> getOrAddDefault(String key) {
        // First, check if we already know about this property on CustomSetting.
        final Optional<ApexValue<?>> valueOptional = getApexValue(key);

        if (!valueOptional.isPresent()) {
            // We don't have a way to know all the properties that a CustomSetting has.
            // Hence, when we see a lookup, we assume it is valid and return a default indeterminant
            // value.
            return Optional.of(
                    ObjectPropertiesUtil.getDefaultIndeterminantValue(key, objectPropertiesHolder));
        }

        return valueOptional;
    }
}
