package com.salesforce.rules;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil.H;
import com.salesforce.graph.ops.directive.EngineDirective;
import com.salesforce.graph.ops.expander.PathExpansionObserver;
import com.salesforce.graph.source.ApexPathSource;
import com.salesforce.graph.vertex.*;
import com.salesforce.rules.unusedmethod.operations.UsageTracker;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

/**
 * Rule for identifying dead code. After all {@link ApexPath} instances have been traversed,
 * generates violations for any eligible {@link MethodVertex} that was never invoked. A method is
 * considered eligible if it meets all the following criteria:
 *
 * <ol>
 *   <li>The method is not declared as a {@code testMethod}
 *   <li>The method is not declared as {@code abstract}
 *   <li>The method is not a private, 0-arity constructor
 *   <li>The method is not a VisualForce getter/setter
 *   <li>The method is not considered a path entrypoint
 *   <li>The method is not annotated with an engine directive that silences this rule.
 * </ol>
 */
public final class RemoveUnusedMethod extends AbstractPathBasedRule implements PostProcessingRule {
    private static final Logger LOGGER = LogManager.getLogger(RemoveUnusedMethod.class);
    // UnusedMethodRule cares about all sources, since they're all equally capable of using a
    // method.
    private static final ImmutableSet<ApexPathSource.Type> SOURCE_TYPES =
            ImmutableSet.copyOf(ApexPathSource.Type.values());
    private static final String URL =
            "https://forcedotcom.github.io/sfdx-scanner/en/v3.x/salesforce-graph-engine/rules/#pilot-rules";

    private RemoveUnusedMethod() {}

    @Override
    public Optional<PathExpansionObserver> getPathExpansionObserver() {
        return Optional.of(new UsageTracker());
    }

    @Override
    public ImmutableSet<ApexPathSource.Type> getSourceTypes() {
        return SOURCE_TYPES;
    }

    /**
     * Create violations for every eligible {@link MethodVertex} for which an invocation was never
     * found.
     */
    public List<Violation> postProcess(GraphTraversalSource g) {
        // Create an empty result list.
        List<Violation> results = new ArrayList<>();
        // Get all vertices eligible for the rule.
        List<MethodVertex> eligibleMethods = getEligibleMethods(g);
        UsageTracker usageTracker = new UsageTracker();
        for (MethodVertex methodVertex : eligibleMethods) {

            if (!usageTracker.isUsed(methodVertex.generateUniqueKey())) {
                String violationMsg =
                        String.format(
                                UserFacingMessages.RuleViolationTemplates.REMOVE_UNUSED_METHOD,
                                methodVertex.getName(),
                                methodVertex.getDefiningType());
                Violation.PathBasedRuleViolation violation =
                        new Violation.PathBasedRuleViolation(
                                // NOTE: Since the violations represent the non-invocation of a
                                // method, the method is both source and sink.
                                violationMsg, methodVertex, methodVertex);
                violation.setPropertiesFromRule(this);
                results.add(violation);
            }
        }
        return results;
    }

    /** Returns every {@link MethodVertex} instance eligible for analysis under this rule. */
    @VisibleForTesting
    public List<MethodVertex> getEligibleMethods(GraphTraversalSource g) {
        // Some eligibility exclusions are easy to apply in the initial query.
        List<MethodVertex> methods =
                SFVertexFactory.loadVertices(
                        g,
                        g.V()
                                .hasLabel(NodeType.METHOD)
                                // The "<clinit>" method is ineligible.
                                .has(Schema.NAME, P.neq("<clinit>"))
                                // TODO: FOR NOW, WE'RE IGNORING CONSTRUCTORS TO CUT DOWN ON NOISE.
                                //       IN THE FULLNESS OF TIME, AS WE FIX RELEVANT BUGS, WE'LL
                                //       REMOVE THIS RESTRICTION.
                                .has(Schema.NAME, P.neq("<init>"))
                                // Getters are typically used by VF controllers rather than Apex,
                                // and setters are often private to render the property immutable.
                                // As such, including these methods is likely to generate
                                // false/noisy positives.
                                .where(
                                        H.hasNotStartingWith(
                                                NodeType.METHOD,
                                                Schema.NAME,
                                                ASTConstants.PROPERTY_METHOD_PREFIX))
                                // Triggers technically have methods. We should ignore those.
                                // TODO: Once triggers are explicit sources, this line is
                                // technically unnecessary.
                                .where(
                                        __.out(Schema.PARENT)
                                                .hasLabel(NodeType.USER_TRIGGER)
                                                .count()
                                                .is(P.eq(0)))
                                // Abstract methods must be implemented by all concrete child
                                // classes. This rule can detect whether those concrete
                                // implementations are used, and another rule detects abstract
                                // types with no implementations. Therefore, inspecting abstract
                                // methods directly is redundant.
                                .where(
                                        __.out(Schema.CHILD)
                                                .has(NodeType.MODIFIER_NODE, Schema.ABSTRACT, false)
                                                .count()
                                                .is(P.eq(1)))
                                // Test methods are ineligible.
                                .where(
                                        __.or(
                                                __.hasNot(Schema.IS_TEST),
                                                __.has(Schema.IS_TEST, false))));
        // Other eligibility exclusions are more easily applied to the returned list.
        return methods.stream()
                .filter(
                        methodVertex -> {
                            // Private, 0-arity constructors are excluded, since they are most often
                            // used by Util classes and other static-method-only classes to prevent
                            // instantiation entirely and thus uninvoked by design.
                            if (methodVertex.isConstructor()
                                    && methodVertex.isPrivate()
                                    && methodVertex.getArity() == 0) {
                                return false;
                            }
                            // Vertices we're directed to skip should be skipped.
                            if (directedToSkip(methodVertex)) {
                                return false;
                            }
                            // We should also skip path entrypoints, because they're definitionally
                            // publicly accessible, and therefore we should assume they're used
                            // somewhere or other.
                            return !ApexPathSource.isPotentialSource(methodVertex);
                        })
                .collect(Collectors.toList());
    }

    /**
     * Helper method for {@link #getEligibleMethods(GraphTraversalSource)}. Indicates whether a
     * method is annotated with an engine directive denoting that it should be skipped by this rule.
     */
    private boolean directedToSkip(MethodVertex methodVertex) {
        List<EngineDirective> directives = methodVertex.getAllEngineDirectives();
        for (EngineDirective directive : directives) {
            if (directive.isAnyDisable()
                    && directive.matchesRule(this.getClass().getSimpleName())) {
                return true;
            }
        }
        return false;
    }

    public static RemoveUnusedMethod getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    protected int getSeverity() {
        return SEVERITY.LOW.code;
    }

    @Override
    protected String getDescription() {
        return UserFacingMessages.RuleDescriptions.REMOVE_UNUSED_METHOD;
    }

    @Override
    protected String getCategory() {
        return CATEGORY.PERFORMANCE.name;
    }

    @Override
    protected String getUrl() {
        return URL;
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final RemoveUnusedMethod INSTANCE = new RemoveUnusedMethod();
    }
}
