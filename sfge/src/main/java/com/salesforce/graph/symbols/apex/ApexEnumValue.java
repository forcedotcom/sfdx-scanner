package com.salesforce.graph.symbols.apex;

import static com.salesforce.graph.symbols.apex.SystemNames.METHOD_EQUALS;

import com.salesforce.apex.ApexEnum;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnimplementedMethodException;
import com.salesforce.graph.Immutable;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Represents the Apex enum type
 *
 * <p>https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/langCon_apex_enums.htm
 */
public final class ApexEnumValue extends ApexValue<ApexEnumValue>
        implements Typeable, Immutable<ApexEnumValue> {
    private static final String METHOD_NAME = "name";
    private static final String METHOD_ORDINAL = "ordinal";

    /** The enum that this value was declared as, i.e. DisplayType */
    private final ApexEnum apexEnum;

    /** The specific enum value that was assigned. i.e DisplayType.ADDRESS */
    private final ApexEnum.Value value;

    ApexEnumValue(ApexEnum apexEnum, ApexValueBuilder builder) {
        this(apexEnum, null, builder);
    }

    ApexEnumValue(ApexEnum apexEnum, @Nullable ApexEnum.Value value, ApexValueBuilder builder) {
        super(builder);
        this.apexEnum = apexEnum;
        this.value = value;
    }

    @Override
    public ApexEnumValue deepClone() {
        // It's immutable reuse
        return this;
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Optional<String> getDefiningType() {
        return Optional.of(apexEnum.getName());
    }

    @Override
    public String getCanonicalType() {
        return apexEnum.getName();
    }

    public boolean matchesType(String inputType) {
        return apexEnum.matchesType(inputType);
    }

    @Override
    public boolean isValuePresent() {
        return value != null;
    }

    @Override
    public Optional<Boolean> asTruthyBoolean() {
        if (isValuePresent()) {
            return Optional.of(true);
        } else {
            return Optional.empty();
        }
    }

    public Optional<ApexEnum.Value> getValue() {
        return Optional.ofNullable(value);
    }

    public ApexEnum getApexEnum() {
        return this.apexEnum;
    }

    private static final TreeMap<String, Function<ApexValueBuilder, IndeterminantValueProvider<?>>>
            INDETERMINANT_VALUE_PROVIDERS =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(
                                    METHOD_EQUALS,
                                    IndeterminantValueProvider.BooleanValueProvider::get),
                            Pair.of(
                                    METHOD_NAME,
                                    IndeterminantValueProvider.StringValueProvider::get),
                            Pair.of(
                                    METHOD_ORDINAL,
                                    IndeterminantValueProvider.IntegerValueProvider::get));

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(this, vertex);
        String methodName = vertex.getMethodName();

        if (isIndeterminant() && INDETERMINANT_VALUE_PROVIDERS.containsKey(methodName)) {
            return Optional.ofNullable(
                    INDETERMINANT_VALUE_PROVIDERS.get(methodName).apply(builder).get());
        }

        if (METHOD_NAME.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            return Optional.of(builder.buildString(value.getValueName()));
        } else if (METHOD_ORDINAL.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            return Optional.of(builder.buildInteger(value.getOrdinal()));
        }

        throw new UnimplementedMethodException(this, vertex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ApexEnumValue that = (ApexEnumValue) o;
        return Objects.equals(apexEnum, that.apexEnum) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), apexEnum, value);
    }
}
