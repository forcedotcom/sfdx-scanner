package com.salesforce.graph.symbols;

import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.visitor.VertexVisitor;
import java.util.Optional;

public interface SymbolProviderVertexVisitor extends VertexVisitor {
    /**
     * Initializes the scope stack with the correct {@link SymbolProvider} based on the type of
     * {@code vertex}
     *
     * @param vertex that defines the start of the path
     * @return the {@link SymbolProvider} that is on the top of the sta
     */
    SymbolProvider start(BaseSFVertex vertex);

    /** @return A symbol provider that has accumulated state of the current path. */
    SymbolProvider getSymbolProvider();

    /** Invoked before the {@code path} corresponding to {@code invocable} is invoked */
    PathScopeVisitor beforeMethodCall(InvocableVertex invocable, MethodVertex method);

    /** Invoked after the {@code path} corresponding to {@code invocable} is invoked */
    Optional<ApexValue<?>> afterMethodCall(InvocableVertex invocable, MethodVertex method);

    /**
     * Called after #afterVisit is called on the visitor. This needs to happen separate from {@link
     * VertexVisitor#afterVisit} because the scope must be pooped after {@link
     * PathScopeVisitor#afterVisit} is called.
     */
    void popScope(BaseSFVertex vertex);

    /**
     * Allow the Expander and Walker to put a static class into scope before the class is visited
     *
     * @param scope
     */
    void pushScope(ClassStaticScope scope);

    /**
     * Pop the previously pushed scope, the implementer should validate that the popped scope is the
     * expected one
     *
     * @param scope
     */
    void popScope(ClassStaticScope scope);
}
