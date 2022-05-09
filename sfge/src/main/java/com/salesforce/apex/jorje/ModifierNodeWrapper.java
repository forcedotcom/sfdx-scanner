package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.modifier.ModifierNode;
import apex.jorje.semantic.symbol.type.ModifierTypeInfos;
import com.salesforce.graph.Schema;
import java.lang.reflect.Modifier;
import java.util.Map;

final class ModifierNodeWrapper extends AstNodeWrapper<ModifierNode> {
    ModifierNodeWrapper(ModifierNode node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        final int javaModifiers = getNode().getModifiers().getJavaModifiers();
        properties.put(Schema.MODIFIERS, javaModifiers);
        properties.put(Schema.ABSTRACT, Modifier.isAbstract(javaModifiers));
        properties.put(Schema.STATIC, Modifier.isStatic(javaModifiers));
        properties.put(Schema.GLOBAL, getNode().getModifiers().has(ModifierTypeInfos.GLOBAL));
    }
}
