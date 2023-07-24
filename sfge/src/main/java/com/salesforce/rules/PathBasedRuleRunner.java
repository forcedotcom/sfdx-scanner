package com.salesforce.rules;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ApexPathVertexMetaInfo;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.ops.ApexPathUtil.ApexPathRetrievalSummary;
import com.salesforce.graph.ops.expander.ApexPathExpanderConfig;
import com.salesforce.graph.ops.expander.PathExpansionException;
import com.salesforce.graph.ops.expander.PathExpansionObserver;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.rules.fls.apex.operations.FlsViolationInfo;
import com.salesforce.rules.fls.apex.operations.FlsViolationMessageUtil;
import com.salesforce.rules.multiplemassschemalookup.MultipleMassSchemaLookupInfo;
import com.salesforce.rules.ops.ProgressListener;
import com.salesforce.rules.ops.ProgressListenerProvider;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** At the lowest level, executes path-based rules */
public class PathBasedRuleRunner {
    private static final Logger LOGGER = LogManager.getLogger(PathBasedRuleRunner.class);

    private final GraphTraversalSource g;
    private final List<AbstractPathBasedRule> allRules;
    private final List<AbstractPathAnomalyRule> anomalyRules;
    private final List<AbstractPathTraversalRule> traversalRules;
    private final MethodVertex methodVertex;
    /** Set that holds the violations found by this rule execution. */
    private final Set<Violation> violations;

    private final ProgressListener progressListener;

    public PathBasedRuleRunner(
            GraphTraversalSource g, List<AbstractPathBasedRule> rules, MethodVertex methodVertex) {
        this.g = g;
        this.allRules = rules;
        this.anomalyRules = new ArrayList<>();
        this.traversalRules = new ArrayList<>();
        for (AbstractPathBasedRule rule : rules) {
            if (rule instanceof AbstractPathTraversalRule) {
                this.traversalRules.add((AbstractPathTraversalRule) rule);
            } else if (rule instanceof AbstractPathAnomalyRule) {
                this.anomalyRules.add((AbstractPathAnomalyRule) rule);
            }
        }
        this.methodVertex = methodVertex;
        this.violations = new HashSet<>();
        this.progressListener = ProgressListenerProvider.get();
    }

    /**
     * Lowest level where path-based rules are executed.
     *
     * @return a set of violations that were detected
     */
    public Set<Violation> runRules() {
        if (allRules.isEmpty()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "EntryPoint=" + methodVertex.toSimpleString() + "; No interested rules.");
            }
            // TODO: SURFACE A WARNING TO THE USER.
            return new HashSet<>();
        }

        // Build configuration to define how apex paths will be expanded
        final ApexPathExpanderConfig expanderConfig = getApexPathExpanderConfig();

        // Get all the paths that originate in the entry point
        final ApexPathRetrievalSummary pathSummary = getPathSummary(expanderConfig);

        // Execute rules on the paths rejected
        executeRulesOnAnomalies(pathSummary.getRejectionReasons());

        // Execute rules on the paths found
        executeRulesOnPaths(pathSummary.getAcceptedPaths());

        if (!violations.isEmpty()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "EntryPoint="
                                + methodVertex.toSimpleString()
                                + "; Violations="
                                + violations);
            }
        } else {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("EntryPoint=" + methodVertex.toSimpleString() + "; no violations");
            }
        }

        progressListener.finishedAnalyzingEntryPoint(pathSummary.getAcceptedPaths(), violations);

        return violations;
    }

    private void executeRulesOnAnomalies(List<PathExpansionException> anomalies) {
        // If no anomalies were reported, we should log that information and just exit.
        if (anomalies.isEmpty()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("No anomalies found while expanding paths");
            }
            return;
        }
        for (AbstractPathAnomalyRule rule : anomalyRules) {
            List<RuleThrowable> ruleThrowables = rule.run(g, methodVertex, anomalies);
            for (RuleThrowable ruleThrowable : ruleThrowables) {
                // TODO: This is fine for now, because there are no anomaly rules that return
                //       incomplete RuleThrowables instead of violations. But if that ever
                //       changes, it may be desirable to refactor this method and
                //       `executeRulesOnPaths()` to use a common method that processes
                //       throwables. At the very least, an else-branch will be required here.
                if (ruleThrowable instanceof Violation.RuleViolation) {
                    Violation.RuleViolation violation = (Violation.RuleViolation) ruleThrowable;
                    violation.setPropertiesFromRule(rule);
                    violations.add(violation);
                }
            }
        }
    }

    private void executeRulesOnPaths(List<ApexPath> paths) {
        boolean foundVertex = false;
        // This set holds the violations whose information couldn't be fully processed at creation
        // time, and
        // require post-processing.
        final HashSet<FlsViolationInfo> flsViolationInfos = new HashSet<>();
        final HashSet<MultipleMassSchemaLookupInfo> mmsLookupInfos = new HashSet<>();
        // For each path...
        for (ApexPath path : paths) {
            // If the path's metadata is present...
            if (path.getApexPathMetaInfo().isPresent()) {
                // Iterate over all vertices in the path...
                for (ApexPathVertexMetaInfo.PredicateMatch predicateMatch :
                        path.getApexPathMetaInfo().get().getAllMatches()) {
                    AbstractPathTraversalRule rule =
                            (AbstractPathTraversalRule) predicateMatch.getPredicate();
                    BaseSFVertex vertex = predicateMatch.getPathVertex().getVertex();
                    List<RuleThrowable> ruleThrowables = rule.run(g, path, vertex);
                    for (RuleThrowable ruleThrowable : ruleThrowables) {
                        // If the throwable is a Rule Violation, then it needs to be completed with
                        // a reference to the
                        // rule that caused it.
                        if (ruleThrowable instanceof Violation.RuleViolation) {
                            ((Violation.RuleViolation) ruleThrowable).setPropertiesFromRule(rule);
                        }
                        // If the throwable represents an incomplete violation, it needs to be added
                        // to the list of such
                        // objects.
                        if (ruleThrowable instanceof FlsViolationInfo) {
                            flsViolationInfos.add((FlsViolationInfo) ruleThrowable);
                        } else if (ruleThrowable instanceof MultipleMassSchemaLookupInfo) {
                            // FIXME: PR incoming with refactors to this portion
                            mmsLookupInfos.add((MultipleMassSchemaLookupInfo) ruleThrowable);
                        } else if (ruleThrowable instanceof Violation) {
                            // If the violation is done, it can just go directly into the results
                            // set.
                            violations.add((Violation) ruleThrowable);
                        }
                    }

                    foundVertex = true;
                }
            }
        }

        convertFlsInfoToViolations(flsViolationInfos);
        convertMmsInfoToViolations(mmsLookupInfos);

        if (!foundVertex) {
            // If no vertices were found, we should log something so that information isn't lost,
            // but don't synthesize
            // a violation or anything like that.
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("No vertices of interest found in eligible paths");
            }
        }
    }

    private void convertMmsInfoToViolations(HashSet<MultipleMassSchemaLookupInfo> mmsLookupInfos) {
        for (MultipleMassSchemaLookupInfo mmsLookupInfo : mmsLookupInfos) {
            Violation.RuleViolation violation = mmsLookupInfo.convert();
            violation.setPropertiesFromRule(AvoidMultipleMassSchemaLookups.getInstance());
            violations.add(violation);
        }
    }

    // TODO: Restructure to make this logic work on other types of violation info too
    private void convertFlsInfoToViolations(HashSet<FlsViolationInfo> flsViolationInfos) {
        // Consolidate/regroup FLS violations across paths so that there are no
        // duplicates with different field sets for the same source/sink/dmlOperation
        final HashSet<FlsViolationInfo> consolidatedFlsViolationInfos =
                FlsViolationMessageUtil.consolidateFlsViolations(flsViolationInfos);

        for (FlsViolationInfo flsViolationInfo : consolidatedFlsViolationInfos) {
            final String violationMessage =
                    FlsViolationMessageUtil.constructMessage(flsViolationInfo);
            final Optional<SFVertex> sourceVertex = flsViolationInfo.getSourceVertex();
            final Optional<SFVertex> sinkVertex = flsViolationInfo.getSinkVertex();
            if (sinkVertex.isPresent()) {
                final Violation.RuleViolation ruleViolation =
                        new Violation.PathBasedRuleViolation(
                                violationMessage, sourceVertex.get(), sinkVertex.get());
                flsViolationInfo
                        .getRule()
                        .ifPresent(rule -> ruleViolation.setPropertiesFromRule(rule));
                violations.add(ruleViolation);
            } else {
                throw new UnexpectedException(
                        "Developer error: Sink vertex not set in flsViolationInfo: "
                                + flsViolationInfo);
            }
        }
    }

    private ApexPathRetrievalSummary getPathSummary(ApexPathExpanderConfig expanderConfig) {
        return ApexPathUtil.summarizeForwardPaths(g, methodVertex, expanderConfig);
    }

    private ApexPathExpanderConfig getApexPathExpanderConfig() {
        ApexPathExpanderConfig.Builder expanderConfigBuilder =
                ApexPathUtil.getFullConfiguredPathExpanderConfigBuilder();
        for (AbstractPathBasedRule rule : allRules) {
            Optional<PathExpansionObserver> observerOptional = rule.getPathExpansionObserver();
            if (observerOptional.isPresent()) {
                expanderConfigBuilder =
                        expanderConfigBuilder.withPathExpansionObserver(observerOptional.get());
            }
        }
        for (AbstractPathTraversalRule rule : traversalRules) {
            expanderConfigBuilder = expanderConfigBuilder.withVertexPredicate(rule);
        }
        ApexPathExpanderConfig expanderConfig = expanderConfigBuilder.build();
        return expanderConfig;
    }
}
