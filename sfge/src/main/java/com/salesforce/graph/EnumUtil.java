package com.salesforce.graph;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public final class EnumUtil {
    /**
     * Creates a lookup map from some value to the enum itself.
     *
     * @param clazz Enum class to map
     * @param keyFunction function that generates the correct key
     */
    public static <E extends Enum<E>, K> Map<K, E> getEnumMap(
            Class<E> clazz, Function<E, K> keyFunction) {
        final Map<K, E> map = new HashMap<>();
        for (E e : EnumSet.allOf(clazz)) {
            K key = keyFunction.apply(e);
            E previous = map.put(key, e);
            if (previous != null) {
                throw new UnexpectedException(
                        "Duplicate keys. key="
                                + key
                                + ", previousEnum="
                                + previous
                                + ", newEnum="
                                + e);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Special case of {@link #getEnumMap(Class, Function)} that uses case insensitive strings as
     * keys
     */
    public static <E extends Enum<E>> TreeMap<String, E> getEnumTreeMap(
            Class<E> clazz, Function<E, String> keyFunction) {
        return CollectionUtil.newTreeMapOf(getEnumMap(clazz, keyFunction));
    }

    private EnumUtil() {}
}
