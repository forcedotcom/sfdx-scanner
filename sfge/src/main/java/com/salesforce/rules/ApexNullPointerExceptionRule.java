package com.salesforce.rules;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.vertex.BaseSFVertex;
import java.util.ArrayList;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

// TODO: Long-term, this class will extend a different abstract class
public final class ApexNullPointerExceptionRule extends AbstractPathBasedRule {
    private static final String URL =
            "https://forcedotcom.github.io./sfdx-scanner/en/v3.x/salesforce-graph-engine/rules/#ApexNullPointerExceptionRule";

    private ApexNullPointerExceptionRule() {
        super();
    }

    public static ApexNullPointerExceptionRule getInstance() {
        return LazyHolder.INSTANCE;
    }

    // TODO: Long-term, this method is unnecessary.
    @Override
    public boolean test(BaseSFVertex vertex) {
        return false;
    }

    // TODO: Long-term, this method is unnecessary.
    @Override
    protected List<RuleThrowable> _run(GraphTraversalSource g, ApexPath path, BaseSFVertex vertex) {
        return new ArrayList<>();
    }

    // TODO: ENABLE THIS RULE.
    @Override
    protected boolean isEnabled() {
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
