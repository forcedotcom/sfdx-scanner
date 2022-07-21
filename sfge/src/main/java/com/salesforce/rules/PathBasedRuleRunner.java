package com.salesforce.rules;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ApexPathVertexMetaInfo;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.ops.expander.ApexPathExpanderConfig;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.rules.fls.apex.operations.FlsViolationInfo;
import com.salesforce.rules.fls.apex.operations.FlsViolationMessageUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** At the lowest level, executes path-based rules */
public class PathBasedRuleRunner {
    private static final Logger LOGGER = LogManager.getLogger(PathBasedRuleRunner.class);

    private final GraphTraversalSource g;
    private final List<AbstractPathBasedRule> rules;
    private final MethodVertex methodVertex;
    /** Set that holds the violations found by this rule execution. */
    private final Set<Violation> violations;

    public PathBasedRuleRunner(
            GraphTraversalSource g, List<AbstractPathBasedRule> rules, MethodVertex methodVertex) {
        this.g = g;
        this.rules = rules;
        this.methodVertex = methodVertex;
        this.violations = new HashSet<>();
    }

    /**
     * Lowest level where path-based rules are executed.
     *
     * @return a set of violations that were detected
     */
    public Set<Violation> runRules() {
        // Build configuration to define how apex paths will be expanded
        final ApexPathExpanderConfig expanderConfig = getApexPathExpanderConfig();

        // Get all the paths that originate in the entry point
        final List<ApexPath> paths = getPaths(expanderConfig);

        // Execute rules on the paths found
        executeRulesOnPaths(paths);

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

        return violations;
    }

    private void executeRulesOnPaths(List<ApexPath> paths) {
        boolean foundVertex = false;
        // This set holds the violations whose information couldn't be fully processed at creation
        // time, and
        // require post-processing.
        final HashSet<FlsViolationInfo> incompleteThrowables = new HashSet<>();
        // For each path...
        for (ApexPath path : paths) {
            // If the path's metadata is present...
            if (path.getApexPathMetaInfo().isPresent()) {
                // Iterate over all vertices in the path...
                for (ApexPathVertexMetaInfo.PredicateMatch predicateMatch :
                        path.getApexPathMetaInfo().get().getAllMatches()) {
                    AbstractPathBasedRule rule =
                            (AbstractPathBasedRule) predicateMatch.getPredicate();
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
                            incompleteThrowables.add((FlsViolationInfo) ruleThrowable);
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

        convertToViolations(incompleteThrowables);

        if (!foundVertex) {
            // If no vertices were found, we should log something so that information isn't lost,
            // but don't synthesize
            // a violation or anything like that.
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("No vertices of interest found in eligible paths");
            }
        }
    }

    private void convertToViolations(HashSet<FlsViolationInfo> flsViolationInfos) {
        // Consolidate/regroup FLS violations across paths so that there are no
        // duplicates with different field sets for the same source/sink/dmlOperation
        final HashSet<FlsViolationInfo> consolidatedFlsViolationInfos =
                FlsViolationMessageUtil.consolidateFlsViolations(flsViolationInfos);

        for (FlsViolationInfo flsViolationInfo : consolidatedFlsViolationInfos) {
            final String violationMessage = FlsViolationMessageUtil.constructMessage(flsViolationInfo);
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

    private List<ApexPath> getPaths(ApexPathExpanderConfig expanderConfig) {
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex, expanderConfig);
        return paths;
    }

    private ApexPathExpanderConfig getApexPathExpanderConfig() {
        ApexPathExpanderConfig.Builder expanderConfigBuilder =
                ApexPathUtil.getFullConfiguredPathExpanderConfigBuilder();
        for (AbstractPathBasedRule rule : rules) {
            expanderConfigBuilder = expanderConfigBuilder.withVertexPredicate(rule);
        }
        ApexPathExpanderConfig expanderConfig = expanderConfigBuilder.build();
        return expanderConfig;
    }
}
