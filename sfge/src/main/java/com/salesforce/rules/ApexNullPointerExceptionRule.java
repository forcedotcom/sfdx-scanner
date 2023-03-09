package com.salesforce.rules;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.ops.expander.NullValueAccessedException;
import com.salesforce.graph.ops.expander.PathExpansionException;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** */
public final class ApexNullPointerExceptionRule extends AbstractPathAnomalyRule {
    private static final String URL =
            "https://forcedotcom.github.io./sfdx-scanner/en/v3.x/salesforce-graph-engine/rules/#ApexNullPointerExceptionRule";

    private ApexNullPointerExceptionRule() {
        super();
    }

    public static ApexNullPointerExceptionRule getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    public List<RuleThrowable> run(
            GraphTraversalSource g,
            MethodVertex methodVertex,
            List<PathExpansionException> anomalies) {
        List<RuleThrowable> violations = new ArrayList<>();
        // Use a Set to track each vertex that causes a violation, so we can avoid duplicates.
        Set<Long> vertexIds = new HashSet<>();
        for (PathExpansionException anomaly : anomalies) {
            if (!(anomaly instanceof NullValueAccessedException)) {
                continue;
            }
            MethodCallExpressionVertex mcev = ((NullValueAccessedException) anomaly).getVertex();
            if (vertexIds.contains(mcev.getId())) {
                continue;
            }
            violations.add(
                    new Violation.PathBasedRuleViolation(
                            String.format(
                                    UserFacingMessages.RuleViolationTemplates
                                            .APEX_NULL_POINTER_EXCEPTION_RULE,
                                    mcev.getFullMethodName()),
                            methodVertex,
                            mcev));
            vertexIds.add(mcev.getId());
        }
        return violations;
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    protected int getSeverity() {
        return SEVERITY.MODERATE.code;
    }

    @Override
    protected String getDescription() {
        return UserFacingMessages.RuleDescriptions.APEX_NULL_POINTER_EXCEPTION_RULE;
    }

    @Override
    protected String getCategory() {
        return CATEGORY.ERROR_PRONE.name;
    }

    @Override
    protected String getUrl() {
        return URL;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use.
        private static final ApexNullPointerExceptionRule INSTANCE =
                new ApexNullPointerExceptionRule();
    }
}
