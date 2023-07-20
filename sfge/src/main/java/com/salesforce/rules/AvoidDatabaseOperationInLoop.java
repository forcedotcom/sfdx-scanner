package com.salesforce.rules;

import com.google.common.collect.ImmutableSet;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.source.ApexPathSource;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.rules.avoiddatabaseoperationinloop.AvoidDatabaseOperationInLoopHandler;
import java.util.ArrayList;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public final class AvoidDatabaseOperationInLoop extends AbstractPathTraversalRule {

    private static final ImmutableSet<ApexPathSource.Type> SOURCE_TYPES =
            ImmutableSet.copyOf(ApexPathSource.Type.values());

    private final AvoidDatabaseOperationInLoopHandler ruleHandler;

    private AvoidDatabaseOperationInLoop() {
        ruleHandler = AvoidDatabaseOperationInLoopHandler.getInstance();
    }

    /** check if a certain vertex is of interest to this rule */
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
        return SEVERITY.MODERATE.code;
    }

    @Override
    protected String getDescription() {
        return UserFacingMessages.RuleDescriptions.AVOID_DATABASE_OPERATION_IN_LOOP;
    }

    @Override
    protected String getCategory() {
        return CATEGORY.PERFORMANCE.name;
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    public static AvoidDatabaseOperationInLoop getInstance() {
        return AvoidDatabaseOperationInLoop.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // postpone initialization until after first use
        private static final AvoidDatabaseOperationInLoop INSTANCE =
                new AvoidDatabaseOperationInLoop();
    }
}
