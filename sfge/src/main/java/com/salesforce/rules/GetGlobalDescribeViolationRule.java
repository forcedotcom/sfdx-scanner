package com.salesforce.rules;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.rules.getglobaldescribe.GetGlobalDescribeViolationRuleHandler;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GetGlobalDescribeViolationRule extends AbstractPathTraversalRule {

    private final GetGlobalDescribeViolationRuleHandler ruleHandler;

    public GetGlobalDescribeViolationRule() {
        ruleHandler = GetGlobalDescribeViolationRuleHandler.getInstance();
    }

    @Override
    public boolean test(BaseSFVertex vertex) {
        return ruleHandler.test(vertex);
    }

    @Override
    protected List<RuleThrowable> _run(GraphTraversalSource g, ApexPath path, BaseSFVertex vertex) {
        final Set<Violation> violations = ruleHandler.detectViolations(g, path, vertex);
        return violations.stream().collect(Collectors.toList());
    }

    @Override
    protected int getSeverity() {
        return SEVERITY.HIGH.code;
    }

    @Override
    protected String getDescription() {
        return UserFacingMessages.RuleDescriptions.GET_GLOBAL_DESCRIBE_VIOLATION_RULE;
    }

    @Override
    protected String getCategory() {
        return CATEGORY.PERFORMANCE.name;
    }

    public static GetGlobalDescribeViolationRule getInstance() {
        return GetGlobalDescribeViolationRule.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final GetGlobalDescribeViolationRule INSTANCE = new GetGlobalDescribeViolationRule();
    }
}
