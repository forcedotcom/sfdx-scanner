package com.salesforce.rules.fls.apex.operations;

import com.salesforce.exception.TodoException;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BooleanExpressionVertex;
import com.salesforce.graph.vertex.PrefixExpressionVertex;
import com.salesforce.graph.vertex.StandardConditionVertex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NegationContainmentUtil {
    private NegationContainmentUtil() {}

    public static boolean includesNonNegatedClause(BaseSFVertex vertex) {
        // !!x == x, so if any of the clauses within this vertex have an even negation level, then
        // such a clause is effectively non-negated.
        return getContainedNegationLevels(vertex, 0).stream().anyMatch(i -> i % 2 == 0);
    }

    public static boolean includesNegatedClause(BaseSFVertex vertex) {
        // !!!x == !x, so if any of the clauses within this vertex have an odd negation level, then
        // such a clause is effectively negated.
        return getContainedNegationLevels(vertex, 0).stream().anyMatch(i -> i % 2 == 1);
    }

    private static List<Integer> getContainedNegationLevels(BaseSFVertex vertex, int level) {
        if (vertex instanceof StandardConditionVertex.Positive) {
            // A positive standard condition means that the path in question is the one where the
            // condition was satisfied. So we want to get the negation level of the child.
            return getContainedNegationLevels(vertex.getOnlyChild(), level);
        } else if (vertex instanceof StandardConditionVertex.Negative) {
            // A negative standard condition means that the path in question is the one where the
            // condition wasn't satisfied.
            // i.e., it's a kind of implicit negation. So we want to increment the negation level by
            // one, then process the child.
            return getContainedNegationLevels(vertex.getOnlyChild(), level + 1);
        } else if (vertex instanceof PrefixExpressionVertex) {
            // Currently the only prefix expression we support for this is NOT expressions, which
            // are an explicit negation. So increment the level and recurse into the child.
            PrefixExpressionVertex prefix = (PrefixExpressionVertex) vertex;
            if (!prefix.isOperatorNegation()) {
                throw new TodoException(
                        "Operator is not handled for conditional clause: " + prefix.getOperator());
            }
            return getContainedNegationLevels(vertex.getOnlyChild(), level + 1);
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
}
