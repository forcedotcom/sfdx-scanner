package com.salesforce.rules;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SoqlExpressionVertex;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.rules.fls.apex.ReadFlsRuleVisitor;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.rules.fls.apex.operations.FlsValidationCentral;
import com.salesforce.rules.fls.apex.operations.FlsViolationInfo;
import java.util.HashSet;
import java.util.Set;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Rule to detect missing CRUD/FLS checks for Database Read operations. Uses path-based approach and
 * moves only in forward direction. This class is utilized by {@link ApexFlsViolationRule} to detect
 * FLS violations in Read operations.
 */
final class ApexFlsReadRuleHandler implements FlsRuleHandler {
    private static final FlsConstants.FlsValidationType VALIDATION_TYPE =
            FlsConstants.FlsValidationType.READ;
    private static final String DATABASE_READ_METHOD_NAME = VALIDATION_TYPE.databaseOperationMethod;

    @Override
    public Set<FlsViolationInfo> detectViolations(
            GraphTraversalSource g, ApexPath path, BaseSFVertex vertex) {
        final Set<FlsViolationInfo> violations = new HashSet<>();
        final FlsValidationCentral validationCentral = new FlsValidationCentral(VALIDATION_TYPE);
        final ReadFlsRuleVisitor ruleVisitor =
                new ReadFlsRuleVisitor(VALIDATION_TYPE, validationCentral);
        ruleVisitor.setTargetVertex(vertex);

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, ruleVisitor, symbols);

        validationCentral.tallyValidations(vertex);
        violations.addAll(validationCentral.getViolations());
        return violations;
    }

    @Override
    public boolean test(BaseSFVertex vertex) {
        return isDatabaseReadMethod(vertex) || isSoqlStatement(vertex);
    }

    private boolean isDatabaseReadMethod(BaseSFVertex vertex) {
        return vertex instanceof MethodCallExpressionVertex
                && DATABASE_READ_METHOD_NAME.equalsIgnoreCase(
                        ((MethodCallExpressionVertex) vertex).getFullMethodName());
    }

    private boolean isSoqlStatement(BaseSFVertex vertex) {
        return vertex instanceof SoqlExpressionVertex;
    }

    static ApexFlsReadRuleHandler getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final ApexFlsReadRuleHandler INSTANCE = new ApexFlsReadRuleHandler();
    }

    private ApexFlsReadRuleHandler() {}
}
