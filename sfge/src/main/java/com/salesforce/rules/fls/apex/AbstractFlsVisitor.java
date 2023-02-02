package com.salesforce.rules.fls.apex;

import com.salesforce.exception.TodoException;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.*;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.rules.fls.apex.operations.FlsValidationCentral;
import com.salesforce.rules.fls.apex.operations.FlsViolationInfo;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractFlsVisitor extends BooleanStateDetectorVisitor {
    protected static final Logger LOGGER = LogManager.getLogger(AbstractFlsVisitor.class);

    protected final FlsValidationCentral validationCentral;
    private final FlsValidationType validationType;

    /** Specifies the specific vertex that the visitor should gather information about. */
    private BaseSFVertex targetVertex;

    AbstractFlsVisitor(FlsValidationType validationType) {
        this(validationType, new FlsValidationCentral(validationType));
    }

    AbstractFlsVisitor(FlsValidationType validationType, FlsValidationCentral validationCentral) {
        super();
        this.validationType = validationType;
        this.validationCentral = validationCentral;
    }

    public Set<FlsViolationInfo> getViolations() {
        return validationCentral.getViolations();
    }

    public boolean isSafe() {
        return validationCentral.getViolations().isEmpty();
    }

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        if (shouldCollectInfo(vertex)) {
            // Change gears and check if we encountered a new DML operation invoked through Database
            // namespace
            final String fullMethodName = vertex.getFullMethodName();
            if (validationType.databaseOperationMethod.equalsIgnoreCase(fullMethodName)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Encountered database operation: " + fullMethodName);
                }
                validationCentral.createExpectedValidations(vertex, symbols);
                validationCentral.tallyValidations(vertex);
            }
        }
    }

    @Override
    public void afterVisit(StandardConditionVertex.Negative vertex, SymbolProvider symbols) {
        if (containsPotentiallyValidCheck()) {
            processStandardCondition(vertex, symbols);
        }
        super.afterVisit(vertex, symbols);
    }

    @Override
    public void afterVisit(StandardConditionVertex.Positive vertex, SymbolProvider symbols) {
        if (containsPotentiallyValidCheck()) {
            processStandardCondition(vertex, symbols);
        }
        super.afterVisit(vertex, symbols);
    }

    protected boolean shouldCollectInfo(BaseSFVertex vertex) {
        // Only gather information if the target vertex hasn't been set or it has been set and it
        // matches
        return !getTargetVertex().isPresent() || getTargetVertex().get().equals(vertex);
    }

    protected void afterVisitDmlStatementVertex(DmlStatementVertex vertex, SymbolProvider symbols) {
        if (shouldCollectInfo(vertex)) {
            validationCentral.createExpectedValidations(vertex, symbols);
            validationCentral.tallyValidations(vertex);
        }
    }

    public void setTargetVertex(BaseSFVertex targetVertex) {
        this.targetVertex = targetVertex;
    }

    protected Optional<BaseSFVertex> getTargetVertex() {
        return Optional.ofNullable(targetVertex);
    }

    private void processStandardCondition(StandardConditionVertex vertex, SymbolProvider symbols) {
        final List<BaseSFVertex> children = vertex.getChildren();
        if (children.size() > 1) {
            throw new TodoException("Need to handle more than one child: " + children);
        }
        final BaseSFVertex child = children.get(0);
        // A Positive condition means the IF was satisfied, and a Negative condition means it
        // wasn't. So for the former,
        // we want to start looking for any clauses that needed to be satisfied, and for the latter,
        // we're looking for
        // clauses the needed to be unsatisfied.
        boolean seekSatisfiedClauses = vertex instanceof StandardConditionVertex.Positive;
        recursivelyProcessConditionChildren(child, symbols, seekSatisfiedClauses);
    }

    /**
     * @param expectingSatisfied - Does the path containing this vertex require that the clause it
     *     represents be satisfied? Indicates whether a base case should be processed. Toggled by
     *     some recursive calls.
     */
    private void recursivelyProcessConditionChildren(
            BaseSFVertex vertex, SymbolProvider symbols, boolean expectingSatisfied) {
        // What we do with this vertex depends on its type.
        if (vertex instanceof BooleanExpressionVertex) {
            // Boolean expressions contain two sides, and we want to check both of them.
            BooleanExpressionVertex booleanVertex = (BooleanExpressionVertex) vertex;
            BaseSFVertex lhs = booleanVertex.getLhs();
            BaseSFVertex rhs = booleanVertex.getRhs();
            if (booleanExprRequiresRecursion(booleanVertex, expectingSatisfied)) {
                if (includesNonNegatedClause(lhs)) {
                    recursivelyProcessConditionChildren(lhs, symbols, expectingSatisfied);
                }
                if (includesNonNegatedClause(rhs)) {
                    recursivelyProcessConditionChildren(rhs, symbols, expectingSatisfied);
                }
            }
        } else if (vertex instanceof PrefixExpressionVertex) {
            // For a prefix expression, pull off the first child and recursively call on that.
            // If it's a NOT-expression, we also toggle expectingSatisfied, because our expectations
            // for X should be the
            // opposite of whatever they were for !X.
            final boolean expectingSatisfiedUpdated =
                    ((PrefixExpressionVertex) vertex).isOperatorNegation() != expectingSatisfied;
            final BaseSFVertex child = vertex.getChildren().get(0);
            recursivelyProcessConditionChildren(child, symbols, expectingSatisfiedUpdated);
        } else {
            // All other vertex types are base cases, so don't recurse. If the satisfiability of the
            // expression depends
            // on this clause being satisfied, then process it.
            if (expectingSatisfied) {
                validationCentral.checkSchemaBasedFlsValidation(
                        vertex.getParent(), vertex, symbols);
            }
        }
    }

    /**
     * Indicates whether a boolean expression should be processed recursively, based on the nature
     * of its operator, its children, and whether its satisfaction is expected for this path.
     */
    // TODO: This implementation is somewhat simplistic, and there are definitely
    //       cases that it doesn't support. These cases will lead to false positives.
    //       If unsupported edge cases crop up frequently in the wild, we should
    //       enhance this to be more intelligent.
    private boolean booleanExprRequiresRecursion(
            BooleanExpressionVertex booleanExpressionVertex, boolean expectingSatisfied) {
        BaseSFVertex lhs = booleanExpressionVertex.getLhs();
        BaseSFVertex rhs = booleanExpressionVertex.getRhs();
        if (booleanExpressionVertex.isOperatorAnd() && expectingSatisfied) {
            // An AND expression expecting satisfaction should always be
            // recursively processed, since it's only true if both sides are true.
            return true;
        } else if (booleanExpressionVertex.isOperatorOr()) {
            // An OR expression should be recursively processed if it's not supposed
            // to be satisfied (since that's only true if neither side is true),
            // or if either side is literally false, since it requires the other
            // side to be true.
            return !expectingSatisfied || isLiterallyFalse(lhs) || isLiterallyFalse(rhs);
        } else if (booleanExpressionVertex.isOperatorEquals()) {
            // An EQUALS expression should be recursively processed if it's supposed
            // to be satisfied and either side is literally true, or if it's not
            // supposed to be satisfied and either side is literally false.
            return (expectingSatisfied && (isLiterallyTrue(lhs) || isLiterallyTrue(rhs)))
                    || (!expectingSatisfied && (isLiterallyFalse(lhs) || isLiterallyFalse(rhs)));
        } else if (booleanExpressionVertex.isOperatorNotEquals()) {
            // A NOT-EQUALS expression should be recursively processed if it's supposed
            // to be satisfied and either side is false true, or if it's not
            // supposed to be satisfied and either side is literally true.
            return (!expectingSatisfied && (isLiterallyTrue(lhs) || isLiterallyTrue(rhs)))
                    || (expectingSatisfied && (isLiterallyFalse(lhs) || isLiterallyFalse(rhs)));
        } else {
            // We can't say anything else for certain.
            return false;
        }
    }

    private boolean isLiterallyTrue(BaseSFVertex vertex) {
        // We can drill down into a negation.
        if (vertex instanceof PrefixExpressionVertex) {
            PrefixExpressionVertex prefix = (PrefixExpressionVertex) vertex;
            if (prefix.isOperatorNegation()) {
                // If the inner vertex is literally false, its negation is literally true.
                return isLiterallyFalse(prefix.getChild(0));
            }
        }
        // For anything else, just check if it's an instance of TRUE.
        return vertex instanceof LiteralExpressionVertex.True;
    }

    private boolean isLiterallyFalse(BaseSFVertex vertex) {
        // We can drill down into a negation.
        if (vertex instanceof PrefixExpressionVertex) {
            PrefixExpressionVertex prefix = (PrefixExpressionVertex) vertex;
            if (prefix.isOperatorNegation()) {
                // If the inner vertex is literally true, its negation is literally false.
                return isLiterallyTrue(prefix.getChild(0));
            }
        }
        // For anything else, just check if it's an instance of false.
        return vertex instanceof LiteralExpressionVertex.False;
    }
}
