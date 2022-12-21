package com.salesforce.graph.symbols.apex;

import static com.salesforce.graph.symbols.apex.SystemNames.METHOD_EQUALS;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.collections.NonNullArrayList;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.exception.UnimplementedMethodException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.ops.TypeableUtil;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.NewListInitExpressionVertex;
import com.salesforce.graph.vertex.NewListLiteralExpressionVertex;
import com.salesforce.graph.vertex.SoqlExpressionVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents an Apex list type. See
 * https://developer.salesforce.com/docs/atlas.en-us.apexref.meta/apexref/apex_methods_system_list.htm
 */
public final class ApexListValue extends AbstractSanitizableValue<ApexListValue>
        implements DeepCloneable<ApexListValue>, ApexIterableCollectionValue {
    private static final Logger LOGGER = LogManager.getLogger(ApexListValue.class);

    public static final String METHOD_ADD = "add";
    public static final String METHOD_ADD_ALL = "addAll";
    public static final String METHOD_CLEAR = "clear";
    public static final String METHOD_CLONE = "clone";
    public static final String METHOD_CONTAINS = "contains";
    public static final String METHOD_DEEP_CLONE = "deepClone";
    public static final String METHOD_GET = "get";
    public static final String METHOD_INDEX_OF = "indexOF";
    public static final String METHOD_IS_EMPTY = "isEmpty";
    public static final String METHOD_REMOVE = "remove";
    public static final String METHOD_SET = "set";
    public static final String METHOD_SIZE = "size";
    public static final String METHOD_SORT = "sort";

    private static final String TYPE_PATTERN_STR = "list<\\s*([^\\s]*)\\s*>";
    private static final Pattern TYPE_PATTERN =
            Pattern.compile(TYPE_PATTERN_STR, Pattern.CASE_INSENSITIVE);

    private final NonNullArrayList<ApexValue<?>> values;
    private final Typeable listType;

    public ApexListValue(ApexValueBuilder builder) {
        super(builder);
        this.values = CollectionUtil.newNonNullArrayList();
        Typeable typeable = builder.getMostSpecificTypedVertex().orElse(null);
        this.listType = identifyType(typeable).orElse(null);
        setValue(builder.getValueVertex(), builder.getSymbolProvider());
    }

    /** DeepCloneable#deepClone constructor */
    private ApexListValue(ApexListValue other) {
        super(other, other.getReturnedFrom().orElse(null), other.getInvocable().orElse(null));
        this.values = CloneUtil.cloneNonNullArrayList(other.values);
        this.listType = other.listType;
    }

    /**
     * Constructor for List#clone and List#deepClone
     *
     * @param values the new values. This is a shallow or deep clone of the original
     */
    private ApexListValue(
            ApexListValue other,
            NonNullArrayList<ApexValue<?>> values,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
        this.values = values;
        this.listType = other.listType;
    }

    @Override
    public ApexListValue deepClone() {
        return new ApexListValue(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    @Override
    public List<ApexValue<?>> getValues() {
        return Collections.unmodifiableList(values);
    }

    protected void setValue(@Nullable ChainedVertex valueVertex, SymbolProvider symbolProvider) {
        super.setValueVertex(valueVertex, symbolProvider);
        ApexValueBuilder builder = ApexValueBuilder.get(symbolProvider);
        // New lists and Soql expressions result in implicit adds
        if (valueVertex instanceof NewListInitExpressionVertex) {
            NewListInitExpressionVertex list = (NewListInitExpressionVertex) valueVertex;
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
        } else if (valueVertex instanceof NewListLiteralExpressionVertex) {
            NewListLiteralExpressionVertex list = (NewListLiteralExpressionVertex) valueVertex;
            for (ChainedVertex parameter : list.getParameters()) {
                ApexValue<?> apexValue = ScopeUtil.resolveToApexValueOrBuild(builder, parameter);
                add(apexValue);
            }
        } else if (valueVertex instanceof SoqlExpressionVertex) {
            ApexValue<?> apexValue =
                    ApexValueBuilder.get(symbolProvider).valueVertex(valueVertex).buildSoql();
            add(apexValue);
        }
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
                                    METHOD_INDEX_OF,
                                    IndeterminantValueProvider.IntegerValueProvider::get),
                            Pair.of(
                                    METHOD_IS_EMPTY,
                                    IndeterminantValueProvider.BooleanValueProvider::get),
                            Pair.of(
                                    METHOD_SIZE,
                                    IndeterminantValueProvider.IntegerValueProvider::get));

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
            validateParameterSizes(vertex, 1, 2);
            if (parameters.size() == 1) {
                ApexValue<?> apexValue =
                        ScopeUtil.resolveToApexValueOrBuild(builder, parameters.get(0));
                ApexValueUtil.assertNotCircular(this, apexValue, vertex);
                values.add(apexValue);
            } else if (parameters.size() == 2) {
                Integer index = convertParameterToInteger(parameters.get(0), symbols).orElse(null);
                if (index != null) {
                    ApexValue<?> apexValue =
                            ScopeUtil.resolveToApexValueOrBuild(builder, parameters.get(1));
                    ApexValueUtil.assertNotCircular(this, apexValue, vertex);
                    values.add(index, apexValue);
                }
            }
        } else if (METHOD_ADD_ALL.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 1);
            ChainedVertex parameter = parameters.get(0);
            ApexValue<?> apexValue = ScopeUtil.resolveToApexValue(builder, parameter).orElse(null);
            if (apexValue instanceof ApexListValue) {
                ApexValueUtil.assertNotCircular(this, apexValue, vertex);
                for (ApexValue<?> value : ((ApexListValue) apexValue).getValues()) {
                    ApexValueUtil.assertNotCircular(this, value, vertex);
                }
                values.addAll(((ApexListValue) apexValue).getValues());
            } else if (apexValue instanceof ApexSetValue) {
                for (ApexValue<?> value : ((ApexSetValue) apexValue).getValues()) {
                    ApexValueUtil.assertNotCircular(this, value, vertex);
                }
                values.addAll(((ApexSetValue) apexValue).getValues());
            } else {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(
                            "Unable to find parameter for addAll. vertex="
                                    + vertex
                                    + ", parameter="
                                    + apexValue);
                }
            }
        } else if (METHOD_CLONE.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            // Clone creates a new list with the same values as the original list
            final NonNullArrayList<ApexValue<?>> clonedValues =
                    CollectionUtil.newNonNullArrayList();
            for (ApexValue<?> value : values) {
                clonedValues.add(value);
            }
            return Optional.of(new ApexListValue(this, clonedValues, this, vertex));
        } else if (METHOD_CLEAR.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            values.clear();
            return Optional.empty();
        } else if (METHOD_CONTAINS.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 1);
            ChainedVertex parameter = parameters.get(0);
            ApexValue<?> apexValue = ScopeUtil.resolveToApexValue(builder, parameter).orElse(null);
            if (apexValue != null) {
                return Optional.of(builder.buildBoolean(values.contains(apexValue)));
            }
            return Optional.of(builder.buildBoolean());
        } else if (METHOD_DEEP_CLONE.equalsIgnoreCase(methodName)) {
            // All three parameters are optional
            validateParameterSizes(vertex, 0, 1, 2, 3);
            // deepClone creates a new list with each of the values cloned
            return Optional.of(
                    new ApexListValue(this, CloneUtil.cloneNonNullArrayList(values), this, vertex));
        } else if (METHOD_EQUALS.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 1);
            ChainedVertex parameter = parameters.get(0);
            ApexValue<?> apexValue = ScopeUtil.resolveToApexValue(builder, parameter).orElse(null);
            if (apexValue instanceof ApexListValue) {
                ApexListValue listValue = (ApexListValue) apexValue;
                return Optional.of(builder.buildBoolean(values.equals(listValue.values)));
            }
            return Optional.of(builder.buildBoolean());
        } else if (METHOD_GET.equalsIgnoreCase(methodName)
                || METHOD_REMOVE.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 1);
            if (isIndeterminant()) {
                return Optional.of(getIndeterminantReturnValue(builder));
            }

            ChainedVertex parameter = parameters.get(0);
            Integer index = getInteger(builder, parameter).orElse(null);
            if (index != null && index < values.size()) {
                if (METHOD_GET.equalsIgnoreCase(methodName)) {
                    return Optional.of(values.get(index));
                } else if (METHOD_REMOVE.equalsIgnoreCase(methodName)) {
                    ApexValue<?> toRemove = values.get(index);
                    values.remove(index);
                    return Optional.ofNullable(toRemove);
                } else {
                    throw new UnexpectedException("Impossible. methodName=" + methodName);
                }
            }
            if (index != null) {
                // TODO: Consider throwing an exception that would exclude the path because the
                // index is out of bounds
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Index out of bounds. vertex=" + vertex + ", this=" + this);
                }
            }
            return Optional.of(getIndeterminantReturnValue(builder));
        } else if (METHOD_INDEX_OF.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 1);
            ChainedVertex parameter = parameters.get(0);
            ApexValue<?> apexValue = ScopeUtil.resolveToApexValue(builder, parameter).orElse(null);
            if (apexValue != null) {
                return Optional.of(builder.buildInteger(values.indexOf(apexValue)));
            }
            return Optional.of(builder.buildBoolean());
        } else if (METHOD_IS_EMPTY.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            return Optional.of(builder.buildBoolean(values.isEmpty()));
        } else if (METHOD_SET.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 2);
            // TODO: What if index is out of bounds?
            final Integer index =
                    convertParameterToInteger(parameters.get(0), symbols).orElse(null);
            if (index != null) {
                ApexValue<?> apexValue =
                        ScopeUtil.resolveToApexValue(builder, parameters.get(1)).orElse(null);
                ApexValueUtil.assertNotCircular(this, apexValue, vertex);
                if (apexValue != null) {
                    values.set(index, apexValue);
                }
            }
        } else if (METHOD_SIZE.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            return Optional.of(builder.buildInteger(values.size()));
        } else if (METHOD_SORT.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            // Intentionally left blank
            // TODO: Does this need to sort?
        } else {
            throw new UnimplementedMethodException(this, vertex);
        }
        return Optional.empty();
    }

    Optional<Integer> getInteger(ApexValueBuilder builder, ChainedVertex parameter) {
        ApexValue<?> apexValue = ScopeUtil.resolveToApexValue(builder, parameter).orElse(null);
        if (apexValue instanceof ApexIntegerValue && apexValue.isDeterminant()) {
            ApexIntegerValue apexIntegerValue = (ApexIntegerValue) apexValue;
            return apexIntegerValue.getValue();
        }
        return Optional.empty();
    }

    public void add(ApexValue<?> value) {
        ApexValueUtil.assertNotCircular(this, value, null);
        this.values.add(value);
    }

    public ApexValue<?> get(int index) {
        return values.get(index);
    }

    /**
     * @return an indeterminant value of the correct type. Used when the method needs to return an
     *     item from the list but we don't have an actual value. This is either because the list is
     *     indeterminant or the parameter to a method is is indeterminant.
     */
    private ApexValue<?> getIndeterminantReturnValue(ApexValueBuilder builder) {
        return builder.withStatus(ValueStatus.INDETERMINANT)
                .declarationVertex(listType)
                .buildUnknownType();
    }

    @Override
    public Optional<String> getDefiningType() {
        return Optional.empty();
    }

    public Optional<Typeable> getListType() {
        return Optional.ofNullable(listType);
    }

    private static Optional<Typeable> identifyType(@Nullable Typeable typeable) {
        if (typeable == null) {
            return Optional.empty();
        }

        // SoqlExpression stores the canonical type and does not need to be parsed with a pattern
        // matcher
        if (typeable instanceof SoqlExpressionVertex) {
            return Optional.of(typeable);
        }

        final String canonicalType = typeable.getCanonicalType();
        Optional<String> optSubType = TypeableUtil.getListSubType(canonicalType);
        if (optSubType.isPresent()) {
            return Optional.of(SyntheticTypedVertex.get(optSubType.get()));
        }

        // If we don't find a match, we are still okay.
        return Optional.empty();
    }

    @Override
    public Optional<Typeable> getSubType() {
        return Optional.ofNullable(this.listType);
    }

    @Override
    public void markSanitized(
            MethodBasedSanitization.SanitizerMechanism sanitizerMechanism,
            MethodBasedSanitization.SanitizerDecision sanitizerDecision) {
        super.markSanitized(sanitizerMechanism, sanitizerDecision);
        // Pass on sanitization to the items in the list
        for (ApexValue<?> value : this.values) {
            if (value instanceof AbstractSanitizableValue) {
                ((AbstractSanitizableValue) value)
                        .markSanitized(sanitizerMechanism, sanitizerDecision);
            }
        }
    }
}
