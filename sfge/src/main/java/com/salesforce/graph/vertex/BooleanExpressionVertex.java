package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.List;
import java.util.Map;

public class BooleanExpressionVertex extends TODO_FIX_HIERARCHY_ChainedVertex
        implements OperatorVertex {
    private final BaseSFVertex lhs;
    private final BaseSFVertex rhs;

    BooleanExpressionVertex(Map<Object, Object> properties) {
        this(properties, null);
    }

    BooleanExpressionVertex(Map<Object, Object> properties, Object supplementalParam) {
        super(properties, supplementalParam);
        List<BaseSFVertex> children = getChildren();
        if (children.size() != 2) {
            throw new UnexpectedException(children);
        }
        this.lhs = children.get(0);
        this.rhs = children.get(1);
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

    public BaseSFVertex getLhs() {
        return lhs;
    }

    public BaseSFVertex getRhs() {
        return rhs;
    }

    @Override
    public String getOperator() {
        return getString(Schema.OPERATOR);
    }

    public boolean isOr() {
        return ASTConstants.OPERATOR_OR.equals(getOperator());
    }

    public boolean isOperatorAnd() {
        return ASTConstants.OPERATOR_AND.equals(getOperator());
    }

    public boolean isOperatorEquals() {
        return ASTConstants.OPERATOR_DOUBLE_EQUAL.equals(getOperator());
    }

    public boolean isOperatorNotEquals() {
        return ASTConstants.OPERATOR_NOT_EQUAL.equals(getOperator());
    }

    public boolean isOperatorOr() {
        return ASTConstants.OPERATOR_OR.equals(getOperator());
    }
}
