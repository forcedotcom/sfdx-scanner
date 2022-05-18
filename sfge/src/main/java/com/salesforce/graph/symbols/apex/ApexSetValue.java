package com.salesforce.graph.symbols.apex;

import static com.salesforce.graph.symbols.apex.SystemNames.METHOD_EQUALS;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.collections.NonNullHashSet;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.NewSetInitExpressionVertex;
import com.salesforce.graph.vertex.NewSetLiteralExpressionVertex;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;

// TODO: Create a ApexCollectionValue that contains common Collection methods
@SuppressWarnings(
        "PMD.ConstructorCallsOverridableMethod") // Violation is thrown since setValue() in
// constructor
// invokes ApexListValue.getValues(). Since ApexListValue is final, the method can't be overriden.
// Hence the violation is incorrect.
public class ApexSetValue extends ApexValue<ApexSetValue> implements DeepCloneable<ApexSetValue> {
    private static final String METHOD_ADD = "add";
    private static final String METHOD_CLEAR = "clear";
    private static final String METHOD_CLONE = "clone";
    private static final String METHOD_CONTAINS = "contains";
    protected static final String METHOD_IS_EMPTY = "isEmpty";
    protected static final String METHOD_SIZE = "size";

    private final NonNullHashSet<ApexValue<?>> values;

    public ApexSetValue(ApexValueBuilder builder) {
        super(builder);
        this.values = CollectionUtil.newNonNullHashSet();
        setValue(builder.getValueVertex(), builder.getSymbolProvider());
    }

    /** DeepCloneable#deepClone constructor */
    private ApexSetValue(ApexSetValue other) {
        super(other, other.getReturnedFrom().orElse(null), other.getInvocable().orElse(null));
        this.values = CloneUtil.cloneNonNullHashSet(other.values);
    }

    /**
     * Constructor for Set#clone
     *
     * @param values the new values. This is a shallow clone of the original
     */
    private ApexSetValue(
            ApexSetValue other,
            NonNullHashSet<ApexValue<?>> values,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
        this.values = values;
    }

    @Override
    public ApexSetValue deepClone() {
        return new ApexSetValue(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    // Methods that return indeterminant values if this object is itself indeterminant
    private static final TreeMap<String, Function<ApexValueBuilder, IndeterminantValueProvider<?>>>
            INDETERMINANT_VALUE_PROVIDERS =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(
                                    METHOD_CONTAINS,
                                    IndeterminantValueProvider.BooleanValueProvider::get),
                            Pair.of(
                                    METHOD_EQUALS,
                                    IndeterminantValueProvider.BooleanValueProvider::get),
                            Pair.of(
                                    METHOD_IS_EMPTY,
                                    IndeterminantValueProvider.BooleanValueProvider::get),
                            Pair.of(
                                    METHOD_SIZE,
                                    IndeterminantValueProvider.IntegerValueProvider::get));

    private void setValue(@Nullable ChainedVertex valueVertex, SymbolProvider symbolProvider) {
        super.setValueVertex(valueVertex, symbolProvider);
        ApexValueBuilder builder = ApexValueBuilder.get(symbolProvider);
        // New sets can result in implicit adds.
        if (valueVertex instanceof NewSetInitExpressionVertex) {
            NewSetInitExpressionVertex list = (NewSetInitExpressionVertex) valueVertex;
            if (list.getParameters().size() > 1) {
                throw new UnexpectedException(valueVertex);
            }
            if (list.getParameters().size() == 1) {
                ChainedVertex parameter = list.getParameters().get(0);
                ApexValue<?> apexValue = ScopeUtil.resolveToApexValueOrBuild(builder, parameter);
                if (apexValue instanceof ApexListValue) {
                    for (ApexValue<?> item : ((ApexListValue) apexValue).getValues()) {
                        add(item);
                    }
                } else if (apexValue instanceof ApexSetValue) {
                    for (ApexValue<?> item : ((ApexSetValue) apexValue).getValues()) {
                        add(item);
                    }
                }
            }
        } else if (valueVertex instanceof NewSetLiteralExpressionVertex) {
            NewSetLiteralExpressionVertex set = (NewSetLiteralExpressionVertex) valueVertex;
            for (ChainedVertex chainedVertex : set.getParameters()) {
                ApexValue<?> apexValue =
                        ScopeUtil.resolveToApexValueOrBuild(builder, chainedVertex);
                add(apexValue);
            }
        }
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(this, vertex);
        String methodName = vertex.getMethodName();

        if (isIndeterminant() && INDETERMINANT_VALUE_PROVIDERS.containsKey(methodName)) {
            return Optional.ofNullable(
                    INDETERMINANT_VALUE_PROVIDERS.get(methodName).apply(builder).get());
        }

        List<ChainedVertex> parameters = vertex.getParameters();
        if (METHOD_ADD.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 1);
            ChainedVertex parameter = parameters.get(0);
            ApexValue<?> apexValue = ScopeUtil.resolveToApexValueOrBuild(builder, parameter);
            ApexValueUtil.assertNotCircular(this, apexValue, vertex);
            add(apexValue);
        } else if (METHOD_CLONE.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            // Clone creates a new set with the same values as the original set
            final NonNullHashSet<ApexValue<?>> clonedValues = CollectionUtil.newNonNullHashSet();
            for (ApexValue<?> value : values) {
                clonedValues.add(value);
            }
            return Optional.of(new ApexSetValue(this, clonedValues, this, vertex));
        } else if (METHOD_CLEAR.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            values.clear();
        } else if (METHOD_CONTAINS.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 1);
            ChainedVertex parameter = parameters.get(0);
            ApexValue<?> apexValue = ScopeUtil.resolveToApexValue(builder, parameter).orElse(null);
            if (apexValue != null) {
                return Optional.of(builder.buildBoolean(values.contains(apexValue)));
            }
            return Optional.of(builder.buildBoolean());
        } else if (METHOD_EQUALS.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 1);
            ChainedVertex parameter = parameters.get(0);
            if (parameter != null) {
                ApexValue<?> apexValue =
                        ScopeUtil.resolveToApexValue(builder, parameter).orElse(null);
                if (apexValue instanceof ApexSetValue) {
                    ApexSetValue setValue = (ApexSetValue) apexValue;
                    return Optional.of(builder.buildBoolean(values.equals(setValue.values)));
                }
            }
            return Optional.of(builder.buildBoolean());
        } else if (METHOD_IS_EMPTY.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            return Optional.of(builder.buildBoolean(values.isEmpty()));
        } else if (METHOD_SIZE.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            return Optional.of(builder.buildInteger(values.size()));
        }
        return Optional.empty();
    }

    public void add(ApexValue<?> value) {
        ApexValueUtil.assertNotCircular(this, value, null);
        this.values.add(value);
    }

    public List<ApexValue<?>> getValues() {
        return Collections.unmodifiableList(values.stream().collect(Collectors.toList()));
    }

    @Override
    public Optional<String> getDefiningType() {
        return Optional.empty();
    }
}
