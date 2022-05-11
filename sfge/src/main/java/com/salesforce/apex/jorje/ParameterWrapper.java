package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.member.Parameter;
import apex.jorje.semantic.symbol.type.TypeInfo;
import com.salesforce.graph.Schema;
import java.util.Map;

final class ParameterWrapper extends AstNodeWrapper<Parameter> {
    ParameterWrapper(Parameter node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        final Parameter node = getNode();
        properties.put(Schema.NAME, node.getName().getValue());

        final TypeInfo typeInfo = node.getType();
        properties.put(Schema.TYPE, normalizeType(typeInfo.toString()));
    }
}
