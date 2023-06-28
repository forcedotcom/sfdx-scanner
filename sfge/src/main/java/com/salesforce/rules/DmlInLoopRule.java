package com.salesforce.rules;

import com.google.common.collect.ImmutableSet;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.source.ApexPathSource;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.rules.dmlinloop.DmlInLoopRuleHandler;
import java.util.ArrayList;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class DmlInLoopRule extends AbstractPathTraversalRule {

    private static final ImmutableSet<ApexPathSource.Type> SOURCE_TYPES =
            ImmutableSet.copyOf(ApexPathSource.Type.values());

    private final DmlInLoopRuleHandler ruleHandler;

    private DmlInLoopRule() {
        ruleHandler = DmlInLoopRuleHandler.getInstance();
    }

    @Override
    public boolean test(BaseSFVertex vertex) {
        return ruleHandler.test(vertex);
    }

    @Override
    protected List<RuleThrowable> _run(GraphTraversalSource g, ApexPath path, BaseSFVertex vertex) {
        List<RuleThrowable> violations = new ArrayList<>();

        violations.addAll(ruleHandler.detectViolations(g, path, vertex));

        return violations;
    }

    @Override
    public ImmutableSet<ApexPathSource.Type> getSourceTypes() {
        return SOURCE_TYPES;
    }

    // TODO confirm severity of this rule
    @Override
    protected int getSeverity() {
        return SEVERITY.HIGH.code;
    }

    @Override
    protected String getDescription() {
        return UserFacingMessages.RuleDescriptions.DML_IN_LOOP_RULE;
    }

    @Override
    protected String getCategory() {
        return CATEGORY.PERFORMANCE.name;
    }

    @Override
    protected boolean isEnabled() {
        return false;
    }

    public static DmlInLoopRule getInstance() {
        return DmlInLoopRule.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // postpone initialization until after first use
        private static final DmlInLoopRule INSTANCE = new DmlInLoopRule();
    }
}
