package com.salesforce.graph.vertex;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import java.util.concurrent.ConcurrentMap;

/**
 * Provides type information for classes that don't contain vertices, such as {@link
 * com.salesforce.graph.symbols.apex.ApexSingleValue} when that value is synthesized from a method
 * without a body.
 */
public final class SyntheticTypedVertex implements Typeable {
    private static final ConcurrentMap<String, SyntheticTypedVertex> TYPES =
            CollectionUtil.newConcurrentMap();

    private final String type;

    private SyntheticTypedVertex(String type) {
        if (type == null) {
            throw new UnexpectedException("Type can't be null");
        }
        this.type = type;
    }

    @Override
    public String getCanonicalType() {
        return type;
    }

    @Override
    public boolean matchesParameterType(Typeable parameterVertex) {
        return type.equalsIgnoreCase(parameterVertex.getCanonicalType());
    }

    /**
     * Get a case insensitive instance that matches {@code canonicalType}. {@link
     * #getCanonicalType()} may return a different case value if this method was first called with
     * the same type using a different case.
     */
    public static SyntheticTypedVertex get(String canonicalType) {
        return TYPES.computeIfAbsent(canonicalType, k -> new SyntheticTypedVertex(canonicalType));
    }
}
