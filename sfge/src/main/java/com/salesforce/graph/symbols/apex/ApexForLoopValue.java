package com.salesforce.graph.symbols.apex;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneableApexValue;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.ops.ObjectPropertiesUtil;
import com.salesforce.graph.symbols.DeepCloneContextProvider;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

public final class ApexForLoopValue extends ApexPropertiesValue<ApexForLoopValue>
        implements DeepCloneableApexValue<ApexForLoopValue> {
    private final List<ApexValue<?>> items;

    ApexForLoopValue(ApexValueBuilder builder) {
        super(builder);
        this.items = new ArrayList<>();
        setValue(builder.getValueVertex(), builder.getSymbolProvider());
        if (valueVertex != null
                && !(valueVertex instanceof VariableExpressionVertex.ForLoop)
                && !(valueVertex instanceof NewListInitExpressionVertex)
                && !(valueVertex instanceof NewListLiteralExpressionVertex)
                && !(valueVertex instanceof NewSetInitExpressionVertex)
                && !(valueVertex instanceof NewSetLiteralExpressionVertex)) {
            throw new UnexpectedException(valueVertex);
        }
    }

    ApexForLoopValue(ApexIterableCollectionValue value, ApexValueBuilder builder) {
        super(builder);
        this.items = new ArrayList<>();
        setValue(value);
        if (valueVertex != null
                && !(valueVertex instanceof VariableExpressionVertex.ForLoop)
                && !(valueVertex instanceof NewListInitExpressionVertex)
                && !(valueVertex instanceof NewListLiteralExpressionVertex)
                && !(valueVertex instanceof NewSetInitExpressionVertex)
                && !(valueVertex instanceof NewSetLiteralExpressionVertex)) {
            throw new UnexpectedException(valueVertex);
        }
    }

    private ApexForLoopValue(ApexForLoopValue other) {
        this(other, other.getReturnedFrom().orElse(null), other.getInvocable().orElse(null));
    }

    private ApexForLoopValue(
            ApexForLoopValue other,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
        this.items = CloneUtil.cloneArrayList(other.items);
    }

    @Override
    public ApexForLoopValue deepClone() {
        return DeepCloneContextProvider.cloneIfAbsent(this, () -> new ApexForLoopValue(this));
    }

    @Override
    public ApexForLoopValue deepCloneForReturn(
            @Nullable ApexValue<?> returnedFrom, @Nullable InvocableVertex invocable) {
        return new ApexForLoopValue(this, returnedFrom, invocable);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    public List<ApexValue<?>> getForLoopValues() {
        return Collections.unmodifiableList(items);
    }

    private void setValue(@Nullable ChainedVertex valueVertex, SymbolProvider symbolProvider) {
        this.items.clear();

        ApexIterableCollectionValue collectionValue = null;
        if (valueVertex instanceof VariableExpressionVertex.ForLoop) {
            ChainedVertex forLoopValues =
                    ((VariableExpressionVertex.ForLoop) valueVertex).getForLoopValues();
            ApexValue<?> apexValue =
                    ScopeUtil.resolveToApexValue(symbolProvider, forLoopValues).orElse(null);
            if (apexValue instanceof ApexIterableCollectionValue) {
                collectionValue = (ApexIterableCollectionValue) apexValue;
            }
        } else if (valueVertex instanceof NewListLiteralExpressionVertex) {
            collectionValue =
                    ApexValueBuilder.get(symbolProvider).valueVertex(valueVertex).buildList();
        } else if (valueVertex instanceof NewSetLiteralExpressionVertex) {
            collectionValue =
                    ApexValueBuilder.get(symbolProvider).valueVertex(valueVertex).buildSet();
        }

        if (collectionValue != null) {
            setValue(collectionValue);
        }
    }

    private void setValue(ApexIterableCollectionValue collectionValue) {
        // Pass on sanitization information
        if (collectionValue instanceof AbstractSanitizableValue) {
            AbstractSanitizableValue.copySanitization(
                    (AbstractSanitizableValue) collectionValue, this);
        }
        // Add items in the list
        for (ApexValue<?> item : collectionValue.getValues()) {
            items.add(item);
        }
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        if (isIndeterminant()) {
            return Optional.empty();
        }

        // Apply the method to each of the values in the array
        ApexForLoopValue result = deepClone();
        if (result.items != null) {
            result.items.clear();
            for (ApexValue<?> apexValue : items) {
                // This will check for null access and throw an exception if this would never
                // continue in production
                apexValue.checkForNullAccess(vertex, symbols);
                Optional<ApexValue<?>> optApplied = apexValue.apply(vertex, symbols);
                ApexValue<?> valueToAdd;
                if (optApplied.isPresent()) {
                    valueToAdd = optApplied.get();
                } else {
                    // TODO: path expander needs to expand on this method call and return a value
                    // For now, we clone the same apexValue as a temporary bandage
                    // to handle cases where we don't know what value a method call returns.
                    // NOTE: The returned value is incorrect until we fix this.
                    valueToAdd = apexValue.deepClone();
                }

                if (valueToAdd != null) {
                    result.items.add(valueToAdd);
                }
            }
        }

        return Optional.of(result);
    }

    @Override
    public Optional<String> getDefiningType() {
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ApexForLoopValue that = (ApexForLoopValue) o;
        return Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), items);
    }

    @Override
    public Optional<ApexValue<?>> getOrAddDefault(String key) {

        // TODO: evaluate if this approach is correct
        // Look up items and get the existing/default from the first property value encountered.
        for (ApexValue<?> item : items) {
            if (item instanceof ApexPropertiesValue<?>) {
                return ((ApexPropertiesValue) item).getOrAddDefault(key);
            }
        }

        // If we are here, either none of the items were ApexPropertiesValue
        // or there are no known items.
        // Checking for the latter.
        if (items.isEmpty()) {
            // This means we don't know the items that the loop was initialized with
            // Return an indeterminant value.
            return Optional.of(
                    ObjectPropertiesUtil.getDefaultIndeterminantValue(key, objectPropertiesHolder));
        }

        return Optional.empty();
    }
}
