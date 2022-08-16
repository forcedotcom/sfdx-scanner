package com.salesforce.rules;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil.H;
import com.salesforce.graph.ops.PathEntryPointUtil;
import com.salesforce.graph.ops.directive.EngineDirective;
import com.salesforce.graph.vertex.*;
import com.salesforce.graph.visitor.TypedVertexVisitor;
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
    GraphTraversalSource g;
    /**
     * The set of methods on which analysis was performed. In tests, it's important to know whether
     * a method returned no violations because it was inspected and an invocation was found, or if
     * it was simply never inspected in the first place.
     */
    private final Set<MethodVertex> eligibleMethods;
    /**
     * The set of methods for which no invocation was detected. At the end of execution, we'll
     * generate violations from each method in this set.
     */
    private final Set<MethodVertex> unusedMethods;
    /**
     * A map used to cache every method call expression in a given class. Minimizing redundant
     * queries is a very high priority in this rule.
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
        return DESCRIPTION; // TODO: VERIFY THIS CHOICE
    }

    @Override
    protected String getCategory() {
        return CATEGORY.PERFORMANCE.name; // TODO: CONFIRM THAT THIS IS A GOOD CHOICE
    }

    @Override
    protected String getUrl() {
        return "TODO"; // TODO: ADD A VALUE HERE
    }

    @Override
    protected boolean isEnabled() {
        return true;
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
     * Gets a list of all method vertices on non-standard types. All such methods are candidates for
     * analysis.
     */
    private List<MethodVertex> getCandidateVertices(
            GraphTraversal<Vertex, Vertex> eligibleVertices) {
        return SFVertexFactory.loadVertices(
                g, eligibleVertices.hasLabel(NodeType.METHOD).hasNot(Schema.IS_STANDARD));
    }

    /**
     * Seeks an invocation of each provided method, unless the method is deemed to be ineligible for
     * analysis. As side-effects, adds each method for which analysis was attempted to {@link
     * #eligibleMethods}, and adds each method for which no invocation was found to {@link
     * #unusedMethods}.
     */
    private void seekMethodUsages(List<MethodVertex> candidateVertices) {
        for (MethodVertex candidateVertex : candidateVertices) {
            // If the method is one that is ineligible to be analyzed, skip it.
            if (methodIsIneligible(candidateVertex)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            "Skipping vertex "
                                    + candidateVertex.getName()
                                    + ", as it is ineligible for analysis.");
                }
                continue;
            }

            // If the method was determined as eligible for scanning, add it to
            // the set of eligible methods.
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
            if (methodVisibleToSuperclasses(candidateVertex)
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

            // If we found no usage of the method, then we should add it
            // to the final set.
            unusedMethods.add(candidateVertex);
        }
    }

    /**
     * Returns true if the provided method isn't a valid candidate for analysis by this rule. Used
     * for filtering the list of all possible candidates into just the eligible ones.
     */
    private boolean methodIsIneligible(MethodVertex vertex) {
        return
        // TODO: At this time, only private instance methods and private/protected
        //  constructors are supported. This limit will be loosened over time,
        //  and eventually removed entirely.
        (!(vertex.isPrivate() || (vertex.isProtected() && vertex.isConstructor()))
                        || vertex.isStatic())
                // If we're directed to skip this method, then we should obviously do so.
                || directedToSkip(vertex)
                // Abstract methods must be implemented by all child classes.
                // This rule can detect if those implementations are unused,
                // and we have other rules to detect unused abstract classes/interfaces.
                // As such, inspecting abstract methods directly is unnecessary.
                || vertex.isAbstract()
                // Private constructors with arity of 0 are ineligible. Creating such
                // a constructor is a standard way of preventing utility classes with only
                // static methods from being instantiated at all, and including it in
                // our analysis is likely to generate more false positives than true positives.
                || vertex.isConstructor() && vertex.isPrivate() && vertex.getArity() == 0
                // Methods whose name starts with this prefix are getters/setters.
                // Getters are typically used by VF controllers, and setters are frequently
                // made private to render a property unchangeable.
                // As such, inspecting these methods is likely to generate false or noisy
                // positives.
                || vertex.getName().toLowerCase().startsWith(ASTConstants.PROPERTY_METHOD_PREFIX)
                // Path entry points should be skipped, since they're definitionally publicly
                // accessible, and we must assume that they're being used somewhere or other.
                || PathEntryPointUtil.isPathEntryPoint(vertex);
    }

    /**
     * Seeks invocations of the provided method that occur within the class where it's defined.
     * E.g., `this.method()` or `method()`, etc.
     *
     * @return - True if such an invocation can be found, else false.
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
     * @return True if this provided method is visible to subclasses of the class where it's
     *     defined.
     */
    private boolean methodVisibleToSubclasses(MethodVertex methodVertex) {
        // If a method is private, it's not visible to subclasses.
        if (methodVertex.isPrivate()) {
            return false;
        }
        // If a method is protected or public, then it's visible to subclasses.
        // But subclasses can only exist if the parent class is abstract or virtual.
        UserClassVertex classVertex =
                methodVertex
                        .getParentClass()
                        .orElseThrow(() -> new UnexpectedException(methodVertex));
        return classVertex.isAbstract() || classVertex.isVirtual();
    }

    /**
     * Seeks invocations of the provided method that occur within subclasses of the class where it's
     * defined. E.g., 'this.method()` if not overridden, or `super.method()` if overridden.
     *
     * @return - True if such an invocation is found, else false.
     */
    private boolean methodUsedBySubclass(MethodVertex methodVertex) {
        // Instantiate a visitor to use for checking subclass calls.
        SubclassCallValidator visitor = new SubclassCallValidator(methodVertex);
        // Get a list of each subclass of the method's host class.
        List<String> subclasses = getSubclasses(methodVertex.getDefiningType());
        // We'll want a loop so we can recursively process subclasses if needed.
        while (!subclasses.isEmpty()) {
            // For each of the subclasses...
            for (String subclass : subclasses) {
                // Get every method call in that class.
                List<InvocableWithParametersVertex> potentialSubclassCallers =
                        getMethodCalls(subclass);
                // If the validator detects a usage in the subclass, then we're set.
                if (validatorDetectsUsage(visitor, potentialSubclassCallers)) {
                    return true;
                }
            }
            // If we're here, then we've checked every subclass at this level.
            // For a constructor, we're done, since constructors are only
            // visible to immediate children.
            // But for other methods, nested calls are possible, so we should
            // get every subclass of the subclasses and keep going.
            if (methodVertex.isConstructor()) {
                break;
            } else {
                subclasses = getSubclasses(subclasses.toArray(new String[] {}));
            }
        }
        // If we're here, then we've fully examined the subclasses and found nothing.
        // So we should return false.
        return false;
    }

    /**
     * @return - True if the provided method is visible to superclasses of the class where it's
     *     defined. E.g., it's an implementation of an inherited abstract method.
     */
    private boolean methodVisibleToSuperclasses(MethodVertex methodVertex) {
        // TODO: IMPLEMENT THIS METHOD
        return false;
    }

    /**
     * Seeks invocations of the provided method that occur within superclasses of the class where
     * it's defined. E.g., the parent class declares the method as abstract and then another method
     * invokes it.
     *
     * @return - True if such an invocation is found, else false.
     */
    private boolean methodUsedBySuperclass(MethodVertex methodVertex) {
        // TODO: IMPLEMENT THIS METHOD
        return false;
    }

    /**
     * @return - True if the provided method is visible to inner classes of the class where it's
     *     defined. E.g., if it's a static method.
     */
    private boolean methodVisibleToInnerClasses(MethodVertex methodVertex) {
        // TODO: IMPLEMENT THIS METHOD
        return false;
    }

    /**
     * Seeks invocations of the provided method by inner classes of its host class.
     *
     * @return - True if such an invocation is found, else false.
     */
    private boolean methodUsedByInnerClass(MethodVertex methodVertex) {
        // TODO: IMPLEMENT THIS METHOD
        return false;
    }

    /** @return - True if the provided method is visible externally, e.g. it's a public method. */
    private boolean methodVisibleExternally(MethodVertex methodVertex) {
        // TODO: IMPLEMENT THIS METHOD
        return false;
    }

    private boolean methodUsedExternally(MethodVertex methodVertex) {
        // TODO: IMPLEMENT THIS METHOD
        return false;
    }

    /**
     * Helper method for {@link #methodIsIneligible(MethodVertex)}. Indicates whether a method is
     * annotated with an engine directive denoting that this rule should skip it.
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

    /** Returns a list of every method call occurring in the specified class. */
    private List<InvocableWithParametersVertex> getMethodCalls(String definingType) {
        // First, check if we've got anything for the desired type.
        // If so, we can just return that.
        if (this.methodCallsByDefiningType.containsKey(definingType)) {
            return this.methodCallsByDefiningType.get(definingType);
        }
        // Otherwise, we'll need to do a query.
        // Any node with one of these labels is a method call.
        List<String> targetLabels =
                Arrays.asList(
                        NodeType.METHOD_CALL_EXPRESSION,
                        NodeType.THIS_METHOD_CALL_EXPRESSION,
                        NodeType.SUPER_METHOD_CALL_EXPRESSION);
        List<InvocableWithParametersVertex> methodCalls =
                SFVertexFactory.loadVertices(
                        g, g.V().where(H.has(targetLabels, Schema.DEFINING_TYPE, definingType)));
        // Cache and then return the result.
        this.methodCallsByDefiningType.put(definingType, methodCalls);
        return methodCalls;
    }

    /** @return - A list of every immediate subclass of the provided classes */
    private List<String> getSubclasses(String... definingTypes) {
        // Start with each UserClassVertex for the defining types.
        List<UserClassVertex> subclassVertices =
                SFVertexFactory.loadVertices(
                        g,
                        g.V().where(
                                        H.hasWithin(
                                                NodeType.USER_CLASS,
                                                Schema.DEFINING_TYPE,
                                                definingTypes))
                                // Go out to every subclass.
                                .out(Schema.EXTENDED_BY));
        return subclassVertices.stream().map(UserClassVertex::getName).collect(Collectors.toList());
    }

    /**
     * Applies the provided validator to each of the provided method call expressions, to see if
     * they are an invocation of the desired method.
     */
    private boolean validatorDetectsUsage(
            CallValidator validator, List<InvocableWithParametersVertex> potentialCalls) {
        // For each call...
        for (InvocableWithParametersVertex potentialCall : potentialCalls) {
            // Use our visitor to determine whether this invocation is of the target method.
            if (validator.isValidCall(potentialCall)) {
                // If our checks were satisfied, then this method call
                // appears to be an invocation of the target method.
                return true;
            }
        }
        // If we're here, none of the method calls satisfied our checks.
        return false;
    }

    /**
     * Converts every method in {@link #unusedMethods} into a violation, and returns them as a list.
     */
    private List<Violation> convertMethodsToViolations() {
        return unusedMethods.stream()
                .map(
                        m ->
                                new Violation.StaticRuleViolation(
                                        String.format(
                                                "Method %s in class %s is never invoked",
                                                m.getName(), m.getDefiningType()),
                                        m))
                .collect(Collectors.toList());
    }

    /** Base class for call validators. */
    private static class CallValidator extends TypedVertexVisitor.DefaultNoOp<Boolean> {
        protected final MethodVertex invokedMethod;

        protected CallValidator(MethodVertex invokedMethod) {
            this.invokedMethod = invokedMethod;
        }

        /**
         * Override the default visit method to return false, since vertex types we're not prepared
         * to deal with should not be interpreted as usage of the target method.
         */
        @Override
        public Boolean defaultVisit(BaseSFVertex vertex) {
            return false;
        }

        protected boolean isValidCall(InvocableWithParametersVertex vertex) {
            return vertex.accept(this);
        }

        protected boolean parametersAreValid(InvocableWithParametersVertex vertex) {
            // If the arity is wrong, then it's not a match, but rather a call to another
            // overload of the same method.
            // TODO: Long-term, we'll want to validate the parameters' types in addition
            //  to their count.
            List<ChainedVertex> parameters = vertex.getParameters();
            return parameters.size() == invokedMethod.getArity();
        }
    }

    /**
     * Validator that can determine whether an internal call is an invocation of the targeted
     * method.
     */
    private static final class InternalCallValidator extends CallValidator {

        private InternalCallValidator(MethodVertex invokedMethod) {
            super(invokedMethod);
        }

        @Override
        public Boolean visit(MethodCallExpressionVertex vertex) {
            // If the method is a constructor, then this can't be an
            // invocation of that method.
            if (invokedMethod.isConstructor()) {
                return false;
            }
            // If the contained reference expression isn't a `this` expression or empty,
            // then this isn't an internal call, and therefore can't be an internal invocation
            // of the method.
            if (!vertex.isThisReference() && !vertex.isEmptyReference()) {
                return false;
            }

            // If the method's name is wrong, then it's not a match.
            if (!vertex.getMethodName().equalsIgnoreCase(invokedMethod.getName())) {
                return false;
            }
            return parametersAreValid(vertex);
        }

        @Override
        public Boolean visit(ThisMethodCallExpressionVertex vertex) {
            // If the method we're looking for isn't a constructor, then
            // this can't be an invocation of that method.
            if (!invokedMethod.isConstructor()) {
                return false;
            }
            return parametersAreValid(vertex);
        }
    }

    /**
     * Validator that can determine whether an internal call by a subclass is an invocation of the
     * targeted method.
     */
    private static final class SubclassCallValidator extends CallValidator {
        private SubclassCallValidator(MethodVertex invokedMethod) {
            super(invokedMethod);
        }

        @Override
        public Boolean visit(MethodCallExpressionVertex vertex) {
            // TODO: IMPLEMENT THIS METHOD
            return false;
        }

        @Override
        public Boolean visit(SuperMethodCallExpressionVertex vertex) {
            // If the method we're looking for isn't a constructor, then
            // this can't be an invocation of that method.
            if (!invokedMethod.isConstructor()) {
                return false;
            }
            return parametersAreValid(vertex);
        }
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final UnusedMethodRule INSTANCE = new UnusedMethodRule();
    }
}
