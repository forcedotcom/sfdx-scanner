package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.ClassUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class NewObjectExpressionVertex extends InvocableWithParametersVertex implements Typeable {
    private static final Consumer<GraphTraversal<Vertex, Vertex>> TRAVERSAL_CONSUMER =
            traversal ->
                    traversal
                            .out(Schema.CHILD)
                            .order(Scope.global)
                            .by(Schema.CHILD_INDEX, Order.asc);

    private final LazyOptionalVertex<BaseSFVertex> nextInvocableVertex;

    NewObjectExpressionVertex(Map<Object, Object> properties) {
        super(properties, TRAVERSAL_CONSUMER);
        this.nextInvocableVertex = _getNext();
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
    public String getCanonicalType() {
        return ApexStandardLibraryUtil.getCanonicalName(getString(Schema.TYPE));
    }

    @Override
    public Optional<String> getSymbolicName() {
        return Optional.empty();
    }

    @Override
    public List<String> getChainedNames() {
        return Collections.emptyList();
    }

    /**
     * @return the full qualified class name. The following example would return
     *     "OuterClass.InnerClass" even though only InnerClass was specified.
     *     <p>public OuterClass { public void myMethod() { InnerClass ic = new InnerClass(); }
     *     public class InnerClass() {
     *     <p>} }
     */
    public Optional<String> getResolvedInnerClassName() {
        return ClassUtil.getMoreSpecificClassName(this, getCanonicalType());
    }

    @Override
    public Optional<InvocableVertex> getNext() {
        InvocableVertex invocableVertex = (InvocableVertex) nextInvocableVertex.get().orElse(null);
        return Optional.ofNullable(invocableVertex);
    }

    private LazyOptionalVertex<BaseSFVertex> _getNext() {
        return new LazyOptionalVertex<>(
                () ->
                        g().V(getId())
                                .out(Schema.PARENT)
                                .hasLabel(ASTConstants.NodeType.REFERENCE_EXPRESSION)
                                .out(Schema.PARENT)
                                .hasLabel(
                                        ASTConstants.NodeType.METHOD_CALL_EXPRESSION,
                                        ASTConstants.NodeType.VARIABLE_EXPRESSION));
    }
}
