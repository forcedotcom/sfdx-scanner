package com.salesforce.rules.fls.apex;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;
import com.salesforce.rules.fls.apex.operations.NegationContainmentUtil;
import java.util.Stack;

public class BooleanStateDetectorVisitor extends DefaultNoOpPathVertexVisitor {
    private final Stack<StandardConditionVertex> standardConditions;

    protected BooleanStateDetectorVisitor() {
        this.standardConditions = new Stack<>();
    }

    protected boolean containsPotentiallyValidCheck() {
        if (standardConditions.isEmpty()) {
            throw new UnexpectedException(this);
        }
        StandardConditionVertex current = standardConditions.peek();
        return NegationContainmentUtil.includesNonNegatedClause(current);
    }

    @Override
    public boolean visit(StandardConditionVertex.Positive vertex, SymbolProvider symbols) {
        standardConditions.push(vertex);
        return true;
    }

    @Override
    public boolean visit(StandardConditionVertex.Negative vertex, SymbolProvider symbols) {
        standardConditions.push(vertex);
        return true;
    }

    @Override
    public void afterVisit(StandardConditionVertex.Positive vertex, SymbolProvider symbols) {
        standardConditions.pop();
    }

    @Override
    public void afterVisit(StandardConditionVertex.Negative vertex, SymbolProvider symbols) {
        standardConditions.pop();
    }
}
