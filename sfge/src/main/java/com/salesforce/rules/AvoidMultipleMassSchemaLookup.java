package com.salesforce.rules;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.rules.getglobaldescribe.MassSchemaLookupViolationInfo;
import com.salesforce.rules.getglobaldescribe.AvoidMultipleMassSchemaLookupHandler;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Rule to detect possible performance degradations while invoking Schema.getGlobalDescribe().
 */
public class AvoidMultipleMassSchemaLookup extends AbstractPathTraversalRule {

    private final AvoidMultipleMassSchemaLookupHandler ruleHandler;

    private AvoidMultipleMassSchemaLookup() {
        ruleHandler = AvoidMultipleMassSchemaLookupHandler.getInstance();
    }

    @Override
    public boolean test(BaseSFVertex vertex) {
        return ruleHandler.test(vertex);
    }

    @Override
    protected List<RuleThrowable> _run(GraphTraversalSource g, ApexPath path, BaseSFVertex vertex) {
        MassSchemaLookupViolationInfo massSchemaLookupViolationInfos = ruleHandler.detectViolations(g, path, vertex);
        List<RuleThrowable> violations = new ArrayList<>();
        // TODO: do further processing here
        violations.add(massSchemaLookupViolationInfos);
        return violations;
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

    @Override
    protected boolean isEnabled() {
        return false;
    }

    public static AvoidMultipleMassSchemaLookup getInstance() {
        return AvoidMultipleMassSchemaLookup.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final AvoidMultipleMassSchemaLookup INSTANCE = new AvoidMultipleMassSchemaLookup();
    }
}
