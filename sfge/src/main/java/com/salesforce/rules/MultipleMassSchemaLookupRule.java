package com.salesforce.rules;

import com.google.common.collect.ImmutableSet;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.source.ApexPathSource;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.rules.multiplemassschemalookup.MultipleMassSchemaLookupRuleHandler;
import java.util.ArrayList;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** Rule to detect possible performance degradations while invoking Schema.getGlobalDescribe(). */
public class MultipleMassSchemaLookupRule extends AbstractPathTraversalRule {
    // Any path is capable of violating this rule.
    private static final ImmutableSet<ApexPathSource.Type> SOURCE_TYPES =
            ImmutableSet.copyOf(ApexPathSource.Type.values());

    private final MultipleMassSchemaLookupRuleHandler ruleHandler;

    private MultipleMassSchemaLookupRule() {
        ruleHandler = MultipleMassSchemaLookupRuleHandler.getInstance();
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

    @Override
    protected int getSeverity() {
        return SEVERITY.HIGH.code;
    }

    @Override
    protected String getDescription() {
        return UserFacingMessages.RuleDescriptions.MULTIPLE_MASS_SCHEMA_LOOKUP_RULE;
    }

    @Override
    protected String getCategory() {
        return CATEGORY.PERFORMANCE.name;
    }

    @Override
    protected boolean isEnabled() {
        return false;
    }

    public static MultipleMassSchemaLookupRule getInstance() {
        return MultipleMassSchemaLookupRule.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final MultipleMassSchemaLookupRule INSTANCE =
                new MultipleMassSchemaLookupRule();
    }
}
