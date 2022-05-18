package com.salesforce.rules.fls.apex;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.DmlUpsertStatementVertex;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;

public class DmlUpsertFlsRuleVisitor extends AbstractFlsVisitor {
    private static final FlsValidationType VALIDATION_TYPE = FlsValidationType.UPSERT;

    public DmlUpsertFlsRuleVisitor() {
        super(VALIDATION_TYPE);
    }

    @Override
    public void afterVisit(DmlUpsertStatementVertex vertex, SymbolProvider symbols) {
        afterVisitDmlStatementVertex(vertex, symbols);
    }
}
