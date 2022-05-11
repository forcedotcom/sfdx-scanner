package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.statement.VariableDeclaration;
import apex.jorje.semantic.symbol.member.variable.LocalInfo;
import apex.jorje.semantic.symbol.type.TypeInfo;
import com.salesforce.graph.Schema;
import java.util.Map;

final class VariableDeclarationWrapper extends AstNodeWrapper<VariableDeclaration> {
    VariableDeclarationWrapper(VariableDeclaration node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        final LocalInfo localInfo = getNode().getLocalInfo();
        properties.put(Schema.NAME, localInfo.getName());

        final TypeInfo typeInfo = localInfo.getType();
        properties.put(Schema.TYPE, normalizeType(typeInfo.getApexName()));
    }
}
