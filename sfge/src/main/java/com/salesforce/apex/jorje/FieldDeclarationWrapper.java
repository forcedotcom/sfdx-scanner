package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.statement.FieldDeclaration;
import com.salesforce.graph.Schema;
import java.util.Map;

public class FieldDeclarationWrapper extends AstNodeWrapper<FieldDeclaration> {
    FieldDeclarationWrapper(FieldDeclaration node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        properties.put(Schema.TYPE, normalizeType(getNode().getTypeNameUsed().toString()));
    }
}
