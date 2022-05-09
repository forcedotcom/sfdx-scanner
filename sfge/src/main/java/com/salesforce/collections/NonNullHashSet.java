package com.salesforce.collections;

import java.util.HashSet;
import org.apache.commons.collections4.PredicateUtils;
import org.apache.commons.collections4.set.PredicatedSet;

/** An HashSet that doesn't allow null values */
public class NonNullHashSet<E> extends PredicatedSet<E> {
    private NonNullHashSet(HashSet<E> set) {
        super(set, PredicateUtils.notNullPredicate());
    }

    static <E> NonNullHashSet<E> newInstance() {
        return new NonNullHashSet<>(new HashSet<>());
    }
}
