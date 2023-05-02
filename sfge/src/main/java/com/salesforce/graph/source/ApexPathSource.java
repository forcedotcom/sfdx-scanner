package com.salesforce.graph.source;

import com.salesforce.graph.ops.MethodUtil;
import com.salesforce.graph.source.supplier.*;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.AbstractRuleRunner;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Descriptor class for {@link MethodVertex} instances selected to act as sources for {@link
 * com.salesforce.graph.ApexPath} instances.
 */
public class ApexPathSource {
    /**
     * The various kinds of sources that an {@link com.salesforce.rules.AbstractPathBasedRule} may
     * or may not be interested in.
     */
    public enum Type {
        /** Non-test methods with the {@code @AuraEnabled} annotation. */
        ANNOTATION_AURA_ENABLED(new AnnotationAuraEnabledSupplier()),
        /** Non-test methods with the {@code @InvocableMethod} annotation. */
        ANNOTATION_INVOCABLE_METHOD(new AnnotationInvocableMethodSupplier()),
        /** Non-test methods with the {@code @NamespaceAccessible} annotation. */
        ANNOTATION_NAMESPACE_ACCESSIBLE(new AnnotationNamespaceAccessibleSupplier()),
        /** Non-test methods with the {@code @RemoteAction} annotation. */
        ANNOTATION_REMOTE_ACTION(new AnnotationRemoteActionSupplier()),
        /**
         * {@code public}/{@code global}, non-test methods on Controllers referenced by VisualForce
         * pages.
         */
        EXPOSED_CONTROLLER_METHOD(new ExposedControllerMethodSupplier()),
        /** Non-test {@code global}-scoped methods. */
        GLOBAL_METHOD(new GlobalMethodSupplier()),
        /**
         * The {@code handleInboundEmail()} method on implementations of {@code
         * InboundEmailHandler}.
         */
        INBOUND_EMAIL_HANDLER(new InboundEmailHandlerSupplier()),
        /** Non-test methods that return a {@code PageReference} object. */
        PAGE_REFERENCE(new PageReferenceSupplier());

        private final AbstractSourceSupplier supplier;

        Type(AbstractSourceSupplier supplier) {
            this.supplier = supplier;
        }

        /**
         * Load all {@link MethodVertex} instances in the specified files that count as sources per
         * this type.
         *
         * @param targets - The files to look in. An empty list implicitly includes all files.
         */
        private List<MethodVertex> getSources(GraphTraversalSource g, List<String> targets) {
            return supplier.getVertices(g, targets);
        }

        /** Returns true if {@code methodVertex} would be considered a source of this type. */
        private boolean isPotentialSource(MethodVertex methodVertex) {
            return supplier.isPotentialSource(methodVertex);
        }
    }

    /** The method acting as the path's source. */
    private final MethodVertex methodVertex;

    /** If {@code true}, this source was explicitly requested by the user. */
    private final boolean forceTargeted;

    /**
     * @param forceTargeted - Was this source explicitly requested by the user?
     */
    private ApexPathSource(MethodVertex methodVertex, boolean forceTargeted) {
        this.methodVertex = methodVertex;
        this.forceTargeted = forceTargeted;
    }

    /**
     * @return - The method acting as a source.
     */
    public MethodVertex getMethodVertex() {
        return methodVertex;
    }

    /**
     * @return - True if this source was explicitly targeted by the user.
     */
    public boolean isForceTargeted() {
        return forceTargeted;
    }

    /**
     * Get all sources of interest to the specified rules, located in the specified targets.
     *
     * @param rules - The rules whose sources should be loaded. An empty list implicitly includes
     *     all rules.
     * @param targets - The targets where sources may be located. An empty list implicitly includes
     *     all files.
     */
    public static List<ApexPathSource> getApexPathSources(
            GraphTraversalSource g,
            List<AbstractPathBasedRule> rules,
            List<AbstractRuleRunner.RuleRunnerTarget> targets) {
        // Sort the list of targets into full-file targets and method-level targets.
        List<String> fileLevelTargets =
                targets.stream()
                        .filter(t -> t.getTargetMethods().isEmpty())
                        .map(AbstractRuleRunner.RuleRunnerTarget::getTargetFile)
                        .collect(Collectors.toList());
        List<AbstractRuleRunner.RuleRunnerTarget> methodLevelTargets =
                targets.stream()
                        .filter(t -> !t.getTargetMethods().isEmpty())
                        .collect(Collectors.toList());

        // Create our empty result list.
        List<ApexPathSource> results = new ArrayList<>();
        // We'll also want to use a set, to avoid duplicates.
        Set<MethodVertex> uniqueMethods = new HashSet<>();

        // Start with method-level targets, if any were present.
        if (!methodLevelTargets.isEmpty()) {
            for (MethodVertex method : MethodUtil.getTargetedMethods(g, methodLevelTargets)) {
                if (!uniqueMethods.contains(method)) {
                    results.add(new ApexPathSource(method, true));
                    uniqueMethods.add(method);
                }
            }
        }

        // Next, handle file-level targets. If there are no explicit targets of any kind, that's
        // treated as implicitly targeting all files.
        if (!fileLevelTargets.isEmpty() || targets.isEmpty()) {
            // If any rules were provided, we want sources relevant to those rules.
            // Otherwise, we want all sources.
            List<Type> types =
                    rules.isEmpty()
                            ? Arrays.asList(Type.values())
                            : rules.stream()
                                    .map(AbstractPathBasedRule::getSourceTypes)
                                    .flatMap(Collection::stream)
                                    .collect(Collectors.toList());

            for (Type type : types) {
                for (MethodVertex method : type.getSources(g, fileLevelTargets)) {
                    if (!uniqueMethods.contains(method)) {
                        results.add(new ApexPathSource(method, false));
                        uniqueMethods.add(method);
                    }
                }
            }
        }
        return results;
    }

    /**
     * Indicates whether the provided method could potentially be considered as a Source of any
     * type.
     */
    public static boolean isPotentialSource(MethodVertex methodVertex) {
        return isPotentialSource(methodVertex, Arrays.asList(Type.values()));
    }

    /**
     * Indicates whether the provided method could potentially be considered a Source of one of the
     * specified types.
     */
    public static boolean isPotentialSource(MethodVertex methodVertex, List<Type> types) {
        return !types.isEmpty() && types.stream().anyMatch(t -> t.isPotentialSource(methodVertex));
    }
}
