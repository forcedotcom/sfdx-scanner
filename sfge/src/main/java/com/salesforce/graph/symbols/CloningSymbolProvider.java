package com.salesforce.graph.symbols;

import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.List;
import java.util.Optional;

/**
 * A symbol provider that will clone all ApexValues before they are returned. This is for cases
 * where the caller is interested in the value at a specific point in time.
 */
public final class CloningSymbolProvider implements SymbolProvider {
    private final SymbolProvider delegate;

    public CloningSymbolProvider(SymbolProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<ChainedVertex> getValue(String key) {
        // Vertices are singletons
        return delegate.getValue(key);
    }

    @Override
    public Optional<Typeable> getTypedVertex(String key) {
        // Vertices are singletons
        return delegate.getTypedVertex(key);
    }

    @Override
    public Optional<Typeable> getTypedVertex(List<String> keySequence) {
        // Vertices are singletons
        return delegate.getTypedVertex(keySequence);
    }

    @Override
    public Optional<ChainedVertex> getValue(ChainedVertex value) {
        // Vertices are singletons
        return delegate.getValue(value);
    }

    @Override
    public Optional<ApexValue<?>> getApexValue(VariableExpressionVertex var) {
        return delegate.getApexValue(var).map(a -> a.deepClone());
    }

    @Override
    public Optional<ApexValue<?>> getReturnedValue(InvocableVertex vertex) {
        return delegate.getReturnedValue(vertex).map(a -> a.deepClone());
    }

    @Override
    public Optional<ApexValue<?>> getApexValue(String key) {
        return delegate.getApexValue(key).map(a -> a.deepClone());
    }

    @Override
    public Optional<ApexValue<?>> getApexValueFromInstanceScope(String key) {
        return delegate.getApexValueFromInstanceScope(key).map(a -> a.deepClone());
    }

    @Override
    public ChainedVertex getValueAtTimeOfInvocation(InvocableVertex vertex, ChainedVertex value) {
        // Vertices are singletons
        return delegate.getValueAtTimeOfInvocation(vertex, value);
    }

    @Override
    public Optional<AbstractClassInstanceScope> getClosestClassInstanceScope() {
        return delegate.getClosestClassInstanceScope();
    }
}
