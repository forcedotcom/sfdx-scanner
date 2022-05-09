package com.salesforce.rules.fls.apex;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.DmlUpdateStatementVertex;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;

public class DmlUpdateFlsRuleVisitor extends AbstractFlsVisitor {
    private static final FlsValidationType VALIDATION_TYPE = FlsValidationType.UPDATE;

    public DmlUpdateFlsRuleVisitor() {
        super(VALIDATION_TYPE);
    }

    @Override
    public void afterVisit(DmlUpdateStatementVertex vertex, SymbolProvider symbols) {
        afterVisitDmlStatementVertex(vertex, symbols);
    }
}
