package com.salesforce.graph.ops.expander;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexEnumValue;
import com.salesforce.graph.symbols.apex.ApexSimpleValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.Constraint;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BooleanExpressionVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.PrefixExpressionVertex;
import com.salesforce.graph.vertex.StandardConditionVertex;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Excludes paths if the execution only occurs when the condition contains an ApexBooleanValue that
 * was generated via different mechanisms such as comparing values.
 */
// TODO: Support greaterThan, lessThan
public class BooleanValuePathConditionExcluder implements ApexPathStandardConditionExcluder {
    private static final Logger LOGGER =
            LogManager.getLogger(BooleanValuePathConditionExcluder.class);

    public static BooleanValuePathConditionExcluder getInstance() {
        return BooleanValuePathConditionExcluder.LazyHolder.INSTANCE;
    }

    @Override
    public void exclude(StandardConditionVertex vertex, SymbolProvider symbols)
            throws PathExcludedException {
        if (!(vertex instanceof StandardConditionVertex.Positive)
                && !(vertex instanceof StandardConditionVertex.Negative)) {
            throw new UnexpectedException(vertex);
        }

        List<BaseSFVertex> children = vertex.getChildren();
        if (children.size() == 1) {
            BaseSFVertex child = children.get(0);

            // Negate the returned value if the whole condition is prefixed with a !
            boolean negated = false;

            if (child instanceof PrefixExpressionVertex) {
                PrefixExpressionVertex prefixExpression = (PrefixExpressionVertex) child;
                if (prefixExpression.isOperatorNegation()) {
                    // Intentionally not handling !!
                    negated = true;
                    child = prefixExpression.getOnlyChild();
                } else {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Skipping prefix operator. vertex=" + child);
                    }
                    return;
                }
            }

            Boolean value = asTruthyBoolean(child, symbols).orElse(null);

            if (value != null) {
                if (negated) {
                    value = !value;
                }
                if (vertex instanceof StandardConditionVertex.Positive && !value) {
                    throw new PathExcludedException(this, vertex, child);
                } else if (vertex instanceof StandardConditionVertex.Negative && value) {
                    throw new PathExcludedException(this, vertex, child);
                }
            }
        }
    }

    /**
     * Converts a vertex to its truthy boolean representation if possible. Will return an empty
     * value when one or more values are indeterminant
     */
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    private Optional<Boolean> asTruthyBoolean(BaseSFVertex vertex, SymbolProvider symbols) {
        if (vertex instanceof LiteralExpressionVertex.Null) {
            // if (null)
            return Optional.of(false);
        } else if (vertex instanceof LiteralExpressionVertex.False) {
            // if (false)
            return Optional.of(false);
        } else if (vertex instanceof LiteralExpressionVertex.True) {
            // if (true)
            return Optional.of(true);
        } else if (vertex instanceof BooleanExpressionVertex) {
            // if (x == y), if (x == null)
            return asTruthyBoolean((BooleanExpressionVertex) vertex, symbols);
        } else if (vertex instanceof ChainedVertex) {
            boolean negated = false;

            if (vertex instanceof PrefixExpressionVertex) {
                PrefixExpressionVertex prefixExpression = (PrefixExpressionVertex) vertex;
                if (prefixExpression.isOperatorNegation()) {
                    // Intentionally not handling !!
                    negated = true;
                    vertex = prefixExpression.getOnlyChild();
                } else {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Skipping prefix operator. vertex=" + vertex);
                    }
                    return Optional.empty();
                }
            }

            // if (s.contains('.'))
            ApexValue<?> apexValue =
                    ScopeUtil.resolveToApexValue(symbols, (ChainedVertex) vertex).orElse(null);
            Optional<Boolean> result = asTruthyBoolean(apexValue);
            if (result.isPresent() && negated) {
                result = Optional.ofNullable(!result.get());
            }
            return result;
        }
        return Optional.empty();
    }

    private Optional<Boolean> asTruthyBoolean(@Nullable ApexValue<?> apexValue) {
        if (apexValue == null) {
            return Optional.empty();
        }

        if (apexValue.isNull()) {
            // String x = null;
            // if (x)
            return Optional.of(false);
        } else if (apexValue.isIndeterminant() && apexValue.hasEitherConstraint(Constraint.Null)) {
            // Constraints are added when an indeterminant variable is present in a
            // StandardCondition earlier in the
            // path, use this information to determine if the value is constrained to null or
            // non-null.
            // void doSomething(String x)
            // if (x)
            return Optional.of(apexValue.hasPositiveConstraint(Constraint.Null));
        } else {
            // Boolean b = true;
            // if (b)
            // DisplayType dt = DisplayType.ADDRESS
            // if (dt)
            return apexValue.asTruthyBoolean();
        }
    }

    private Optional<ApexValue<?>> asApexValue(BaseSFVertex vertex, SymbolProvider symbols) {
        if (vertex instanceof ChainedVertex) {
            return ScopeUtil.resolveToApexValue(symbols, (ChainedVertex) vertex);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Examines the right hand side(rhs) and left hand side(lhs) of a == or != comparison. Supports
     * boolean && of multiple comparisons. Any BooleanExpressionVertex that can evaluates to false
     * will cause Optional.of(false) to be returned for the entire expression, the outcome of an &&
     * is independent of the other value isfalse. True BooleanExpressionVertex expressions combined
     * with an indeterminant value will return an Optional.empty()
     */
    private Optional<Boolean> asTruthyBoolean(
            BooleanExpressionVertex vertex, SymbolProvider symbols) {
        if (vertex.isOperatorAnd()) {
            final Optional<Boolean> lhs = asTruthyBoolean(vertex.getLhs(), symbols);
            final Optional<Boolean> rhs = asTruthyBoolean(vertex.getRhs(), symbols);
            if (lhs.isPresent() && rhs.isPresent()) {
                return Optional.of(lhs.get() && rhs.get());
            } else if (lhs.isPresent() && !lhs.get()) {
                return Optional.of(false);
            } else if (rhs.isPresent() && !rhs.get()) {
                return Optional.of(false);
            }
        } else if (vertex.isOperatorOr()) {
            final Optional<Boolean> lhs = asTruthyBoolean(vertex.getLhs(), symbols);
            final Optional<Boolean> rhs = asTruthyBoolean(vertex.getRhs(), symbols);
            if (lhs.isPresent() && rhs.isPresent()) {
                return Optional.of(lhs.get() || rhs.get());
            } else if (lhs.isPresent() && lhs.get()) {
                return Optional.of(true);
            } else if (rhs.isPresent() && rhs.get()) {
                return Optional.of(true);
            }
        } else if (vertex.isOperatorEquals() || vertex.isOperatorNotEquals()) {
            final BaseSFVertex lhs = vertex.getLhs();
            final BaseSFVertex rhs = vertex.getRhs();
            final ApexValue<?> lhsApexValue = asApexValue(vertex.getLhs(), symbols).orElse(null);
            final ApexValue<?> rhsApexValue = asApexValue(vertex.getRhs(), symbols).orElse(null);
            if (lhs instanceof LiteralExpressionVertex.Null
                    || rhs instanceof LiteralExpressionVertex.Null) {
                final ApexValue<?> comparisonValue;
                if (lhs instanceof LiteralExpressionVertex.Null
                        && rhs instanceof LiteralExpressionVertex.Null) {
                    return Optional.of(vertex.isOperatorEquals());
                } else if (lhs instanceof LiteralExpressionVertex.Null) {
                    comparisonValue = rhsApexValue;
                } else if (rhs instanceof LiteralExpressionVertex.Null) {
                    comparisonValue = lhsApexValue;
                } else {
                    // TODO: Is this too paranoid? it would indicate a bug above
                    throw new UnexpectedException(vertex);
                }
                if (comparisonValue != null) {
                    if (comparisonValue.isNull()) {
                        return Optional.of(vertex.isOperatorEquals());
                    } else if (!comparisonValue.isIndeterminant()) {
                        return Optional.of(vertex.isOperatorNotEquals());
                        // Constraints are added when an indeterminant variable is present in a
                        // StandardCondition earlier in
                        // the path, use this information to determine if the value is constrained
                        // to null or non-null.
                    } else if (comparisonValue.hasPositiveConstraint(Constraint.Null)) {
                        return Optional.of(vertex.isOperatorEquals());
                    } else if (comparisonValue.hasNegativeConstraint(Constraint.Null)) {
                        return Optional.of(vertex.isOperatorNotEquals());
                    }
                }
            } else if (ApexValueUtil.isIndeterminant(lhsApexValue)
                    || ApexValueUtil.isIndeterminant(rhsApexValue)) {
                // If either side is indeterminant, then the whole expression must be indeterminant
                return Optional.empty();
            } else if (lhsApexValue instanceof ApexSimpleValue
                    && rhsApexValue instanceof ApexSimpleValue) {
                // TODO: This logic should be placed into ApexSimpleValue
                final ApexSimpleValue lhsApexSimpleValue = (ApexSimpleValue) lhsApexValue;
                final ApexSimpleValue rhsApexSimpleValue = (ApexSimpleValue) rhsApexValue;
                final Object lhsValue = lhsApexSimpleValue.getValue().orElse(null);
                final Object rhsValue = rhsApexSimpleValue.getValue().orElse(null);
                if (lhsValue != null
                        && rhsValue != null
                        && lhsValue.getClass().equals(rhsValue.getClass())) {
                    return evaluateExpression(vertex, lhsApexValue.equals(rhsApexValue));
                } else if (lhsValue != null && rhsApexSimpleValue.isNull()) {
                    // if ('foo' == null)
                    return Optional.of(false);
                } else if (rhsValue != null && lhsApexSimpleValue.isNull()) {
                    // if (null == 'foo')
                    return Optional.of(false);
                }
            } else if (lhsApexValue instanceof ApexEnumValue
                    && rhsApexValue instanceof ApexEnumValue) {
                return evaluateExpression(vertex, lhsApexValue.equals(rhsApexValue));
            }
        }
        return Optional.empty();
    }

    /** Converts {@code equals} to the correct type depending on the type of boolean comparison */
    private Optional<Boolean> evaluateExpression(BooleanExpressionVertex vertex, boolean equals) {
        if (vertex.isOperatorEquals()) {
            return Optional.of(equals);
        } else if (vertex.isOperatorNotEquals()) {
            return Optional.of(!equals);
        } else {
            throw new UnexpectedException(vertex);
        }
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final BooleanValuePathConditionExcluder INSTANCE =
                new BooleanValuePathConditionExcluder();
    }

    private BooleanValuePathConditionExcluder() {}
}
