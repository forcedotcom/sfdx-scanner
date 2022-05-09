package com.salesforce.graph.symbols.apex;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Represents an Apex map type. See
 * https://developer.salesforce.com/docs/atlas.en-us.apexref.meta/apexref/apex_methods_system_map.htm
 */
public abstract class AbstractApexMapValue<T extends AbstractApexMapValue> extends ApexValue<T>
        implements DeepCloneable<T> {
    protected static final String METHOD_CONTAINS_KEY = "containsKey";
    protected static final String METHOD_GET = "get";
    protected static final String METHOD_KEY_SET = "keySet";
    protected static final String METHOD_IS_EMPTY = "isEmpty";
    protected static final String METHOD_PUT = "put";
    protected static final String METHOD_SIZE = "size";
    protected static final String METHOD_VALUES = "values";

    private static final String TYPE_PATTERN_STR = "map<\\s*([^\\s]*),\\s*([^\\s]*)\\s*>";
    private static final Pattern TYPE_PATTERN =
            Pattern.compile(TYPE_PATTERN_STR, Pattern.CASE_INSENSITIVE);

    private final String keyType;
    private final String valueType;

    protected AbstractApexMapValue(ApexValueBuilder builder) {
        this(identifyType(builder.getDeclarationVertex()), builder);
    }

    // TODO: Don't accept Optional
    @SuppressWarnings("PMD.NullAssignment")
    protected AbstractApexMapValue(Optional<Pair<String, String>> type, ApexValueBuilder builder) {
        this(
                type.map(Pair::getLeft).orElse(null),
                type.isPresent() ? type.get().getRight() : null,
                builder);
    }

    protected AbstractApexMapValue(String keyType, String valueType, ApexValueBuilder builder) {
        super(builder);
        this.keyType = keyType;
        this.valueType = valueType;
    }

    protected AbstractApexMapValue(
            AbstractApexMapValue other,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
        this.keyType = other.keyType;
        this.valueType = other.valueType;
    }

    public String getKeyType() {
        return keyType;
    }

    public String getValueType() {
        return valueType;
    }

    @Override
    public Optional<String> getDefiningType() {
        return Optional.empty();
    }

    private static Optional<Pair<String, String>> identifyType(Typeable typeable) {
        if (typeable == null) {
            return Optional.empty();
        }

        final String canonicalType = typeable.getCanonicalType();
        final Matcher matcher = TYPE_PATTERN.matcher(canonicalType);
        if (matcher.find()) {
            if (matcher.groupCount() != 2) {
                throw new UnexpectedException(
                        "Expected to find two types in map declaration: " + typeable);
            }
            return Optional.of(Pair.of(matcher.group(1), matcher.group(2)));
        }

        // If we don't find a match, we are still okay.
        return Optional.empty();
    }
}
