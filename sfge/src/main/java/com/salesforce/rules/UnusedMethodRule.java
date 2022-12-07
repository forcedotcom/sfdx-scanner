package com.salesforce.rules;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil.H;
import com.salesforce.graph.ops.PathEntryPointUtil;
import com.salesforce.graph.ops.directive.EngineDirective;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.UserClassVertex;
import com.salesforce.rules.unusedmethod.CallValidator;
import com.salesforce.rules.unusedmethod.InternalCallValidator;
import com.salesforce.rules.unusedmethod.SubclassCallValidator;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class UnusedMethodRule extends AbstractStaticRule {
    private static final Logger LOGGER = LogManager.getLogger(UnusedMethodRule.class);
    private static final String DESCRIPTION = "Identifies methods that are not invoked";
    private static final String VIOLATION_TEMPLATE = "Method %s in class %s is never invoked";

    GraphTraversalSource g;

    /**
     * The set of methods on which analysis was performed. Helps us know whether a method returned
     * no violations because it was inspected and an invocation was found, or if it was simply never
     * inspected in the first place.
     */
    private final Set<MethodVertex> eligibleMethods;
    /**
     * The set of methods for which no invocation was found. At the end of execution, we'll generate
     * violations from each method in this set.
     */
    private final Set<MethodVertex> unusedMethods;
    /**
     * A map used to cache every method call expression in a given class. Minimizing redundant
     * queries is a very high priority for this rule.
     */
    private final Map<String, List<InvocableWithParametersVertex>> methodCallsByDefiningType;

    private UnusedMethodRule() {
        super();
        this.eligibleMethods = new HashSet<>();
        this.unusedMethods = new HashSet<>();
        this.methodCallsByDefiningType = CollectionUtil.newTreeMap();
    }

    public static UnusedMethodRule getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    protected int getSeverity() {
        return SEVERITY.LOW.code; // TODO: VERIFY THIS CHOICE
    }

    @Override
    protected String getDescription() {
        return DESCRIPTION;
    }

    @Override
    protected String getCategory() {
        return CATEGORY.PERFORMANCE.name;
    }

    @Override
    protected String getUrl() {
        return "TODO"; // TODO: WE NEED TO CREATE DOCS AND ADD A URL HERE.
    }

    @Override
    protected boolean isEnabled() {
        // TODO: ENABLE THIS RULE WHEN READY.
        return false;
    }

    public Set<MethodVertex> getEligibleMethods() {
        return this.eligibleMethods;
    }

    @Override
    protected List<Violation> _run(
            GraphTraversalSource g, GraphTraversal<Vertex, Vertex> eligibleVertices) {
        reset(g);
        List<MethodVertex> candidateVertices = getCandidateVertices(eligibleVertices);
        seekMethodUsages(candidateVertices);
        return convertMethodsToViolations();
    }

    private void reset(GraphTraversalSource g) {
        this.g = g;
        this.eligibleMethods.clear();
        this.unusedMethods.clear();
        this.methodCallsByDefiningType.clear();
    }

    /**
     * Get a list of all method vertices on non-standard types. All such methods are candidates for
     * analysis.
     */
    private List<MethodVertex> getCandidateVertices(
            GraphTraversal<Vertex, Vertex> eligibleVertices) {
        return SFVertexFactory.loadVertices(
                g, eligibleVertices.hasLabel(NodeType.METHOD).hasNot(Schema.IS_STANDARD));
    }

    /**
     * Seek an invocation of each provided method, unless the method is deemed to be ineligible for
     * analysis. As a side effect, adds each method for which analysis was attempted to {@link
     * #eligibleMethods} and each method for which no invocation was found to {@link
     * #unusedMethods}.
     */
    private void seekMethodUsages(List<MethodVertex> candidateVertices) {
        for (MethodVertex candidateVertex : candidateVertices) {
            // If the method is one that isn't eligible to be analyzed, skip it.
            if (methodIsIneligible(candidateVertex)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            "Skipping vertex "
                                    + candidateVertex.getName()
                                    + ", as it is ineligible for analysis");
                }
                continue;
            }

            // If the method was determined as eligible, add it to the set of eligible methods.
            eligibleMethods.add(candidateVertex);

            // Check first for internal usage of the method.
            if (methodUsedInternally(candidateVertex)) {
                continue;
            }

            // Next, check for uses of the method by subclasses,
            // if the method is invocable in this way.
            if (methodVisibleToSubclasses(candidateVertex)
                    && methodUsedBySubclass(candidateVertex)) {
                continue;
            }

            // Next, check for invocations of the method by a superclass,
            // if the method is invocable in this way.
            if (methodVisibleToSuperClasses(candidateVertex)
                    && methodUsedBySuperclass(candidateVertex)) {
                continue;
            }

            // Next, check for invocations of the method by an inner/outer class,
            // if the method is invocable in this way.
            if (methodVisibleToInnerClasses(candidateVertex)
                    && methodUsedByInnerClass(candidateVertex)) {
                continue;
            }

            // Finally, check for external invocations of the method, if the method
            // is invocable in this way.
            if (methodVisibleExternally(candidateVertex) && methodUsedExternally(candidateVertex)) {
                continue;
            }

            // If we found no usage of the method, then we should add it to the final set.
            unusedMethods.add(candidateVertex);
        }
    }

    /** Convert every method in {@link #unusedMethods} to a violation, and return them in a list. */
    private List<Violation> convertMethodsToViolations() {
        return unusedMethods.stream()
                .map(
                        m ->
                                new Violation.StaticRuleViolation(
                                        String.format(
                                                VIOLATION_TEMPLATE,
                                                m.getName(),
                                                m.getDefiningType()),
                                        m))
                .collect(Collectors.toList());
    }

    /**
     * Returns true if the provided method isn't a valid candidate for analysis by this rule. Used
     * for filtering the list of all possible candidates into just the eligible ones.
     */
    private boolean methodIsIneligible(MethodVertex vertex) {
        // TODO: At this time, only private instance methods and private/protected constructors are
        //       supported. This limit will be loosened over time, and eventually removed entirely.
        if (!(vertex.isPrivate() || (vertex.isProtected() && vertex.isConstructor()))
                || vertex.isStatic()) {
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

        // Path entry points should be skipped, because they're definitionally publicly accessible
        // and therefore we must assume that they're used somewhere or other.
        if (PathEntryPointUtil.isPathEntryPoint(vertex)) {
            return true;
        }

        // If none of the above conditions were satisfied, this vertex is eligible for analysis by
        // the rule.
        return false;
    }

    /**
     * Seeks invocations of the provided method that occur within the class where it's defined.
     * E.g., `this.method()` or `method()`, etc.
     *
     * @return - True if such an invocation is found, otherwise false.
     */
    private boolean methodUsedInternally(MethodVertex methodVertex) {
        // Instantiate a visitor to use for checking internal calls.
        InternalCallValidator visitor = new InternalCallValidator(methodVertex);
        // Get every method call in the class containing the method.
        List<InvocableWithParametersVertex> potentialInternalCallers =
                getMethodCalls(methodVertex.getDefiningType());
        return validatorDetectsUsage(visitor, potentialInternalCallers);
    }

    /**
     * @return True if this method is visible to subclasses of the class where it's defined.
     */
    private boolean methodVisibleToSubclasses(MethodVertex methodVertex) {
        // If a method is private, it's not visible to subclasses.
        if (methodVertex.isPrivate()) {
            return false;
        }
        // Otherwise, the method is visible to subclasses as long as subclasses can actually exist,
        // which is the case when the class is either abstract or virtual.
        UserClassVertex classVertex =
                methodVertex
                        .getParentClass()
                        .orElseThrow(() -> new UnexpectedException(methodVertex));
        return classVertex.isAbstract() || classVertex.isVirtual();
    }

    /**
     * Seeks invocations of the provided method that occur within subclasses of the class where it's
     * defined. E.g., `this.method()` if not overridden, or `super.method()` if overridden.
     *
     * @return - True if such an invocation is found, else false. TODO: Consider optimizing this
     *     method to handle entire classes isntead of individual methods.
     */
    private boolean methodUsedBySubclass(MethodVertex methodVertex) {
        // Instantiate a visitor to use for checking subclass calls.
        SubclassCallValidator visitor = new SubclassCallValidator(methodVertex);
        // Get a list of each subclass of the method's host class.
        List<String> subclasses = getSubclasses(methodVertex.getDefiningType());
        // Put the check in a loop so we can recursively process subclasses if needed.
        while (!subclasses.isEmpty()) {
            // For each of the subclasses...
            for (String subclass : subclasses) {
                // Get every method call in that class.
                List<InvocableWithParametersVertex> potentialSubclassCallers =
                        getMethodCalls(subclass);
                // If the validator detects a usage in the subclass, then we're good.
                if (validatorDetectsUsage(visitor, potentialSubclassCallers)) {
                    return true;
                }
            }
            // If we're here, then we've checked all subclasses at this level of inheritance.
            if (methodVertex.isConstructor()) {
                // If the target method is a constructor, we're done, since constructors are only
                // visible to immediate children.
                break;
            } else {
                // For non-constructor methods, nested calls are possible, so we should get every
                // subclass of the subclasses and keep going.
                subclasses = getSubclasses(subclasses.toArray(new String[] {}));
            }
        }
        // If we're here, then we analyzed the entire subclass inheritance tree and found no
        // potential invocations.
        return false;
    }

    /**
     * @return - True if the provided method is visible to superclasses of the class where it's
     *     defined. E.g., as an implementation of an inherited abstract method.
     */
    private boolean methodVisibleToSuperClasses(MethodVertex methodVertex) {
        // TODO: IMPLEMENT THIS METHOD.
        return false;
    }

    /**
     * Seeks invocations of the provided method that occur within superclasses of the class where
     * it's defined. E.g., the parent class declares the method as abstract and then another method
     * invokes it.
     *
     * @return true if such an invocation could be found, else false.
     */
    private boolean methodUsedBySuperclass(MethodVertex methodVertex) {
        // TODO: IMPLEMENT THIS METHOD.
        return false;
    }

    /**
     * @return - true if the provided method is visible to inner classes of the class where it's
     *     defined. E.g., if it's a static method.
     */
    private boolean methodVisibleToInnerClasses(MethodVertex methodVertex) {
        // TODO: IMPLEMENT THIS METHOD.
        return false;
    }

    /**
     * Seeks invocations of the provided method by inner classes of its host class.
     *
     * @return - True if such invocations could be found, else false.
     */
    private boolean methodUsedByInnerClass(MethodVertex methodVertex) {
        // TODO: IMPLEMENT THIS METHOD.
        return false;
    }

    /**
     * @return - True if this method is visible externally, e.g., it's a public method.
     */
    private boolean methodVisibleExternally(MethodVertex methodVertex) {
        // TODO: IMPLEMENT THIS METHOD.
        return false;
    }

    /**
     * Seeks invocations of the provided method in a context wholly external to its host class.
     *
     * @return - True if such an invocation could be found. Else false.
     */
    private boolean methodUsedExternally(MethodVertex methodVertex) {
        // TODO: IMPLEMENT THIS METHOD.
        return false;
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

    /** Return a list of every method call occurring in the specified class. */
    private List<InvocableWithParametersVertex> getMethodCalls(String definingType) {
        // First, check if we've already got anything for the desired type.
        // If so, we can just return that.
        if (this.methodCallsByDefiningType.containsKey(definingType)) {
            return this.methodCallsByDefiningType.get(definingType);
        }
        // Otherwise, we need to do a query.
        // Any node with one of these labels is a method call.
        List<String> targetLabels =
                Arrays.asList(
                        NodeType.METHOD_CALL_EXPRESSION,
                        NodeType.THIS_METHOD_CALL_EXPRESSION,
                        NodeType.SUPER_METHOD_CALL_EXPRESSION);
        List<InvocableWithParametersVertex> methodCalls =
                SFVertexFactory.loadVertices(
                        g, g.V().where(H.has(targetLabels, Schema.DEFINING_TYPE, definingType)));
        // Cache and return the results.
        this.methodCallsByDefiningType.put(definingType, methodCalls);
        return methodCalls;
    }

    private boolean validatorDetectsUsage(
            CallValidator validator, List<InvocableWithParametersVertex> potentialCalls) {
        // For each call...
        for (InvocableWithParametersVertex potentialCall : potentialCalls) {
            // Use our visitor to determine whether this invocation is of the target method.
            if (validator.isValidCall(potentialCall)) {
                // If our checks are satisfied, then this method call appears to be an invocation of
                // the target method.
                return true;
            }
        }
        // If we're here, then we exited the loop without finding a call.
        return false;
    }

    /** Get all immediate subclasses of the provided classes. */
    private List<String> getSubclasses(String... definingTypes) {
        // Start with each UserClassVertex for the defining types.
        List<UserClassVertex> subclassVertices =
                SFVertexFactory.loadVertices(
                        g,
                        g.V()
                                .where(
                                        H.hasWithin(
                                                NodeType.USER_CLASS,
                                                Schema.DEFINING_TYPE,
                                                definingTypes))
                                .out(Schema.EXTENDED_BY));
        return subclassVertices.stream().map(UserClassVertex::getName).collect(Collectors.toList());
    }

    private static final class LazyHolder {
        // Postpone initialization until first use.
        private static final UnusedMethodRule INSTANCE = new UnusedMethodRule();
    }
}
