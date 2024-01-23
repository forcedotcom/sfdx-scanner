package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;
import java.util.Optional;

public class ReferenceExpressionVertex extends AbstractReferenceExpressionVertex
        implements NamedVertex {
    private final LazyOptionalVertex<ClassRefExpressionVertex> classRefExpressionVertex;

    /**
     * Presence of this vertex indicates that the reference was qualified with a 'this' reference
     */
    private final LazyOptionalVertex<ThisVariableExpressionVertex> thisVariableExpression;
    /**
     * Presence of this vertex indicates that the reference was qualified with a {@code super}
     * reference.
     */
    private final LazyOptionalVertex<SuperVariableExpressionVertex> superVariableExpression;

    ReferenceExpressionVertex(Map<Object, Object> properties) {
        super(properties);
        // TODO: Efficiency. This does 3 queries, is it safe to get the child and then look at the
        // type?
        this.classRefExpressionVertex = _getClassRefExpression();
        this.thisVariableExpression = _getThisVariableExpression();
        this.superVariableExpression = _getSuperVariableExpression();
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

    public Optional<ThisVariableExpressionVertex> getThisVariableExpression() {
        return thisVariableExpression.get();
    }

    public Optional<ClassRefExpressionVertex> getClassRefExpression() {
        return classRefExpressionVertex.get();
    }

    public Optional<SuperVariableExpressionVertex> getSuperVariableExpression() {
        return superVariableExpression.get();
    }

    public String getReferenceType() {
        return getString(Schema.REFERENCE_TYPE);
    }

    @Override
    public String getName() {
        return getString(Schema.NAME);
    }

    private LazyOptionalVertex<ThisVariableExpressionVertex> _getThisVariableExpression() {
        return new LazyOptionalVertex<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .hasLabel(ASTConstants.NodeType.THIS_VARIABLE_EXPRESSION));
    }

    private LazyOptionalVertex<ClassRefExpressionVertex> _getClassRefExpression() {
        return new LazyOptionalVertex<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .hasLabel(ASTConstants.NodeType.CLASS_REF_EXPRESSION));
    }

    private LazyOptionalVertex<SuperVariableExpressionVertex> _getSuperVariableExpression() {
        return new LazyOptionalVertex<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .hasLabel(ASTConstants.NodeType.SUPER_VARIABLE_EXPRESSION));
    }
}
