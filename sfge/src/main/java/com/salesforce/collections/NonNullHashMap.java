package com.salesforce.collections;

import java.util.HashMap;
import org.apache.commons.collections4.PredicateUtils;
import org.apache.commons.collections4.map.PredicatedMap;

/** A HashMap that doesn't allow null keys or values */
public class NonNullHashMap<K, V> extends PredicatedMap<K, V> {
    private NonNullHashMap(HashMap<K, V> map) {
        super(map, PredicateUtils.notNullPredicate(), PredicateUtils.notNullPredicate());
    }

    static <K, V> NonNullHashMap<K, V> newInstance() {
        return new NonNullHashMap<>(new HashMap<>());
    }
}
