package com.salesforce.collections;

import java.util.ArrayList;
import org.apache.commons.collections4.PredicateUtils;
import org.apache.commons.collections4.list.PredicatedList;

/** An ArrayList that doesn't allow null values */
public class NonNullArrayList<E> extends PredicatedList<E> {
    private NonNullArrayList(ArrayList<E> list) {
        super(list, PredicateUtils.notNullPredicate());
    }

    static <E> NonNullArrayList<E> newInstance() {
        return new NonNullArrayList<>(new ArrayList<>());
    }
}
