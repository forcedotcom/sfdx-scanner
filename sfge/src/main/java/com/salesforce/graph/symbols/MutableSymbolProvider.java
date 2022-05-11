package com.salesforce.graph.symbols;

import com.salesforce.graph.symbols.apex.ApexValue;

public interface MutableSymbolProvider extends SymbolProvider {
    /**
     * Update the value of the variable identified by symbolic name {@code key}
     *
     * @param key symbolic name to update
     * @param value right hand side of the expression
     * @param currentScope the current scope that is executing, this could be an inner scope that
     *     has variables which are only in scope for the life of the inner scope. Implementers can
     *     record this information for later usage. For instance {@link AbstractClassInstanceScope}
     *     retains information about local variables that were passed to methods called from the
     *     constructor.
     * @return the previous value that corresponded to {@code key}, may be null. TODO: Make this
     *     return void. The callers aren't using the value and it leads to more complicated methods
     *     for the callee
     */
    ApexValue<?> updateVariable(String key, ApexValue<?> value, SymbolProvider currentScope);
}
