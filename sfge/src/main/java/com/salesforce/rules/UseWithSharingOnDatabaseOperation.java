package com.salesforce.rules;

import com.google.common.collect.ImmutableSet;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.source.ApexPathSource;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.rules.usewithsharingondatabaseoperation.UseWithSharingOnDatabaseOperationHandler;
import java.util.ArrayList;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public final class UseWithSharingOnDatabaseOperation extends AbstractPathTraversalRule {

    private static final String URL =
            "https://forcedotcom.github.io/sfdx-scanner/en/v3.x/salesforce-graph-engine/rules/#UseWithSharingOnDatabaseOperation";

    private static final ImmutableSet<ApexPathSource.Type> SOURCE_TYPES =
            ImmutableSet.copyOf(ApexPathSource.Type.values());

    private final UseWithSharingOnDatabaseOperationHandler ruleHandler;

    private UseWithSharingOnDatabaseOperation() {
        ruleHandler = UseWithSharingOnDatabaseOperationHandler.getInstance();
    }

    @Override
    public boolean test(BaseSFVertex vertex) {
        return ruleHandler.test(vertex);
    }

    @Override
    protected List<RuleThrowable> _run(GraphTraversalSource g, ApexPath path, BaseSFVertex vertex) {
        List<RuleThrowable> violations =
                new ArrayList<>(ruleHandler.detectViolations(g, path, vertex));
        return violations;
    }

    @Override
    public boolean isEnabled() {
        return true;
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
        return UserFacingMessages.RuleDescriptions.SHARING_RULE;
    }

    @Override
    protected String getCategory() {
        return CATEGORY.SECURITY.name;
    }

    @Override
    protected String getUrl() {
        return URL;
    }

    // lazy holder

    public static UseWithSharingOnDatabaseOperation getInstance() {
        return UseWithSharingOnDatabaseOperation.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // postpone initialization until after first use
        private static final UseWithSharingOnDatabaseOperation INSTANCE =
                new UseWithSharingOnDatabaseOperation();
    }
}
