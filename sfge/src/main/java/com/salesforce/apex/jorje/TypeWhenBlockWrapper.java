package com.salesforce.apex.jorje;

import apex.jorje.data.Identifier;
import apex.jorje.semantic.ast.statement.TypeWhenBlock;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ReflectionUtil;
import java.util.Map;

public class TypeWhenBlockWrapper extends AstNodeWrapper<TypeWhenBlock> {
    TypeWhenBlockWrapper(TypeWhenBlock node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        final TypeWhenBlock node = getNode();
        properties.put(Schema.TYPE, typeRefToString(node.getTypeRef()));
        Identifier identifier = ReflectionUtil.getFieldValue(node, "name");
        properties.put(Schema.NAME, identifier.getValue());
    }
}
