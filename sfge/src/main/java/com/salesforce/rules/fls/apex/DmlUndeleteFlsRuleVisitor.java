package com.salesforce.rules.fls.apex;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.DmlUndeleteStatementVertex;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;

public class DmlUndeleteFlsRuleVisitor extends AbstractFlsVisitor {
    private static final FlsValidationType VALIDATION_TYPE = FlsValidationType.UNDELETE;

    public DmlUndeleteFlsRuleVisitor() {
        super(VALIDATION_TYPE);
    }

    @Override
    public void afterVisit(DmlUndeleteStatementVertex vertex, SymbolProvider symbols) {
        afterVisitDmlStatementVertex(vertex, symbols);
    }
}
