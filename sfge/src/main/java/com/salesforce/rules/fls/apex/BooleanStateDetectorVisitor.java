package com.salesforce.rules.fls.apex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BooleanExpressionVertex;
import com.salesforce.graph.vertex.PrefixExpressionVertex;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

public class BooleanStateDetectorVisitor extends DefaultNoOpPathVertexVisitor {
    enum Operator {
        NOT(ASTConstants.OPERATOR_NEGATE),
        AND(ASTConstants.OPERATOR_AND),
        OR(ASTConstants.OPERATOR_OR);

        private final String representation;

        Operator(String representation) {
            this.representation = representation;
        }

        static Optional<Operator> getOperator(String operatorString) {
            for (Operator operator : Operator.values()) {
                if (operator.representation.equalsIgnoreCase(operatorString)) {
                    return Optional.of(operator);
                }
            }
            return Optional.empty();
        }
    }

    private final Stack<StandardConditionVertex> standardConditions;

    protected BooleanStateDetectorVisitor() {
        this.standardConditions = new Stack<>();
    }

    protected boolean containsPotentiallyValidCheck() {
        if (standardConditions.isEmpty()) {
            throw new UnexpectedException(this);
        }
        StandardConditionVertex current = standardConditions.peek();
        return includesNonNegatedClause(current);
    }

    protected boolean includesNegatedClause(BaseSFVertex vertex) {
        // !!!x == !x, so if any of the clauses within this vertex have an odd negation level, then
        // such a clause is
        // effectively negated.
        return getContainedNegationLevels(vertex, 0).stream().anyMatch(i -> i % 2 == 1);
    }

    protected boolean includesNonNegatedClause(BaseSFVertex vertex) {
        // !!x == x, so if any of the clauses within this vertex have an even negation level, then
        // such a clause is effectively
        // non-negated.
        return getContainedNegationLevels(vertex, 0).stream().anyMatch(i -> i % 2 == 0);
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

    protected List<Integer> getContainedNegationLevels(BaseSFVertex vertex, int level) {
        if (vertex instanceof StandardConditionVertex.Positive) {
            // A positive standard condition means that the path in question is the one where the
            // condition was satisfied.
            // So we want to get the negation level of all contained children.
            return getContainedNegationLevels(getOnlyChild(vertex), level);
        } else if (vertex instanceof StandardConditionVertex.Negative) {
            // A negative standard condition means that the path in question is the one where the
            // condition wasn't satisfied.
            // i.e., it's a kind of implicit negation. So we want to increment the negation level by
            // one, then process
            // the children.
            return getContainedNegationLevels(getOnlyChild(vertex), level + 1);
        } else if (vertex instanceof PrefixExpressionVertex
                && isPrefixNegated((PrefixExpressionVertex) vertex)) {
            // NOT-expressions are explicit negation, so increment the level and recurse into the
            // children.
            return getContainedNegationLevels(getOnlyChild(vertex), level + 1);
        } else if (vertex instanceof BooleanExpressionVertex) {
            // For boolean expressions, combine the negation results from each side into one array.
            // E.g., (!!!x && !y) => [3, 1].
            BooleanExpressionVertex booleanExpressionVertex = (BooleanExpressionVertex) vertex;
            List<Integer> negationLevels = new ArrayList<>();
            negationLevels.addAll(
                    getContainedNegationLevels(booleanExpressionVertex.getLhs(), level));
            negationLevels.addAll(
                    getContainedNegationLevels(booleanExpressionVertex.getRhs(), level));
            return negationLevels;
        } else {
            // Any other vertex type is a base case, and we should just return a singleton list of
            // the current negation level.
            return Collections.singletonList(level);
        }
    }

    private boolean isPrefixNegated(PrefixExpressionVertex prefixExpressionVertex) {
        final String operatorStr = prefixExpressionVertex.getOperator();
        final Optional<Operator> operatorOptional = Operator.getOperator(operatorStr);
        if (!operatorOptional.isPresent()) {
            throw new TodoException("Operator is not handled yet: " + operatorStr);
        }
        final Operator operator = operatorOptional.get();
        if (!Operator.NOT.equals(operator)) {
            throw new TodoException("Operator is not handled for conditional clause: " + operator);
        }
        return true;
    }

    private BaseSFVertex getOnlyChild(BaseSFVertex vertex) {
        final List<BaseSFVertex> children = vertex.getChildren();
        if (children.size() > 1) {
            throw new TodoException(
                    "Not yet handling more than one child of StandardConditionVertex: " + vertex);
        }

        return children.get(0);
    }
}
