package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.modifier.AnnotationParameter;
import com.salesforce.graph.Schema;
import java.util.Map;

final class AnnotationParameterWrapper extends AstNodeWrapper<AnnotationParameter> {
    AnnotationParameterWrapper(AnnotationParameter node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        properties.put(Schema.VALUE, getNode().getValueAsString());
    }
}
