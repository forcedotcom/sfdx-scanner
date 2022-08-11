package com.salesforce.rules;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.collections.CollectionUtil;
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
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
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
    private final Map<String, List<InvocableWithParametersVertex>>
            internalMethodCallsByDefiningType;

    private UnusedMethodRule() {
        super();
        this.eligibleMethods = new HashSet<>();
        this.unusedMethods = new HashSet<>();
        this.internalMethodCallsByDefiningType = CollectionUtil.newTreeMap();
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
        this.internalMethodCallsByDefiningType.clear();
    }

    private List<MethodVertex> getCandidateVertices(
            GraphTraversal<Vertex, Vertex> eligibleVertices) {
        return SFVertexFactory.loadVertices(
                g, eligibleVertices.hasLabel(NodeType.METHOD).hasNot(Schema.IS_STANDARD));
    }

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

            // TODO: In the future, we'll need to check for external uses.

            // If we found no usage of the method, then we should add it
            // to the final set.
            unusedMethods.add(candidateVertex);
        }
    }

    private boolean methodIsIneligible(MethodVertex vertex) {
        return
        // TODO: At this time, only private, non-static methods are supported.
        //  This limit will be loosened over time, and eventually removed entirely.
        (!vertex.isPrivate() || vertex.isStatic())
                // If we're directed to skip this method, then we should obviously do so.
                || directedToSkip(vertex)
                // Abstract methods must be implemented by all child classes.
                // This rule can detect if those implementations are unused,
                // and we have other rules to detect unused abstract classes/interfaces.
                // As such, inspecting absract methods directly is unnecessary.
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

    private boolean methodUsedInternally(MethodVertex methodVertex) {
        // Instantiate a visitor to use for checking internal calls.
        InternalCallValidator visitor = new InternalCallValidator(methodVertex);
        // Get every method call in the class containing the method.
        List<InvocableWithParametersVertex> potentialInternalCallers =
                getInternalCalls(methodVertex.getDefiningType());

        // For each internal call...
        for (InvocableWithParametersVertex potentialCall : potentialInternalCallers) {
            // Use our visitor to determine whether this invocation is of the target method.
            if (!visitor.isInternalCall(potentialCall)) {
                continue;
            }
            // If we're here, then our checks were satisfied, and this method call
            // appears to be an invocation of the target method.
            return true;
        }
        // If we're at this point, none of the method calls satisfied our checks.
        return false;
    }

    private List<InvocableWithParametersVertex> getInternalCalls(String definingType) {
        // First, check if we've got anything for the desired type.
        // If so, we can just return that.
        if (this.internalMethodCallsByDefiningType.containsKey(definingType)) {
            return this.internalMethodCallsByDefiningType.get(definingType);
        }
        // Otherwise, we'll need to do a query for internal calls.
        // For instance methods, an internal call is any method call whose reference is `this` or
        // empty.
        String[] desiredReferenceTypes =
                new String[] {
                    NodeType.THIS_VARIABLE_EXPRESSION, NodeType.EMPTY_REFERENCE_EXPRESSION
                };
        List<InvocableWithParametersVertex> internalCalls =
                SFVertexFactory.loadVertices(
                        g,
                        g.V().or(
                                        __.where(
                                                        H.has(
                                                                NodeType.METHOD_CALL_EXPRESSION,
                                                                Schema.DEFINING_TYPE,
                                                                definingType))
                                                // NOTE: There's no .hasLabel(String...) method,
                                                // hence the call to
                                                // .hasLabel(String,String...) with the array
                                                // defined earlier.
                                                .where(
                                                        __.repeat(__.out(Schema.CHILD))
                                                                .until(
                                                                        __.hasLabel(
                                                                                desiredReferenceTypes[
                                                                                        0],
                                                                                desiredReferenceTypes))
                                                                .count()
                                                                .is(P.gt(0))),
                                        __.where(
                                                H.has(
                                                        NodeType.THIS_METHOD_CALL_EXPRESSION,
                                                        Schema.DEFINING_TYPE,
                                                        definingType))));

        // Cache the result, then return it.
        this.internalMethodCallsByDefiningType.put(definingType, internalCalls);
        return internalCalls;
    }

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

    private static final class InternalCallValidator
            extends TypedVertexVisitor.DefaultNoOp<Boolean> {
        private final MethodVertex invokedMethod;

        private InternalCallValidator(MethodVertex invokedMethod) {
            this.invokedMethod = invokedMethod;
        }

        private boolean isInternalCall(InvocableWithParametersVertex vertex) {
            return vertex.accept(this);
        }

        private boolean parametersAreValid(InvocableWithParametersVertex vertex) {
            // If the arity is wrong, then it's not a match, but rather a call to another
            // overload of the same method.
            List<ChainedVertex> parameters = vertex.getParameters();
            if (parameters.size() != invokedMethod.getArity()) {
                return false;
            }
            // TODO: Long-term, we'll want to validate the parameters' types in addition
            //  to their count.
            return true;
        }

        @Override
        public Boolean visit(MethodCallExpressionVertex vertex) {
            // If the method is a constructor, then this can't be an
            // invocation of that method.
            if (invokedMethod.isConstructor()) {
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

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final UnusedMethodRule INSTANCE = new UnusedMethodRule();
    }
}
