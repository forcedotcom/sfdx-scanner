package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.modifier.Annotation;
import com.salesforce.graph.Schema;
import java.util.Map;

final class AnnotationWrapper extends AstNodeWrapper<Annotation> {
    AnnotationWrapper(Annotation node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        properties.put(Schema.NAME, getNode().getType().getApexName());
    }
}
