package com.salesforce.rules;

import com.google.common.annotations.VisibleForTesting;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.PathEntryPointUtil;
import com.salesforce.graph.ops.directive.EngineDirective;
import com.salesforce.graph.vertex.*;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

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
public final class UnusedMethodRule extends AbstractPathTraversalRule
        implements PostProcessingRule {
    private static final Logger LOGGER = LogManager.getLogger(UnusedMethodRule.class);
    private static final String URL =
            "https://forcedotcom.github.io/sfdx-scanner/en/v3.x/salesforce-graph-engine/rules/#UnusedMethodRule";
    /**
     * Used internally for tracking every unique method invoked during the traversal of every path.
     */
    private Set<String> encounteredUsageKeys;

    private UnusedMethodRule() {
        this.encounteredUsageKeys = CollectionUtil.newTreeSet();
    }

    @Override
    public boolean test(BaseSFVertex vertex) {
        // This rule is interested in any vertex representing an invocation of a method.
        return vertex instanceof InvocableVertex;
    }

    /**
     * Reset the internal state of the rule, so it can be reused freshly. NOTE: This method should
     * only be invoked in tests. TODO: Can we make this method's existence unnecessary?
     */
    @VisibleForTesting
    public void reset() {
        encounteredUsageKeys = CollectionUtil.newTreeSet();
    }

    /***
     * Returns no violations, but seeks the {@link MethodVertex} invoked by {@code vertex} and marks it as used.
     * @param vertex - Always an instance of {@link InvocableVertex}.
     * @return - Always an empty list.
     */
    @Override
    protected List<RuleThrowable> _run(GraphTraversalSource g, ApexPath path, BaseSFVertex vertex) {
        InvocableVertex invocable = (InvocableVertex) vertex;
        Optional<ApexPath> subpathOptional = getInvokedSubpath(path, invocable, true);
        if (subpathOptional.isPresent()) {
            // If we found a subpath corresponding to the method call, then we know which method
            // is being called, and can mark it as used.
            Optional<MethodVertex> methodOptional = subpathOptional.get().getMethodVertex();
            if (methodOptional.isPresent()) {
                String usageKey = generateUsageKey(methodOptional.get());
                encounteredUsageKeys.add(usageKey);
            }
        }

        // Method always returns an empty list.
        return new ArrayList<>();
    }

    /**
     * Search the subpaths of {@code path} for an {@link ApexPath} instance corresponding to
     * invocation of {@code invocableVertex}.
     *
     * @param checkRecursively - If true, the subpaths of {@code path} will be checked too.
     */
    private Optional<ApexPath> getInvokedSubpath(
            ApexPath path, InvocableVertex invocableVertex, boolean checkRecursively) {
        Map<InvocableVertex, ApexPath> invocableMap = path.getInvocableVertexToPaths();
        if (invocableMap.containsKey(invocableVertex)) {
            return Optional.of(invocableMap.get(invocableVertex));
        }
        if (checkRecursively) {
            List<ApexPath> allSubpaths = path.getAllSubpaths(true);
            for (ApexPath subpath : allSubpaths) {
                Optional<ApexPath> recursiveResult =
                        getInvokedSubpath(subpath, invocableVertex, false);
                if (recursiveResult.isPresent()) {
                    return recursiveResult;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Create violations for every eligible {@link MethodVertex} for which an invocation was never
     * found.
     */
    public List<Violation> postProcess(GraphTraversalSource g) {
        // Create an empty result list.
        List<Violation> results = new ArrayList<>();
        // Get every method in the graph.
        List<MethodVertex> allMethods =
                SFVertexFactory.loadVertices(g, g.V().hasLabel(NodeType.METHOD));
        for (MethodVertex methodVertex : allMethods) {
            if (methodIsIneligible(methodVertex)) {
                continue;
            }

            String usageKey = generateUsageKey(methodVertex);

            if (!encounteredUsageKeys.contains(usageKey)) {
                String violationMsg =
                        String.format(
                                UserFacingMessages.RuleViolationTemplates.UNUSED_METHOD_RULE,
                                methodVertex.getName(),
                                methodVertex.getDefiningType());
                results.add(
                        new Violation.PathBasedRuleViolation(
                                // NOTE: Since the violations represent the non-invocation of a
                                // method, the method is both source and sink.
                                violationMsg, methodVertex, methodVertex));
            }
        }
        return results;
    }

    /** Generate a unique key associated with this method, so we can mark its usage. */
    private static String generateUsageKey(MethodVertex methodVertex) {
        // Format of keys is "definingType#methodName@beginLine".
        return methodVertex.getDefiningType()
                + "#"
                + methodVertex.getName()
                + "@"
                + methodVertex.getBeginLine();
    }

    /**
     * Returns true if the provided method isn't a valid candidate for analysis by this rule. Used
     * for filtering the list of all possible candidates into just the eligible ones.
     */
    @VisibleForTesting
    public boolean methodIsIneligible(MethodVertex vertex) {
        // Test methods are ineligible.
        if (vertex.isTest()) {
            return true;
        }

        // The "<clinit>" method is inherently ineligible.
        if (vertex.getName().equalsIgnoreCase("<clinit>")) {
            return true;
        }

        // If we're directed to skip this method, obviously we should do so.
        if (directedToSkip(vertex)) {
            return true;
        }

        // Abstract methods must be implemented by all child classes.
        // This rule can detect if those implementations are unused, and another rule exists to
        // detect unused abstract classes and interface themselves. As such, inspecting
        // abstract methods directly is unnecessary.
        if (vertex.isAbstract()) {
            return true;
        }

        // Private constructors with arity of 0 are ineligible. Creating such a constructor is a
        // standard way of preventing utility classes whose only methods are static from being
        // instantiated at all, so including such methods in our analysis is likely to generate
        // more false positives than true positives.
        if (vertex.isConstructor() && vertex.isPrivate() && vertex.getArity() == 0) {
            return true;
        }

        // Methods whose name starts with this prefix are getters/setters. Getters are typically
        // used by VF controllers, and setters are frequently made private to render a property
        // immutable. As such, inspecting these methods is likely to generate false or noisy
        // positives.
        if (vertex.getName().toLowerCase().startsWith(ASTConstants.PROPERTY_METHOD_PREFIX)) {
            return true;
        }

        // Finally, path entry points should be skipped, because they're definitionally publicly
        // accessible, and therefore we must assume that they're used somewhere or other.
        // But if the method isn't a path entry point, then it's eligible.
        return PathEntryPointUtil.isPathEntryPoint(vertex);
    }

    /**
     * Helper method for {@link #methodIsIneligible(MethodVertex)}. Indicates whether a method is
     * annotated with an engine directive denoting that it should be skipped by this rule.
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

    /**
     * Returns true if the provided key matches a method we've seen. Note: This method is only
     * public for use in tests. Don't use it elsewhere.
     */
    @VisibleForTesting
    public boolean usageDetected(String key) {
        return encounteredUsageKeys.contains(key);
    }

    public static UnusedMethodRule getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    protected int getSeverity() {
        return SEVERITY.LOW.code;
    }

    @Override
    protected String getDescription() {
        return UserFacingMessages.RuleDescriptions.UNUSED_METHOD_RULE;
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
        private static final UnusedMethodRule INSTANCE = new UnusedMethodRule();
    }
}
