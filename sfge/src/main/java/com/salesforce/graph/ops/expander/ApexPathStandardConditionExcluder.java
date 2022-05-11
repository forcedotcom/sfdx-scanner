package com.salesforce.graph.ops.expander;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.StandardConditionVertex;

/** Throws an exception if it is determined that a path will never execute. */
public interface ApexPathStandardConditionExcluder extends ApexPathExcluder {
    /**
     * Throws PathExcludedException if it is guaranteed that the {@code StandardConditionVertex}
     * will never be satisified. The following example causes {@link
     * BooleanValuePathConditionExcluder} to throw an exception for the path that executes the else
     * condition.
     *
     * <p>public foo() { String s = 'Hello'; if (s == 'Hello') { } else { } }
     */
    void exclude(StandardConditionVertex vertex, SymbolProvider symbols)
            throws PathExcludedException;
}
