package com.salesforce.graph.vertex;

import com.salesforce.graph.symbols.MethodInvocationScope;
import com.salesforce.graph.symbols.SymbolProvider;
import java.util.List;
import java.util.Optional;

/**
 * Represents a vertex that can be "Invoked" with parameters and chained together.
 *
 * <p>This is implemented as an interface instead of class inheritance because they types of
 * vertices that have this behavior don't seem related. For instance, VariableExpression,
 * NewOjbectExpression, MethodCallExpression.
 *
 * <p>TODO: Revisit if this should be class hierarchy instead of interface
 */
public interface InvocableVertex {
    /**
     * Get the next vertex when chained together. In the following example, the "MyClass"
     * NewObjectExpression would return the "someMethod" MethodeCallExpression.
     */
    Optional<InvocableVertex> getNext();

    /** @return the last vertex in the chain. returns the vertex itself if not part of a chain. */
    InvocableVertex getLast();

    /**
     * @return a list of all methods in the chain, starting with the first vertex in the chain.
     *     returns a singleton list with the vertex itself if not part of a chain.
     */
    List<InvocableVertex> firstToList();

    /** @return the vertices that represent the parameters passed to the invocable. */
    List<ChainedVertex> getParameters();

    /**
     * @return a {@code MethodInvocationScope} that contains the parameters passed to this vertex.
     */
    MethodInvocationScope resolveInvocationParameters(
            MethodVertex methodVertex, SymbolProvider symbols);
}
