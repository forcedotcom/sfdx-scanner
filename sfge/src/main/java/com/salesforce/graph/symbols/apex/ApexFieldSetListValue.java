package com.salesforce.graph.symbols.apex;

import static com.salesforce.graph.symbols.apex.ApexListValue.METHOD_CONTAINS;
import static com.salesforce.graph.symbols.apex.ApexListValue.METHOD_IS_EMPTY;
import static com.salesforce.graph.symbols.apex.ApexListValue.METHOD_SIZE;
import static com.salesforce.graph.symbols.apex.SystemNames.METHOD_EQUALS;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnimplementedMethodException;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.schema.FieldSet;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;

public final class ApexFieldSetListValue extends ApexValue<ApexFieldSetListValue>
        implements Typeable {
    public static final String TYPE =
            ApexStandardLibraryUtil.getListDeclaration(
                    ApexStandardLibraryUtil.Type.SCHEMA_FIELD_SET_MEMBER);

    private static final String METHOD_GET = "get";

    private final FieldSet fieldSet;

    protected ApexFieldSetListValue(FieldSet fieldSet, ApexValueBuilder builder) {
        super(builder);
        this.fieldSet = fieldSet;
    }

    private ApexFieldSetListValue(ApexFieldSetListValue other) {
        super(other);
        this.fieldSet = CloneUtil.clone(other.fieldSet);
    }

    @Override
    public ApexFieldSetListValue deepClone() {
        return new ApexFieldSetListValue(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Optional<String> getDefiningType() {
        return Optional.empty();
    }

    @Override
    public String getCanonicalType() {
        return TYPE;
    }

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

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(this, vertex);
        String methodName = vertex.getMethodName();

        if (METHOD_GET.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 1);
            // We don't care about the index
            return Optional.of(builder.buildFieldSetMember(fieldSet));
        } else if (INDETERMINANT_VALUE_PROVIDERS.containsKey(methodName)) {
            return Optional.ofNullable(
                    INDETERMINANT_VALUE_PROVIDERS.get(methodName).apply(builder).get());
        } else {
            throw new UnimplementedMethodException(this, vertex);
        }
    }
}
