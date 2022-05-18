package com.salesforce.rules.fls.apex;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.DmlMergeStatementVertex;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;

public class DmlMergeFlsRuleVisitor extends AbstractFlsVisitor {
    private static final FlsValidationType VALIDATION_TYPE = FlsValidationType.MERGE;

    public DmlMergeFlsRuleVisitor() {
        super(VALIDATION_TYPE);
    }

    @Override
    public void afterVisit(DmlMergeStatementVertex vertex, SymbolProvider symbols) {
        afterVisitDmlStatementVertex(vertex, symbols);
    }
}
