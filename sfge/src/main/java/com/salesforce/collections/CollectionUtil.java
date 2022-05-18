package com.salesforce.collections;

import com.google.common.collect.ImmutableList;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ops.TypeableUtil;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.Pair;

public final class CollectionUtil {
    public static <T> ConcurrentMap<String, T> newConcurrentMap() {
        return new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    /** Create a tree map that uses cases insensitive keys */
    public static <T> TreeMap<String, T> newTreeMap() {
        return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    public static <T> TreeMap<String, T> newTreeMapOf(Pair<String, T>... pairs) {
        TreeMap<String, T> result = newTreeMap();
        for (Pair<String, T> pair : pairs) {
            result.put(pair.getKey(), pair.getValue());
        }
        return result;
    }

    public static <T> TreeMap<String, T> newTreeMapOf(String k1, T v1) {
        TreeMap<String, T> result = newTreeMap();
        result.put(k1, v1);
        return result;
    }

    public static <T> TreeMap<String, T> newTreeMapOf(String k1, T v1, String k2, T v2) {
        TreeMap<String, T> result = newTreeMap();
        result.put(k1, v1);
        result.put(k2, v2);
        return result;
    }

    public static <T> TreeMap<String, T> newTreeMapOf(
            String k1, T v1, String k2, T v2, String k3, T v3) {
        TreeMap<String, T> result = newTreeMap();
        result.put(k1, v1);
        result.put(k2, v2);
        result.put(k3, v3);
        return result;
    }

    public static <T> TreeMap<String, T> newTreeMapOf(
            String k1, T v1, String k2, T v2, String k3, T v3, String k4, T v4) {
        TreeMap<String, T> result = newTreeMap();
        result.put(k1, v1);
        result.put(k2, v2);
        result.put(k3, v3);
        result.put(k4, v4);
        return result;
    }

    public static <T> TreeMap<String, T> newTreeMapOf(
            String k1, T v1, String k2, T v2, String k3, T v3, String k4, T v4, String k5, T v5) {
        TreeMap<String, T> result = newTreeMap();
        result.put(k1, v1);
        result.put(k2, v2);
        result.put(k3, v3);
        result.put(k4, v4);
        result.put(k5, v5);
        return result;
    }

    public static <T> TreeMap<String, T> newTreeMapOf(
            String k1,
            T v1,
            String k2,
            T v2,
            String k3,
            T v3,
            String k4,
            T v4,
            String k5,
            T v5,
            String k6,
            T v6) {
        TreeMap<String, T> result = newTreeMap();
        result.put(k1, v1);
        result.put(k2, v2);
        result.put(k3, v3);
        result.put(k4, v4);
        result.put(k5, v5);
        result.put(k6, v6);
        return result;
    }

    public static <T> TreeMap<String, T> newTreeMapOf(Map<String, T> inputMap) {
        TreeMap<String, T> result = newTreeMap();
        result.putAll(inputMap);
        return result;
    }

    /** Convert {@code values} to a TreeMap where the key is provided by {@code keyMapper} */
    public static <T> TreeMap<String, T> newTreeMapOf(
            List<T> values, Function<T, String> keyMapper) {
        return values.stream()
                .collect(
                        Collectors.toMap(
                                keyMapper,
                                Function.identity(),
                                throwingMerger(),
                                CollectionUtil::newTreeMap));
    }

    /** Merger that throws when duplicates are detected */
    public static <T> BinaryOperator<T> throwingMerger() {
        return (t, t2) -> {
            throw new UnexpectedException("Duplicate key " + t);
        };
    }

    /** Create a tree set that uses cases insensitive keys */
    public static TreeSet<String> newTreeSet() {
        return new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    }

    public static TreeSet<String> newTreeSetOf(String... values) {
        TreeSet<String> result = newTreeSet();
        result.addAll(Arrays.asList(values));
        return result;
    }

    public static TreeSet<String> newTreeSetOf(TreeSet<String> treeSet1, TreeSet<String> treeSet2) {
        TreeSet<String> result = newTreeSet();
        result.addAll(treeSet1);
        result.addAll(treeSet2);
        return result;
    }

    public static TreeSet<String> newTreeSetOf(Collection<String> collection) {
        TreeSet<String> result = newTreeSet();
        result.addAll(collection);
        return result;
    }

    public static TypeableUtil.OrderedTreeSet newOrderedTreeSet() {
        return new TypeableUtil.OrderedTreeSet();
    }

    public static TypeableUtil.OrderedTreeSet newOrderedTreeSetOf(String... values) {
        final TypeableUtil.OrderedTreeSet result = new TypeableUtil.OrderedTreeSet();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }

    public static <E> NonNullArrayList<E> newNonNullArrayList() {
        return NonNullArrayList.newInstance();
    }

    public static <K, V> NonNullHashMap<K, V> newNonNullHashMap() {
        return NonNullHashMap.newInstance();
    }

    public static <E> NonNullHashSet<E> newNonNullHashSet() {
        return NonNullHashSet.newInstance();
    }

    /**
     * Iterate over the list calling {@code factoryFunction} with the current index. Collect results
     * into an unmodifiable list.
     */
    public static <T, U> List<U> newImmutableListOf(
            List<T> values, BiFunction<Integer, T, U> factoryFunction) {
        return IntStream.range(0, values.size())
                .mapToObj(i -> factoryFunction.apply(i, values.get(i)))
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }

    private CollectionUtil() {}
}
