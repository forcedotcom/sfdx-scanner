package com.salesforce.rules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.source.ApexPathSource.Type;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.rules.fls.apex.operations.FlsViolationInfo;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * FLS Violation rule that uses forward-path approach to detect missing FLS checks on CRUD
 * operations.
 */
public final class ApexFlsViolationRule extends AbstractPathTraversalRule {
    private static final Logger LOGGER = LogManager.getLogger(ApexFlsViolationRule.class);
    // ApexFlsViolationRule only cares about sources that don't intrinsically respect CRUD/FLS.
    private static final ImmutableSet<Type> SOURCE_TYPES =
            ImmutableSet.of(
                    Type.ANNOTATION_AURA_ENABLED,
                    Type.ANNOTATION_INVOCABLE_METHOD,
                    Type.ANNOTATION_NAMESPACE_ACCESSIBLE,
                    Type.ANNOTATION_REMOTE_ACTION,
                    Type.EXPOSED_CONTROLLER_METHOD,
                    Type.GLOBAL_METHOD,
                    Type.INBOUND_EMAIL_HANDLER,
                    Type.PAGE_REFERENCE);

    /**
     * This isn't private, so it can be used as the value for InternalErrorViolation and
     * TimeoutViolation URL fields. If those ever start being unique, this can be made private
     * again.
     */
    static final String URL =
            "https://developer.salesforce.com/docs/platform/salesforce-code-analyzer/guide/apexflsviolation-rule.html";

    private static final String DESCRIPTION =
            "Identifies data read/write operations that may not have CRUD/FLS";
    private final List<FlsRuleHandler> ruleHandlers;

    private ApexFlsViolationRule() {
        ruleHandlers =
                ImmutableList.of(
                        ApexFlsReadRuleHandler.getInstance(),
                        ApexFlsWriteRuleHandler.getInstance());
    }

    @Override
    public ImmutableSet<Type> getSourceTypes() {
        return SOURCE_TYPES;
    }

    protected int getSeverity() {
        return SEVERITY.HIGH.code;
    }

    protected String getDescription() {
        return DESCRIPTION;
    }

    protected String getCategory() {
        return CATEGORY.SECURITY.name;
    }

    protected String getUrl() {
        return URL;
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
    protected List<RuleThrowable> _run(GraphTraversalSource g, ApexPath path, BaseSFVertex vertex) {
        final HashSet<FlsViolationInfo> flsViolationInfos = new HashSet<>();

        ruleHandlers.forEach(
                ruleHandler -> {
                    if (ruleHandler.test(vertex)) {
                        flsViolationInfos.addAll(ruleHandler.detectViolations(g, path, vertex));
                    }
                });

        flsViolationInfos.forEach(
                violation -> {
                    violation.setSourceVertex(path.getMethodVertex().orElse(null));
                    violation.setRule(this);
                });

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "Results. "
                            + "source="
                            + path.getMethodVertex()
                                    .orElseThrow(() -> new UnexpectedException(path))
                                    .toSimpleString()
                            + ", sink="
                            + vertex
                            + ", results="
                            + flsViolationInfos);
        }
        return new ArrayList<>(flsViolationInfos);
    }

    @Override
    public boolean test(BaseSFVertex vertex, SymbolProvider provider) {
        // Return true when any rule handler is interested in this vertex
        return ruleHandlers.stream().anyMatch(ruleHandler -> ruleHandler.test(vertex));
    }

    public static ApexFlsViolationRule getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final ApexFlsViolationRule INSTANCE = new ApexFlsViolationRule();
    }
}
