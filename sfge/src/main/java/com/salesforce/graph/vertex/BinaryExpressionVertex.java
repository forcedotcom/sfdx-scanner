package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BinaryExpressionVertex extends TODO_FIX_HIERARCHY_ChainedVertex
        implements OperatorVertex, NullAccessCheckedVertex {
    private final ChainedVertex lhs;
    private final ChainedVertex rhs;

    private static final String DISPLAY_TEMPLATE = "Operator [%s]";

    BinaryExpressionVertex(Map<Object, Object> properties) {
        this(properties, null);
    }

    BinaryExpressionVertex(Map<Object, Object> properties, Object supplementalParam) {
        super(properties, supplementalParam);
        List<BaseSFVertex> children = getChildren();
        if (children.size() != 2) {
            throw new UnexpectedException(this);
        }
        this.lhs = (ChainedVertex) children.get(0);
        this.rhs = (ChainedVertex) children.get(1);
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

    /** Return Typeable if the binary expression will be implicitly converted to another type. */
    public Optional<Typeable> getTypedVertex(SymbolProvider symbols) {
        Optional<Typeable> result = Optional.empty();

        /** Handle string concatenation. String + AnythingElse will always be a String */
        if (getOperator().equals(ASTConstants.OPERATOR_ADDITION)) {
            if (lhs instanceof LiteralExpressionVertex.SFString
                    || rhs instanceof LiteralExpressionVertex.SFString) {
                result = Optional.of(SyntheticTypedVertex.get(ASTConstants.TYPE_STRING));
            }

            if (!result.isPresent() && lhs instanceof BinaryExpressionVertex) {
                result = ((BinaryExpressionVertex) lhs).getTypedVertex(symbols);
            }

            if (!result.isPresent() && rhs instanceof BinaryExpressionVertex) {
                result = ((BinaryExpressionVertex) rhs).getTypedVertex(symbols);
            }
        }

        return result;
    }

    public ChainedVertex getLhs() {
        return lhs;
    }

    public ChainedVertex getRhs() {
        return rhs;
    }

    @Override
    public String getOperator() {
        return getString(Schema.OPERATOR);
    }

    @Override
    public String getDisplayName() {
        return String.format(DISPLAY_TEMPLATE, getOperator());
    }
}
