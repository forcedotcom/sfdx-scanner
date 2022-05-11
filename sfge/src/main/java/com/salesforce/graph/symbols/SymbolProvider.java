package com.salesforce.graph.symbols;

import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.List;
import java.util.Optional;

public interface SymbolProvider {
    /**
     * Get the value represented by {@code key} at this moment. Resolving it to the most specific
     * value.
     */
    Optional<ChainedVertex> getValue(String key);

    /** Get the declaration represented by {@code key} at this moment. */
    Optional<Typeable> getTypedVertex(String key);

    /**
     * Follow the scope chain represented by {@code keySequence}, returning the declaration
     * represened by the final entry of the array.
     */
    Optional<Typeable> getTypedVertex(List<String> keySequence);

    /**
     * Attempt to resolve the object to a more specific value if possible. Will return {@link
     * Optional#empty()} if it is already the most specific value.
     */
    Optional<ChainedVertex> getValue(ChainedVertex value);

    /**
     * Resolves a variable expression to the {@link ApexValue that it points to}
     *
     * @return
     */
    Optional<ApexValue<?>> getApexValue(VariableExpressionVertex var);

    /**
     * Resolves an invocable call expression to the {@link ApexValue that it points to}
     *
     * @param vertex
     */
    Optional<ApexValue<?>> getReturnedValue(InvocableVertex vertex);

    /**
     * Get the ApexValue represented by {@code key} at this moment. Resolving it to the most
     * specific value.
     */
    Optional<ApexValue<?>> getApexValue(String key);

    /**
     * Get the resolved value at the time the method was invoked. This is used to resolve what was
     * invoked during object constructors
     */
    ChainedVertex getValueAtTimeOfInvocation(InvocableVertex vertex, ChainedVertex value);

    /**
     * @return traverses the scope stack returning the first scope that is a {@link
     *     AbstractClassInstanceScope}. This is the scope which all "this" values should resolve to.
     *     May return null if only static methods have been invoked.
     */
    Optional<AbstractClassInstanceScope> getClosestClassInstanceScope();
}
