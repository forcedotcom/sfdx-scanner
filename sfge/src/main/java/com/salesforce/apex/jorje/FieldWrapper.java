package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.member.Field;
import apex.jorje.semantic.symbol.member.variable.FieldInfo;
import com.salesforce.graph.Schema;
import java.util.Map;

final class FieldWrapper extends AstNodeWrapper<Field> {
    FieldWrapper(Field node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        final FieldInfo fieldInfo = getNode().getFieldInfo();
        properties.put(Schema.NAME, fieldInfo.getName());
        properties.put(Schema.TYPE, normalizeType(fieldInfo.getEmitType().getApexName()));
    }
}
