package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractReferenceExpressionVertex extends BaseSFVertex {
    private final LazyOptionalVertex<ArrayLoadExpressionVertex> arrayLoadExpression;

    AbstractReferenceExpressionVertex(Map<Object, Object> properties) {
        super(properties);
        this.arrayLoadExpression = _getArrayLoadExpressionVertex();
    }

    public List<String> getNames() {
        return getStrings(Schema.NAMES);
    }

    public Optional<ArrayLoadExpressionVertex> getArrayLoadExpressionVertex() {
        return arrayLoadExpression.get();
    }

    private LazyOptionalVertex<ArrayLoadExpressionVertex> _getArrayLoadExpressionVertex() {
        return new LazyOptionalVertex<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .hasLabel(ASTConstants.NodeType.ARRAY_LOAD_EXPRESSION));
    }
}
