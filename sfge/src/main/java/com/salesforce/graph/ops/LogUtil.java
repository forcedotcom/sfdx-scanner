package com.salesforce.graph.ops;

import java.util.UUID;
import org.apache.logging.log4j.CloseableThreadContext;

public final class LogUtil {
    private static final String RULE_RUN_ID = "ruleRunId";

    public static CloseableThreadContext.Instance startRuleRun() {
        String uuid = UUID.randomUUID().toString();
        return CloseableThreadContext.put(RULE_RUN_ID, uuid);
    }

    private LogUtil() {}
}
