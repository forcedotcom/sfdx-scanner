package com.salesforce.graph.vertex;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.out;

import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;

public class SwitchStatementVertex extends BaseSFVertex {
    private final LazyVertex<ChainedVertex> switchExpressionVertex;
    private final LazyVertexList<CaseVertex> caseVertices;
    private final LazyVertexList<WhenBlockVertex> whenBlocks;

    SwitchStatementVertex(Map<Object, Object> properties) {
        super(properties);
        this.switchExpressionVertex = _getSwitchExpressionVertex();
        this.caseVertices = _getCaseVertices();
        this.whenBlocks = _getWhenBlocks();
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
    public boolean startsInnerScope() {
        return true;
    }

    public ChainedVertex getSwitchExpressionVertex() {
        return switchExpressionVertex.get();
    }

    private LazyVertex<ChainedVertex> _getSwitchExpressionVertex() {
        // The switch expression is the first child of the SwitchStatement. It can be a variable, a
        // method call
        // expression, or a TriggerVariableExpresion vertex
        return new LazyVertex<>(
                () -> g().V(getId()).out(Schema.CHILD).has(Schema.FIRST_CHILD, true));
    }

    /** The list of all cases covered in ALL case statements */
    public List<CaseVertex> getCaseVertices() {
        return caseVertices.get();
    }

    private LazyVertexList<CaseVertex> _getCaseVertices() {
        return new LazyVertexList<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .hasLabel(NodeType.VALUE_WHEN_BLOCK)
                                .out(Schema.CHILD)
                                .hasLabel(NodeType.IDENTIFIER_CASE, NodeType.LITERAL_CASE)
                                .order(Scope.global)
                                // Primary and secondary sort by parent/child index
                                .by(out(Schema.PARENT).values(Schema.CHILD_INDEX))
                                .by(Schema.CHILD_INDEX, Order.asc));
    }

    public List<WhenBlockVertex> getWhenBlocks() {
        return whenBlocks.get();
    }

    private LazyVertexList<WhenBlockVertex> _getWhenBlocks() {
        return new LazyVertexList<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .hasLabel(
                                        NodeType.ELSE_WHEN_BLOCK,
                                        NodeType.TYPE_WHEN_BLOCK,
                                        NodeType.VALUE_WHEN_BLOCK)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc));
    }
}
