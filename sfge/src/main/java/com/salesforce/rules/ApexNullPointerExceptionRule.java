package com.salesforce.rules;

import com.google.common.collect.ImmutableSet;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.ops.expander.NullValueAccessedException;
import com.salesforce.graph.ops.expander.PathExpansionException;
import com.salesforce.graph.source.ApexPathSource.Type;
import com.salesforce.graph.vertex.*;
import java.util.*;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Rule for detecting NPEs. Operates by checking paths that were rejected from analysis, and seeing
 * which ones were rejected because they terminated in an NPE.
 */
public final class ApexNullPointerExceptionRule extends AbstractPathAnomalyRule {
    private static final String URL =
            "https://forcedotcom.github.io./sfdx-scanner/en/v3.x/salesforce-graph-engine/rules/#ApexNullPointerExceptionRule";
    // ApexNullPointerExceptionRule cares about all sources, since they're all equally capable
    // of throwing NPEs.
    private static final ImmutableSet<Type> SOURCE_TYPES = ImmutableSet.copyOf(Type.values());

    private ApexNullPointerExceptionRule() {
        super();
    }

    public static ApexNullPointerExceptionRule getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    public ImmutableSet<Type> getSourceTypes() {
        return SOURCE_TYPES;
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
            final NullAccessCheckedVertex affectedVertex =
                    ((NullValueAccessedException) anomaly).getVertex();
            if (vertexIds.contains(affectedVertex.getId())) {
                continue;
            }
            final String operationName = affectedVertex.getDisplayName();
            violations.add(
                    new Violation.PathBasedRuleViolation(
                            String.format(
                                    UserFacingMessages.RuleViolationTemplates
                                            .APEX_NULL_POINTER_EXCEPTION_RULE,
                                    operationName),
                            methodVertex,
                            (SFVertex) affectedVertex));
            vertexIds.add(affectedVertex.getId());
        }
        return violations;
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    protected boolean isPilot() {
        return false;
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
