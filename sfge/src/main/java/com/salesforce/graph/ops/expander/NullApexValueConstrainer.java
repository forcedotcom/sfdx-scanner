package com.salesforce.graph.ops.expander;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.ObjectProperties;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.Constraint;
import com.salesforce.graph.symbols.apex.ValueStatus;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BooleanExpressionVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles single case if conditions such as if (x == null) for indeterminant values and adds a
 * constraint to x indicating which path was taken in the first if/else statement.
 *
 * <p>Important: This the only place that an ApexValue is modified outside of the SymbolProvider.
 * The rationale is that the information added is only used by the ApexPathExpander and not the
 * ApexPathWalker. This code should be moved elsewhere if rules also need access to this information
 * in order to make decisions.
 */
public class NullApexValueConstrainer implements ApexValueConstrainer {
    private static final Logger LOGGER = LogManager.getLogger(NullApexValueConstrainer.class);

    @Override
    public void constrain(StandardConditionVertex vertex, SymbolProvider symbols) {
        if (!(vertex instanceof StandardConditionVertex.Positive)
                && !(vertex instanceof StandardConditionVertex.Negative)) {
            throw new UnexpectedException(vertex);
        }

        List<BaseSFVertex> children = vertex.getChildren();
        if (children.size() == 1) {
            BaseSFVertex child = children.get(0);

            if (child instanceof BooleanExpressionVertex) {
                constrainBooleanExpression(vertex, (BooleanExpressionVertex) child, symbols);
            } else if (child instanceof VariableExpressionVertex) {
                constrainVariableExpression(vertex, (VariableExpressionVertex) child, symbols);
            }
        }
    }

    private void constrainBooleanExpression(
            StandardConditionVertex vertex,
            BooleanExpressionVertex booleanExpression,
            SymbolProvider symbols) {
        if (booleanExpression.isOperatorEquals() || booleanExpression.isOperatorNotEquals()) {
            BaseSFVertex lhs = booleanExpression.getLhs();
            BaseSFVertex rhs = booleanExpression.getRhs();
            VariableExpressionVertex comparisonVertex;
            if (lhs instanceof LiteralExpressionVertex.Null
                    && rhs instanceof VariableExpressionVertex) {
                comparisonVertex = (VariableExpressionVertex) rhs;
            } else if (lhs instanceof VariableExpressionVertex
                    && rhs instanceof LiteralExpressionVertex.Null) {
                comparisonVertex = (VariableExpressionVertex) lhs;
            } else {
                return;
            }

            // Add an indeterminant ApexValue to any ApexValues that implement the ObjectProperties
            // interface when the
            // property request is not found on the object. This helps in situations such as
            // if (myObj.TheField__c == null) {
            // } else {
            // }
            // This code will add a new indeterminant property with a Null constraint named
            // 'TheField__c' to the ApexValue
            // represented by 'myObj'
            ApexValue<?> apexValue = symbols.getApexValue(comparisonVertex).orElse(null);
            if (apexValue == null) {
                List<String> chainedNames = comparisonVertex.getSymbolicNameChain();
                if (chainedNames.size() == 2) {
                    apexValue = symbols.getApexValue(chainedNames.get(0)).orElse(null);
                    if (apexValue instanceof ObjectProperties) {
                        ObjectProperties objectProperties = (ObjectProperties) apexValue;
                        ApexStringValue key =
                                ApexValueBuilder.getWithoutSymbolProvider()
                                        .buildString(chainedNames.get(1));
                        apexValue =
                                ApexValueBuilder.getWithoutSymbolProvider()
                                        .withStatus(ValueStatus.INDETERMINANT)
                                        .buildUnknownType();
                        objectProperties.putConstrainedApexValue(key, apexValue);
                    }
                }
            }

            if (apexValue != null && apexValue.isIndeterminant() && !apexValue.isNull()) {
                boolean positiveConstraint;
                if (vertex instanceof StandardConditionVertex.Positive) {
                    positiveConstraint = booleanExpression.isOperatorEquals();
                } else if (vertex instanceof StandardConditionVertex.Negative) {
                    positiveConstraint = !booleanExpression.isOperatorEquals();
                } else {
                    throw new UnexpectedException("Impossible");
                }
                if (positiveConstraint) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Adding positive constraint. apeValue=" + apexValue);
                    }
                    apexValue.addPositiveConstraint(Constraint.Null);
                } else {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Adding negative constraint. apeValue=" + apexValue);
                    }
                    apexValue.addNegativeConstraint(Constraint.Null);
                }
            }
        }
    }

    private void constrainVariableExpression(
            StandardConditionVertex vertex,
            VariableExpressionVertex variableExpressionVertex,
            SymbolProvider symbols) {
        ApexValue<?> apexValue = symbols.getApexValue(variableExpressionVertex).orElse(null);
        if (apexValue != null && apexValue.isIndeterminant() && !apexValue.isNull()) {
            if (vertex instanceof StandardConditionVertex.Positive) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Adding positive constraint. apeValue=" + apexValue);
                }
                apexValue.addPositiveConstraint(Constraint.Null);
            } else {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Adding negative constraint. apeValue=" + apexValue);
                }
                apexValue.addNegativeConstraint(Constraint.Null);
            }
        }
    }

    public static NullApexValueConstrainer getInstance() {
        return NullApexValueConstrainer.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final NullApexValueConstrainer INSTANCE = new NullApexValueConstrainer();
    }

    private NullApexValueConstrainer() {}
}
