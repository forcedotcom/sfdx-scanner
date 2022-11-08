package com.salesforce.graph.symbols;

import static com.salesforce.graph.symbols.apex.ApexMapValue.METHOD_CLONE;
import static com.salesforce.graph.symbols.apex.ApexMapValue.METHOD_DEEP_CLONE;
import static com.salesforce.graph.symbols.apex.ApexMapValue.METHOD_REMOVE;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnimplementedMethodException;
import com.salesforce.graph.DeepCloneableApexValue;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.apex.AbstractApexMapValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.IndeterminantValueProvider;
import com.salesforce.graph.symbols.apex.ValueStatus;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Represents a map returned by a System method call. The code has no way of knowing the contents of
 * the map and returns indeterminant results
 */
public abstract class ApexStandardMapValue<T extends ApexStandardMapValue>
        extends AbstractApexMapValue<T> implements DeepCloneableApexValue<ApexStandardMapValue<T>> {

    protected ApexStandardMapValue(String keyType, String valueType, ApexValueBuilder builder) {
        super(keyType, valueType, builder);
    }

    protected ApexStandardMapValue(
            AbstractApexMapValue other,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
    }

    /**
     * @return an indeterminant value that can be returned from the #get and #remove methods
     */
    protected abstract ApexValue<?> getReturnValue(ApexValueBuilder builder, ApexValue<?> keyName);

    private static final TreeMap<String, Function<ApexValueBuilder, IndeterminantValueProvider<?>>>
            INDETERMINANT_VALUE_PROVIDERS =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(
                                    METHOD_CONTAINS_KEY,
                                    IndeterminantValueProvider.BooleanValueProvider::get),
                            Pair.of(
                                    METHOD_IS_EMPTY,
                                    IndeterminantValueProvider.BooleanValueProvider::get),
                            Pair.of(
                                    METHOD_KEY_SET,
                                    IndeterminantValueProvider.SetValueProvider::getStringSet),
                            Pair.of(
                                    METHOD_SIZE,
                                    IndeterminantValueProvider.IntegerValueProvider::get));

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(this, vertex);
        String methodName = vertex.getMethodName();

        if (INDETERMINANT_VALUE_PROVIDERS.containsKey(methodName)) {
            return Optional.ofNullable(
                    INDETERMINANT_VALUE_PROVIDERS.get(methodName).apply(builder).get());
        }

        // Treat clone and deepClone the same, there is no distinction between these because the
        // maps don't contain any values
        if (methodName.equalsIgnoreCase(METHOD_CLONE)
                || methodName.equalsIgnoreCase(METHOD_DEEP_CLONE)) {
            return Optional.of(deepCloneForReturn(this, vertex));
        } else if (methodName.equalsIgnoreCase(METHOD_GET)
                || methodName.equalsIgnoreCase(METHOD_REMOVE)) {
            validateParameterSize(vertex, 1);
            // Return a value that is specific to the map type
            ApexValue<?> keyName =
                    ScopeUtil.resolveToApexValueOrBuild(builder, vertex.getParameters().get(0));
            return Optional.of(getReturnValue(builder, keyName));
        } else if (methodName.equalsIgnoreCase(METHOD_VALUES)) {
            validateParameterSize(vertex, 0);
            return Optional.of(
                    builder.withStatus(ValueStatus.INDETERMINANT)
                            .declarationVertex(
                                    SyntheticTypedVertex.get(
                                            ApexStandardLibraryUtil.getListDeclaration(
                                                    getValueType())))
                            .buildList());
        } else {
            throw new UnimplementedMethodException(this, vertex);
        }
    }
}
