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
        properties.put(Schema.VIRTUAL, getNode().getModifiers().has(ModifierTypeInfos.VIRTUAL));
        properties.put(Schema.OVERRIDE, getNode().getModifiers().has(ModifierTypeInfos.OVERRIDE));

        if (getNode().getModifiers().has(ModifierTypeInfos.WITH_SHARING)) {
            properties.put(Schema.SHARING_POLICY, ASTConstants.SharingPolicy.WITH_SHARING);
        } else if (getNode().getModifiers().has(ModifierTypeInfos.WITHOUT_SHARING)) {
            properties.put(Schema.SHARING_POLICY, ASTConstants.SharingPolicy.WITHOUT_SHARING);
        } else if (getNode().getModifiers().has(ModifierTypeInfos.INHERITED_SHARING)) {
            properties.put(Schema.SHARING_POLICY, ASTConstants.SharingPolicy.INHERITED_SHARING);
        }
        /** don't store omitted sharing policy, instead */
    }
}
