package com.salesforce.graph.vertex;

import com.salesforce.exception.UnexpectedException;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

/** Extends {@link LazyInitializer} removing the checked exception from {@link #get()} */
abstract class UncheckedLazyInitializer<T> extends LazyInitializer<T> {
    @Override
    public T get() {
        try {
            return super.get();
        } catch (ConcurrentException ex) {
            throw new UnexpectedException(ex);
        }
    }
}
