package com.salesforce.graph.symbols.apex;

import static com.salesforce.graph.symbols.apex.SystemNames.METHOD_EQUALS;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.collections.NonNullHashMap;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MapEntryNodeVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.NewMapInitExpressionVertex;
import com.salesforce.graph.vertex.NewMapLiteralExpressionVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a generic Apex map type. See
 * https://developer.salesforce.com/docs/atlas.en-us.apexref.meta/apexref/apex_methods_system_map.htm
 */
public final class ApexMapValue extends AbstractApexMapValue<ApexMapValue> {
    private static final Logger LOGGER = LogManager.getLogger(ApexMapValue.class);

    public static final String METHOD_CLONE = "clone";
    public static final String METHOD_DEEP_CLONE = "deepClone";
    public static final String METHOD_REMOVE = "remove";

    private final NonNullHashMap<ApexValue<?>, ApexValue<?>> values;

    ApexMapValue(ApexValueBuilder builder) {
        super(builder);
        this.values = CollectionUtil.newNonNullHashMap();
        setValue(builder.getValueVertex(), builder.getSymbolProvider());
    }

    /** DeepCloneable#deepClone constructor */
    private ApexMapValue(ApexMapValue other) {
        super(other, other.getReturnedFrom().orElse(null), other.getInvocable().orElse(null));
        this.values = CloneUtil.cloneNonNullHashMap(other.values);
    }

    /**
     * Constructor for Map#clone and Map#deepClone
     *
     * @param values the new values. This is a shallow or deep clone of the original
     */
    private ApexMapValue(
            ApexMapValue other,
            NonNullHashMap<ApexValue<?>, ApexValue<?>> values,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
        this.values = values;
    }

    @Override
    public ApexMapValue deepClone() {
        return new ApexMapValue(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    public Map<ApexValue<?>, ApexValue<?>> getValues() {
        return Collections.unmodifiableMap(values);
    }

    // Methods that return indeterminant values if this object is itself indeterminant
    private static final TreeMap<String, Function<ApexValueBuilder, IndeterminantValueProvider<?>>>
            INDETERMINANT_VALUE_PROVIDERS =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(
                                    METHOD_CONTAINS_KEY,
                                    IndeterminantValueProvider.BooleanValueProvider::get),
                            Pair.of(
                                    METHOD_EQUALS,
                                    IndeterminantValueProvider.BooleanValueProvider::get),
                            Pair.of(
                                    METHOD_IS_EMPTY,
                                    IndeterminantValueProvider.BooleanValueProvider::get),
                            Pair.of(
                                    METHOD_KEY_SET,
                                    IndeterminantValueProvider.SetValueProvider::getStringSet),
                            Pair.of(
                                    METHOD_SIZE,
                                    IndeterminantValueProvider.IntegerValueProvider::get),
                            Pair.of(
                                    METHOD_VALUES,
                                    IndeterminantValueProvider.ListValueProvider::get));

    protected void setValue(@Nullable ChainedVertex valueVertex, SymbolProvider symbolProvider) {
        super.setValueVertex(valueVertex, symbolProvider);
        ApexValueBuilder builder = ApexValueBuilder.get(symbolProvider);
        // New maps can result in implicit adds
        if (valueVertex instanceof NewMapInitExpressionVertex) {
            NewMapInitExpressionVertex map = (NewMapInitExpressionVertex) valueVertex;
            if (map.getParameters().size() > 1) {
                throw new UnexpectedException(valueVertex);
            }
            if (map.getParameters().size() == 1) {
                ChainedVertex parameter = map.getParameters().get(0);
                ApexValue<?> apexValue = ScopeUtil.resolveToApexValueOrBuild(builder, parameter);
                if (apexValue instanceof ApexMapValue) {
                    ApexValueUtil.assertNotCircular(this, apexValue, null);
                    for (Map.Entry<ApexValue<?>, ApexValue<?>> entry :
                            ((ApexMapValue) apexValue).getValues().entrySet()) {
                        ApexValueUtil.assertNotCircular(this, entry.getKey(), null);
                        ApexValueUtil.assertNotCircular(this, entry.getValue(), null);
                    }
                    values.putAll(((ApexMapValue) apexValue).getValues());
                }
            }
        } else if (valueVertex instanceof NewMapLiteralExpressionVertex) {
            NewMapLiteralExpressionVertex map = (NewMapLiteralExpressionVertex) valueVertex;
            for (MapEntryNodeVertex menv : map.getEntries()) {
                BaseSFVertex mapKeyVertex = menv.getKey();
                BaseSFVertex mapValueVertex = menv.getValue();
                if (mapKeyVertex instanceof ChainedVertex
                        && mapValueVertex instanceof ChainedVertex) {
                    ApexValue<?> key =
                            ScopeUtil.resolveToApexValueOrBuild(
                                    builder.deepClone(), (ChainedVertex) mapKeyVertex);
                    ApexValue<?> value =
                            ScopeUtil.resolveToApexValueOrBuild(
                                    builder.deepClone(), (ChainedVertex) mapValueVertex);
                    ApexValueUtil.assertNotCircular(this, key, null);
                    ApexValueUtil.assertNotCircular(this, value, null);
                    values.put(key, value);
                } else {
                    throw new UnexpectedException(valueVertex);
                }
            }
        }
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(this, vertex);
        List<ChainedVertex> parameters = vertex.getParameters();
        String methodName = vertex.getMethodName();

        if (isIndeterminant() && INDETERMINANT_VALUE_PROVIDERS.containsKey(methodName)) {
            return Optional.ofNullable(
                    INDETERMINANT_VALUE_PROVIDERS.get(methodName).apply(builder).get());
        }

        if (METHOD_CLONE.equalsIgnoreCase(vertex.getMethodName())) {
            validateParameterSize(vertex, 0);
            // Clone creates a new map with the same values as the original map
            final NonNullHashMap<ApexValue<?>, ApexValue<?>> clonedValues =
                    CollectionUtil.newNonNullHashMap();
            for (Map.Entry<ApexValue<?>, ApexValue<?>> entry : values.entrySet()) {
                clonedValues.put(entry.getKey(), entry.getValue());
            }
            return Optional.of(new ApexMapValue(this, clonedValues, this, vertex));
        } else if (METHOD_DEEP_CLONE.equalsIgnoreCase(vertex.getMethodName())) {
            // deepClone creates a new map with each of the entries cloned
            return Optional.of(
                    new ApexMapValue(this, CloneUtil.cloneNonNullHashMap(values), this, vertex));
        } else if (METHOD_CONTAINS_KEY.equalsIgnoreCase(vertex.getMethodName())) {
            validateParameterSize(vertex, 1);
            ChainedVertex parameter = parameters.get(0);
            ApexValue<?> key = ScopeUtil.resolveToApexValue(symbols, parameter).orElse(null);
            if (key != null) {
                return Optional.of(builder.buildBoolean(values.containsKey(key)));
            }
        } else if (METHOD_GET.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 1);
            if (this.isIndeterminant()) {
                // We don't know what values can be returned here, so let's return
                // an indeterminant value of matching type
                builder.declarationVertex(SyntheticTypedVertex.get(getValueType()))
                        .withStatus(ValueStatus.INDETERMINANT);
                return Optional.of(builder.build());
            }
            ChainedVertex parameter = parameters.get(0);
            ApexValue<?> key = ScopeUtil.resolveToApexValue(symbols, parameter).orElse(null);
            if (key != null) {
                ApexValue<?> value = values.get(key);
                return Optional.ofNullable(value);
            }
        } else if (METHOD_IS_EMPTY.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            return Optional.of(builder.buildBoolean(values.isEmpty()));
        } else if (METHOD_EQUALS.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 1);
            ChainedVertex parameter = parameters.get(0);
            ApexValue<?> apexValue = ScopeUtil.resolveToApexValue(builder, parameter).orElse(null);
            if (apexValue instanceof ApexMapValue) {
                ApexMapValue mapValue = (ApexMapValue) apexValue;
                return Optional.of(builder.buildBoolean(values.equals(mapValue.values)));
            }
            return Optional.of(builder.buildBoolean());
        } else if (METHOD_KEY_SET.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            ApexSetValue apexSetValue = builder.buildSet();
            for (ApexValue<?> key : values.keySet()) {
                apexSetValue.add(key);
            }
            return Optional.of(apexSetValue);
        } else if (METHOD_PUT.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 2);
            ChainedVertex keyParameter = parameters.get(0);
            ChainedVertex valueParameter = parameters.get(1);
            ApexValue<?> key = ScopeUtil.resolveToApexValueOrBuild(builder, keyParameter);
            ApexValue<?> value = ScopeUtil.resolveToApexValueOrBuild(builder, valueParameter);
            ApexValueUtil.assertNotCircular(this, key, vertex);
            ApexValueUtil.assertNotCircular(this, value, vertex);
            if (key != null) {
                return Optional.ofNullable(values.put(key, value));
            }
        } else if (METHOD_REMOVE.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 1);
            ChainedVertex parameter = parameters.get(0);
            ApexValue<?> apexValue = ScopeUtil.resolveToApexValue(symbols, parameter).orElse(null);
            ApexValueUtil.assertNotCircular(this, apexValue, vertex);
            if (apexValue != null) {
                return Optional.ofNullable(values.remove(apexValue));
            } else {
                return Optional.empty();
            }
        } else if (METHOD_SIZE.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            return Optional.of(builder.buildInteger(values.size()));
        } else if (METHOD_VALUES.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            ApexListValue list = builder.buildList();
            for (ApexValue<?> value : values.values()) {
                list.add(value);
            }
            return Optional.of(list);
        } else {
            // Only log a warning. It's possible that methods are called on objects that don't
            // support then when a path
            // is traversed where the path will eventually be filtered out. We let these methods
            // turn into a no-op.
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Unknown method. vertex=" + vertex);
            }
        }

        return Optional.empty();
    }
}
