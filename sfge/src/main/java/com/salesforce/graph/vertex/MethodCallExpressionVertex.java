package com.salesforce.graph.vertex;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;

import com.google.common.collect.ImmutableList;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import com.salesforce.graph.visitor.TypedVertexVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class MethodCallExpressionVertex extends InvocableWithParametersVertex implements KeyVertex {
    private static final Consumer<GraphTraversal<Vertex, Vertex>> TRAVERSAL_CONSUMER =
            traversal ->
                    traversal
                            .out(Schema.CHILD)
                            // The first child is the reference expression
                            .not(has(Schema.FIRST_CHILD, true))
                            .order(Scope.global)
                            .by(Schema.CHILD_INDEX, Order.asc);

    private final LazyVertex<AbstractReferenceExpressionVertex> referenceExpression;
    // TODO: This might need to move back into InvocableWithParametersVertex for situations
    // such as "new Account().doSomething()"
    private final LazyOptionalVertex<MethodCallExpressionVertex> nextChainedExpression;
    private final LazyOptionalVertex<InvocableWithParametersVertex> previous;
    private final List<String> chainedNames;
    private final ExpressionType expressionType;
    private final FirstInitializer firstInitializer;

    MethodCallExpressionVertex(Map<Object, Object> properties) {
        this(properties, null);
    }

    MethodCallExpressionVertex(Map<Object, Object> properties, Object supplementalParam) {
        super(properties, TRAVERSAL_CONSUMER);
        this.referenceExpression = _getReferenceVertex();
        this.chainedNames = ImmutableList.copyOf(referenceExpression.get().getNames());
        if (supplementalParam != null) {
            this.expressionType = (ExpressionType) supplementalParam;
        } else {
            this.expressionType = _getExpressionType();
        }
        this.nextChainedExpression = _getNext();
        this.previous = _getPrevious();
        this.firstInitializer = new FirstInitializer();
    }

    @Override
    public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
        return visitor.visit(this, symbols);
    }

    @Override
    public boolean visit(SymbolProviderVertexVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
        visitor.afterVisit(this, symbols);
    }

    @Override
    public void afterVisit(SymbolProviderVertexVisitor visitor) {
        visitor.afterVisit(this);
    }

    @Override
    public Optional<String> getKeyName() {
        if (expressionType.equals(ExpressionType.KEY_VALUE)) {
            return Optional.of((String) getProperty(Schema.KEY_NAME));
        } else {
            return Optional.empty();
        }
    }

    public Optional<ClassRefExpressionVertex> getClassRefExpression() {
        AbstractReferenceExpressionVertex abstractReferenceExpression = referenceExpression.get();
        if (abstractReferenceExpression instanceof ReferenceExpressionVertex) {
            ReferenceExpressionVertex referenceExpression =
                    (ReferenceExpressionVertex) abstractReferenceExpression;
            return referenceExpression.gtClassRefExpression();
        }
        return Optional.empty();
    }

    // TODO: Law of demeter
    public AbstractReferenceExpressionVertex getReferenceExpression() {
        return referenceExpression.get();
    }

    @Override
    public ExpressionType getExpressionType() {
        return expressionType;
    }

    private ExpressionType _getExpressionType() {
        String value = getString(Schema.EXPRESSION_TYPE);
        if (value != null) {
            return ExpressionType.valueOf(value);
        } else {
            return ExpressionType.SIMPLE;
        }
    }

    @Override
    public List<String> getChainedNames() {
        // This was retrieved off of the ReferenceExpression
        return chainedNames;
    }

    @Override
    public Optional<String> getSymbolicName() {
        if (!chainedNames.isEmpty()) {
            return Optional.of(chainedNames.get(0));
        } else {
            return Optional.empty();
        }
    }

    /** This has to be calculated dynamically because of #cloneRemovingFirstName */
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        chainedNames.forEach(n -> sb.append(n).append('.'));
        sb.append(getMethodName());
        return sb.toString();
    }

    @Override
    public String toString() {
        return "MethodCallExpressionVertex{"
                + "fullMethodName="
                + getFullMethodName()
                + ", referenceVertex="
                + referenceExpression
                + ", chainedNames="
                + chainedNames
                + ", properties="
                + properties
                + '}';
    }

    /**
     * Returns the first method call from a chain.
     *
     * <p>returns the Schema.getGlobalDescribe() vertex if called on 'Schema.getGlobalDescribe' or
     * 'get' vertex in the following example.
     *
     * <p>Schema.getGlobalDescribe().get('Account')
     */
    public MethodCallExpressionVertex getFirst() {
        return firstInitializer.get();
    }

    /** Thread safe initializer. This doesn't use LazyVertex since it is more complex. */
    private final class FirstInitializer
            extends UncheckedLazyInitializer<MethodCallExpressionVertex> {
        @Override
        protected MethodCallExpressionVertex initialize() throws ConcurrentException {
            // TODO: Efficiency. Replace with a repeat clause instead of iterating
            // Traverse down the child relationship until reaching a reference expression that
            // doesn't
            // have a parent MethodCallExpression
            MethodCallExpressionVertex next = MethodCallExpressionVertex.this;
            MethodCallExpressionVertex result = MethodCallExpressionVertex.this;

            while (next != null) {
                next =
                        SFVertexFactory.loadSingleOrNull(
                                g(),
                                g().V(next.getId())
                                        .out(Schema.CHILD)
                                        .hasLabel(
                                                NodeType.EMPTY_REFERENCE_EXPRESSION,
                                                NodeType.REFERENCE_EXPRESSION)
                                        .out(Schema.CHILD)
                                        .hasLabel(NodeType.METHOD_CALL_EXPRESSION));
                if (next != null) {
                    result = next;
                }
            }

            return result;
        }
    }

    /**
     * Returns the first method call from a chain.
     *
     * <p>returns the getName() vertex if called on 'Schema.getGlobalDescribe' or 'getName' vertex
     * in the following example.
     *
     * <p>Schema.getGlobalDescribe().get('Account')
     */
    @Override
    public InvocableVertex getLast() {
        List<InvocableVertex> results = firstToList();
        return results.get(results.size() - 1);
    }

    /** @return a list of all methods in the chain, starting with the first. */
    @Override
    public List<InvocableVertex> firstToList() {
        List<InvocableVertex> results = new ArrayList<>();

        InvocableVertex next = getFirst();
        while (next != null) {
            results.add(next);
            next = next.getNext().orElse(null);
        }

        return results;
    }

    @Override
    public Optional<InvocableVertex> getNext() {
        MethodCallExpressionVertex methodCallExpression = nextChainedExpression.get().orElse(null);
        return Optional.ofNullable(methodCallExpression);
    }

    private LazyOptionalVertex<InvocableWithParametersVertex> _getPrevious() {
        return new LazyOptionalVertex<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .hasLabel(
                                        NodeType.EMPTY_REFERENCE_EXPRESSION,
                                        NodeType.REFERENCE_EXPRESSION)
                                .out(Schema.CHILD)
                                .hasLabel(
                                        NodeType.METHOD_CALL_EXPRESSION, NodeType.SOQL_EXPRESSION));
    }

    public Optional<InvocableWithParametersVertex> getPrevious() {
        return previous.get();
    }

    public String getFullMethodName() {
        return (String) properties.get(Schema.FULL_METHOD_NAME);
    }

    public String getMethodName() {
        return (String) properties.get(Schema.METHOD_NAME);
    }

    @Override
    public <T> T accept(TypedVertexVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /**
     * @return true if the method is qualified by a 'this' expression. i.e. Returns true for
     *     "this.aList.size();"
     */
    public boolean isThisReference() {
        if (referenceExpression.get() instanceof ReferenceExpressionVertex) {
            return ((ReferenceExpressionVertex) referenceExpression.get())
                    .getThisVariableExpression()
                    .isPresent();
        } else {
            return false;
        }
    }

    /**
     * @return True if the method is qualified by an empty reference expression. i.e., returns true
     *     for "someMethod()", but not "this.someMethod()" or "a.someMethod()".
     */
    public boolean isEmptyReference() {
        return referenceExpression.get() instanceof EmptyReferenceExpressionVertex;
    }

    private LazyVertex<AbstractReferenceExpressionVertex> _getReferenceVertex() {
        return new LazyVertex<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .hasLabel(
                                        NodeType.EMPTY_REFERENCE_EXPRESSION, // Assignment of other
                                        // variable
                                        NodeType.REFERENCE_EXPRESSION) // Invocation of method
                                .has(Schema.FIRST_CHILD, true));
    }

    /**
     * Get the next method call if this method is part of a chain such as, next points from left to
     * right when reading code. Is represented as a parent child relationship in AST where
     * #getDescribe would be the topmost parent in the following example.
     *
     * <p>Schema.getGlobalDescribe().get(objectName).getDescribe()
     */
    private LazyOptionalVertex<MethodCallExpressionVertex> _getNext() {
        return new LazyOptionalVertex<>(
                () ->
                        g().V(getId())
                                .out(Schema.PARENT)
                                .hasLabel(
                                        NodeType.EMPTY_REFERENCE_EXPRESSION, // Assignment of other
                                        // variable
                                        NodeType.REFERENCE_EXPRESSION) // Invocation of method
                                .out(Schema.PARENT)
                                .hasLabel(NodeType.METHOD_CALL_EXPRESSION));
    }
}
