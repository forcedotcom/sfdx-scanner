package com.salesforce.graph.symbols.apex;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.ChainedVertex;

/**
 * Implemented by ApexValues that support complex assignments such as the following where a.Name is
 * a complex assignment.
 *
 * <p>Account a = new Account(); a.Name = 'Acme Inc.';
 */
public interface ComplexAssignable {

    void assign(ChainedVertex lhs, ChainedVertex rhs, SymbolProvider symbols);
}
