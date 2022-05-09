package com.salesforce.rules.fls.apex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.DmlInsertStatementVertex;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;

public class DmlInsertFlsRuleVisitor extends AbstractFlsVisitor {

    public static final String DML_STATEMENT_TYPE =
            ASTConstants.NodeType
                    .DML_INSERT_STATEMENT; // TODO: this is naive. Revisit when supporting
    // Database.insert
    private static final FlsValidationType VALIDATION_TYPE = FlsValidationType.INSERT;

    public DmlInsertFlsRuleVisitor() {
        super(VALIDATION_TYPE);
    }

    @Override
    public void afterVisit(DmlInsertStatementVertex vertex, SymbolProvider symbols) {
        afterVisitDmlStatementVertex(vertex, symbols);
    }
}
