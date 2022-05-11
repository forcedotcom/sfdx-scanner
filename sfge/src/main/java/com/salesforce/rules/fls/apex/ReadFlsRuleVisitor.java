package com.salesforce.rules.fls.apex;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SoqlExpressionVertex;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.rules.fls.apex.operations.FlsValidationCentral;

public class ReadFlsRuleVisitor extends AbstractFlsVisitor {
    private boolean hasSoqlBeenVisited; // defaults to false

    public ReadFlsRuleVisitor(
            FlsValidationType validationType, FlsValidationCentral validationCentral) {
        super(validationType, validationCentral);
    }

    @Override
    public void afterVisit(SoqlExpressionVertex vertex, SymbolProvider symbols) {
        if (!shouldCollectInfo(vertex)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Vertex does not match. Nothing to analyze here: " + vertex);
            }
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Visiting SoqlExpressionVertex: " + vertex);
        }
        validationCentral.createExpectedValidations(vertex, symbols);
        hasSoqlBeenVisited = true;
    }

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        // Check if we encountered a new DML operation invoked through Database namespace
        if (shouldCollectInfo(vertex)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Encountered database operation: " + vertex.getFullMethodName());
            }
            validationCentral.createExpectedValidations(vertex, symbols);
            hasSoqlBeenVisited = true;
        }

        if (hasSoqlBeenVisited) {
            // check stripInaccessible(). Since this needs to happen after the read operation,
            // we cannot use the sanitization check that's used with DML
            validationCentral.performStripInaccessibleValidationForRead(vertex, symbols);
        }
    }

    @Override
    public void afterVisit(StandardConditionVertex.Negative vertex, SymbolProvider symbols) {
        if (!hasSoqlBeenVisited) {
            super.afterVisit(vertex, symbols);
        }
    }

    @Override
    public void afterVisit(StandardConditionVertex.Positive vertex, SymbolProvider symbols) {
        if (!hasSoqlBeenVisited) {
            super.afterVisit(vertex, symbols);
        }
    }
}
