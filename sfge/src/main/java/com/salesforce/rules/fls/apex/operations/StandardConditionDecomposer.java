package com.salesforce.rules.fls.apex.operations;

import static com.salesforce.graph.vertex.LiteralExpressionVertex.False;
import static com.salesforce.graph.vertex.LiteralExpressionVertex.True;

import com.salesforce.exception.TodoException;
import com.salesforce.graph.vertex.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Util class for decomposing {@link StandardConditionVertex} objects into components that can
 * be evaluated by a path-based rule.
 */
public final class StandardConditionDecomposer {

    private StandardConditionDecomposer() {}

    /**
     * Decompose a {@link StandardConditionVertex} into a list of {@link BaseSFVertex} objects
     * that must be satisfied in order for the condition to be met.
     * @param vertex - A {@link StandardConditionVertex.Positive} or {@link StandardConditionVertex.Negative}.
     * @return List containing {@link BaseSFVertex} objects that must be satisfied for the condition to achieve its expected outcome.
     */
    public static List<BaseSFVertex> decomposeStandardCondition(StandardConditionVertex vertex) {
        final List<BaseSFVertex> children = vertex.getChildren();
        if (children.size() > 1) {
            throw new TodoException("Need to handle more than one child: " + children);
        }
        final BaseSFVertex child = children.get(0);
        // StandardConditions are the contents of an IF clause's test. A Positive condition means
        // that the path
        // expects the expression to be satisfied, so we should limit our search to the clauses that
        // need
        // to be satisfied for this to be the case. Meanwhile, a Negative condition means that the
        // path expects
        // the expression to be unsatisfied, so we should limit our search to clauses that need to
        // be unsatisfied
        // for that to happen.
        boolean seekSatisfiedClauses = vertex instanceof StandardConditionVertex.Positive;
        return recursivelyDecomposeChildren(child, seekSatisfiedClauses);
    }

    private static List<BaseSFVertex> recursivelyDecomposeChildren(
            BaseSFVertex vertex, boolean seekingSatisfied) {
        // Create an empty list to which we'll add all the clauses that must be processed.
        List<BaseSFVertex> results = new ArrayList<>();
        // How we process a vertex depends on its type.
        if (vertex instanceof BooleanExpressionVertex) {
            BooleanExpressionVertex booleanVertex = (BooleanExpressionVertex) vertex;
            results.addAll(decomposeBooleanExpression(booleanVertex, seekingSatisfied));
        } else if (vertex instanceof PrefixExpressionVertex) {
            // For a prefix expression, pull off the first child and recursively call on that.
            // If it's a NOT-expression, we also switch whether we're seeking satisfied or
            // unsatisfied clauses, because we're now expecting the opposite of whatever we
            // were expecting previously.
            PrefixExpressionVertex prefixVertex = (PrefixExpressionVertex) vertex;
            BaseSFVertex child = vertex.getChild(0);
            results.addAll(
                    recursivelyDecomposeChildren(
                            child,
                            prefixVertex.isOperatorNegation()
                                    ? !seekingSatisfied
                                    : seekingSatisfied));
        } else if (seekingSatisfied) {
            // All other vertex types are base cases, so we don't recurse. Instead, if this vertex
            // is one that
            // we're expecting to be satisfied, we should add it to our list.
            results.add(vertex);
        }
        return results;
    }

    private static List<BaseSFVertex> decomposeBooleanExpression(
            BooleanExpressionVertex vertex, boolean seekingSatisfied) {
        List<BaseSFVertex> results = new ArrayList<>();
        BaseSFVertex lhs = vertex.getLhs();
        BaseSFVertex rhs = vertex.getRhs();
        boolean eitherSideTrue = True.isLiterallyTrue(lhs) || True.isLiterallyTrue(rhs);
        boolean eitherSideFalse = False.isLiterallyFalse(lhs) || False.isLiterallyFalse(rhs);
        // If we're expecting an AND expression to be satisfied, then by extension we're also
        // seeking for both sides to be satisfied.
        boolean expectingSatisfiedAnd = vertex.isOperatorAnd() && seekingSatisfied;
        // If we're expecting an OR expression to be satisfied and either side is equivalent to
        // a literal false, then the other side must be satisfied.
        boolean expectingSatisfiedOrFalse =
                vertex.isOperatorOr() && seekingSatisfied && eitherSideFalse;
        // If we're expecting an OR expression to be unsatisfied, then by extension we're
        // also expecting each side to be unsatisfied.
        boolean expectingUnsatisfiedOr = vertex.isOperatorOr() && !seekingSatisfied;
        // If we're expecting an EQUALS expression to be satisfied and either side is true,
        // the other side must be true.
        boolean expectingSatisfiedEqualsTrue =
                vertex.isOperatorEquals() && seekingSatisfied && eitherSideTrue;
        // If we're expecting an EQUALS expression to be unsatisfied and either side is false,
        // then the other side must be true.
        boolean expectingUnsatisfiedEqualsFalse =
                vertex.isOperatorEquals() && !seekingSatisfied && eitherSideFalse;
        // If we're expecting a NOT-EQUALS expression to be satisfied and either side is false,
        // then the other side must be true.
        boolean expectingSatisfiedNotEqualsFalse =
                vertex.isOperatorNotEquals() && seekingSatisfied && eitherSideFalse;
        // If we're expecting a NOT-EQUALS expression to be unsatisfied and either side is true,
        // then the other side must also be true.
        boolean expectingUnsatisfiedNotEqualsTrue =
                vertex.isOperatorNotEquals() && !seekingSatisfied && eitherSideTrue;

        // TODO: This system is a little bit naive, and there are definitely some edge cases
        //       that it won't support. In the fullness of time, we may wish to rework
        //       this (and the code that uses it) into something a bit more intelligent.
        if (expectingSatisfiedAnd
                || expectingSatisfiedOrFalse
                || expectingSatisfiedEqualsTrue
                || expectingUnsatisfiedEqualsFalse
                || expectingSatisfiedNotEqualsFalse
                || expectingUnsatisfiedNotEqualsTrue) {
            if (NegationContainmentUtil.includesNonNegatedClause(lhs)) {
                results.addAll(recursivelyDecomposeChildren(lhs, true));
            }
            if (NegationContainmentUtil.includesNonNegatedClause(rhs)) {
                results.addAll(recursivelyDecomposeChildren(rhs, true));
            }
        } else if (expectingUnsatisfiedOr) {
            if (NegationContainmentUtil.includesNegatedClause(lhs)) {
                results.addAll(recursivelyDecomposeChildren(lhs, false));
            }
            if (NegationContainmentUtil.includesNegatedClause(rhs)) {
                results.addAll(recursivelyDecomposeChildren(rhs, false));
            }
        }
        return results;
    }
}
