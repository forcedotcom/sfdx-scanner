package com.salesforce.graph.vertex;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.visitor.VertexPredicateVisitor;

public interface VertexPredicate {
    /**
     * Return true if the implementer is interested in this vertex.<br>
     * <br>
     * WARNING: the {@link SymbolProvider} parameter is experimental and may be removed in future
     * releases. It should be used sparingly, for performance reasons.
     */
    boolean test(BaseSFVertex vertex, SymbolProvider provider);

    /** Dispatch to a {@link VertexPredicateVisitor} */
    void accept(VertexPredicateVisitor visitor);
}
