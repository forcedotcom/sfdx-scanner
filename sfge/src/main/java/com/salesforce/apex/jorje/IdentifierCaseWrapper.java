package com.salesforce.apex.jorje;

import apex.jorje.data.Identifier;
import apex.jorje.semantic.ast.statement.WhenCases;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ReflectionUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IdentifierCaseWrapper extends AstNodeWrapper<WhenCases.IdentifierCase> {
    IdentifierCaseWrapper(WhenCases.IdentifierCase node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        properties.put(Schema.IDENTIFIER, getIdentifier());
    }

    private String getIdentifier() {
        final WhenCases.IdentifierCase node = getNode();
        final java.util.List<java.lang.String> results = new ArrayList<>();
        final List<Identifier> identifiers = ReflectionUtil.getFieldValue(node, "identifiers");
        for (Identifier identifier : identifiers) {
            results.add(identifier.getValue());
        }
        if (results.size() != 1) {
            throw new UnexpectedException(node);
        }
        return results.get(0);
    }
}
