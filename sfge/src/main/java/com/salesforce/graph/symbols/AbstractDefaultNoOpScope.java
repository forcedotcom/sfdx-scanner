package com.salesforce.graph.symbols;

import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.List;
import java.util.Optional;

public abstract class AbstractDefaultNoOpScope implements MutableSymbolProvider {
    @Override
    public Optional<ChainedVertex> getValue(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<Typeable> getTypedVertex(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<Typeable> getTypedVertex(List<String> keySequence) {
        return Optional.empty();
    }

    @Override
    public Optional<ChainedVertex> getValue(ChainedVertex value) {
        return Optional.empty();
    }

    @Override
    public Optional<ApexValue<?>> getApexValue(VariableExpressionVertex var) {
        return Optional.empty();
    }

    @Override
    public Optional<ApexValue<?>> getApexValue(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<ApexValue<?>> getApexValueFromInstanceScope(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<ApexValue<?>> getReturnedValue(InvocableVertex vertex) {
        return Optional.empty();
    }

    @Override
    public ChainedVertex getValueAtTimeOfInvocation(InvocableVertex vertex, ChainedVertex value) {
        return value;
    }

    @Override
    public Optional<AbstractClassInstanceScope> getClosestClassInstanceScope() {
        return Optional.empty();
    }

    @Override
    public ApexValue<?> updateVariable(
            String key, ApexValue<?> value, SymbolProvider currentScope) {
        return null;
    }
}
