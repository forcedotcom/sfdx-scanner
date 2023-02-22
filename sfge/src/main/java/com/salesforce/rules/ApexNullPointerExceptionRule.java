package com.salesforce.rules;

import com.salesforce.config.UserFacingMessages;

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
