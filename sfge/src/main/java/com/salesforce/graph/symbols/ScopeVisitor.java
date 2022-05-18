package com.salesforce.graph.symbols;

import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.visitor.VertexVisitor;
import java.util.Optional;

public interface ScopeVisitor extends VertexVisitor {
    /**
     * Called before the path that represents a method is invoked. The implementer should make
     * {@link MethodInvocationScope} available to resolve values.
     */
    void pushMethodInvocationScope(MethodInvocationScope methodInvocationScope);

    /**
     * Pop the scope that was previously pushed by {@link
     * #pushMethodInvocationScope(MethodInvocationScope)}
     *
     * @param invocable
     */
    MethodInvocationScope popMethodInvocationScope(InvocableVertex invocable);

    /**
     * @return A symbol provider that has accumulated state of the current path and allows updates.
     */
    MutableSymbolProvider getMutableSymbolProvider();

    /**
     * Invoked by the PathWalker before a path is traversed. The implementer should return the
     * {@code PathScopeVisitor} that is identified by {@code methodCallExpression} if this is an
     * instance variable currently in scope, or if it is a method on the same class that the
     * implementer already corresponds to.
     *
     * <p>Example: public class MyClass { public void doSomething() { MyOtherClass c = new
     * MyOtherClass(); c.logSomething(); // The ScopeVisitor should return the //
     * ClassInstanceScopeVisitor that corresponds to 'c'
     *
     * <p>someOtherMethod(); // The ScopeVisitor should return itself
     *
     * <p>SomeOtherClass.someStaticMethod(); // The ScopeVisitor should return Optional#empty() }
     *
     * <p>public void someOtherMethod() { } }
     *
     * <p>public class MyOtherClass() { public void logSomething() { } }
     *
     * @param invocable the method call that resulted in {@code path} being visited.
     */
    Optional<PathScopeVisitor> getImplementingScope(InvocableVertex invocable, MethodVertex method);

    /**
     * Called on the visitor that contains {@code methodCallExpression} after the corresponding path
     * is walked.
     *
     * @param invocable expression that caused the path to be walked. This can be kept in a map for
     *     delayed resolution of a value that is returned from another method.
     * @param returnValue The value returned from the method.
     */
    Optional<ApexValue<?>> afterMethodCall(
            InvocableVertex invocable, MethodVertex method, ApexValue<?> returnValue);
}
