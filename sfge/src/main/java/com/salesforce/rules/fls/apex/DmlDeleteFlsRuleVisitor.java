package com.salesforce.rules.fls.apex;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.DmlDeleteStatementVertex;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;

public class DmlDeleteFlsRuleVisitor extends AbstractFlsVisitor {

    private static final FlsValidationType VALIDATION_TYPE = FlsValidationType.DELETE;

    public DmlDeleteFlsRuleVisitor() {
        super(VALIDATION_TYPE);
    }

    @Override
    public void afterVisit(DmlDeleteStatementVertex vertex, SymbolProvider symbols) {
        afterVisitDmlStatementVertex(vertex, symbols);
    }
}
