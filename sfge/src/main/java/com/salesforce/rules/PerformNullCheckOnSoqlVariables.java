package com.salesforce.rules;

import com.google.common.collect.ImmutableSet;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.exception.ProgrammingException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.source.ApexPathSource;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.Constraint;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.graph.vertex.SoqlExpressionVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.*;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public final class PerformNullCheckOnSoqlVariables extends AbstractPathTraversalRule {

    private static final ImmutableSet<ApexPathSource.Type> SOURCE_TYPES =
            ImmutableSet.copyOf(ApexPathSource.Type.values());

    private static final String URL =
            "https://forcedotcom.github.io/sfdx-scanner/en/v3.x/salesforce-graph-engine/rules/#PerformNullCheckOnSoqlVariables";

    // don't instantiate
    private PerformNullCheckOnSoqlVariables() {}

    @Override
    public ImmutableSet<ApexPathSource.Type> getSourceTypes() {
        return SOURCE_TYPES;
    }

    @Override
    protected int getSeverity() {
        return SEVERITY.MODERATE.code;
    }

    @Override
    protected String getDescription() {
        return UserFacingMessages.RuleDescriptions.PERFORM_NULL_CHECK_ON_SOQL_VARIABLE;
    }

    @Override
    protected String getCategory() {
        return CATEGORY.PERFORMANCE.name;
    }

    @Override
    protected String getUrl() {
        return URL;
    }

    public static PerformNullCheckOnSoqlVariables getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    /**
     * Tests a vertex using a symbol provider to check if it violates this rule.
     *
     * @param vertex the vertex to check for this rule
     * @param symbols a {@link SymbolProvider} that can provide symbols for the given vertex
     * @return true if, according to the symbol provider, the vertex is equal to null, constrained
     *     to be null, or has an indeterminate value
     */
    @Override
    public boolean test(BaseSFVertex vertex, SymbolProvider symbols) {

        // We only check for SOQL expressions with variables as grandchildren.
        // No need to check for MethodCallExpressionVertex because the only relevant method is
        // Database.queryWithBinds, which already does null checks for the keys and
        // values in its bind variables.
        if (!(vertex instanceof VariableExpressionVertex)
                || !(vertex.getParent().getParent() instanceof SoqlExpressionVertex)) {
            return false;
        }

        VariableExpressionVertex variableExpressionVertex = (VariableExpressionVertex) vertex;

        // try and find the value of this vertex
        Optional<ApexValue<?>> variableValueOpt =
                ScopeUtil.resolveToApexValue(symbols, variableExpressionVertex);
        if (variableValueOpt.isPresent()) {
            ApexValue<?> variableValue = variableValueOpt.get();

            if (variableValue.hasNegativeConstraint(Constraint.Null)) {
                // negative constraint means the value is guaranteed NOT to be null,
                // so this deserves no violation
                return false;
            } else if (variableValue.isNull() || variableValue.isIndeterminant()) {
                // if the variable is null (is actually null or has positive null constraint) OR the
                // value is indeterminant, this deserves a violation
                return true;
            }

        } else {
            throw new UnexpectedException(
                    "PerformNullCheckOnSoqlVariables couldn't find an apex value associated with variable vertex "
                            + vertex);
        }
        return false;
    }

    @Override
    protected List<RuleThrowable> _run(
            GraphTraversalSource g, ApexPath path, BaseSFVertex sinkVertex) {
        if (!(sinkVertex instanceof VariableExpressionVertex)) {
            throw new ProgrammingException(
                    "PerformNullCheckOnSoqlVariables rule can only be applied to VariableExpressionVertex sink vertex. Provided sink vertex="
                            + sinkVertex);
        }

        VariableExpressionVertex vertex = (VariableExpressionVertex) sinkVertex;
        final SFVertex sourceVertex = path.getMethodVertex().orElse(null);

        // Note: this rule runs differently. The test(BaseSFVertex, SymbolProvider) method
        // above only accepts vertices that are confirmed to be violations. So, this run method just
        // needs to actually create and return the violation.
        return Collections.singletonList(
                new Violation.PathBasedRuleViolation(
                        String.format(
                                UserFacingMessages.PerformNullCheckOnSoqlVariablesTemplates
                                        .MESSAGE_TEMPLATE,
                                vertex.getFullName()),
                        sourceVertex,
                        sinkVertex));
    }

    // lazy holder
    private static final class LazyHolder {
        // postpone initialization until after first use
        private static final PerformNullCheckOnSoqlVariables INSTANCE =
                new PerformNullCheckOnSoqlVariables();
    }
}
