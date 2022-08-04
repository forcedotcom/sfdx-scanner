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
     * The set of methods for which no invocation was detected. At the end of execution, we'll
     * generate violations from each method in this set.
     */
    private final Set<MethodVertex> unusedMethods;
    /**
     * A map used to cache every method call expression in a given class. Minimizing redundant
     * queries is a very high priority in this rule.
     */
    private final Map<String, List<MethodCallExpressionVertex>> internalMethodCallsByDefiningType;

    private UnusedMethodRule() {
        super();
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
        // TODO: At this time, only private, non-static, non-constructor methods are supported.
        //  This limit will be loosened over time, and eventually removed entirely.
        (!vertex.isPrivate() || vertex.isStatic() || vertex.isConstructor())
                // If we're directed to skip this method, then we should obviously do so.
                || directedToSkip(vertex)
                // Abstract methods must be implemented by all child classes.
                // This rule can detect if those implementations are unused,
                // and we have other rules to detect unused abstract classes/interfaces.
                // As such, inspecting absract methods directly is unnecessary.
                || vertex.isAbstract()
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
        // Get every method call in the class containing the method.
        List<MethodCallExpressionVertex> potentialInternalCallers =
                getInternalCalls(methodVertex.getDefiningType());

        // For each internal call...
        for (MethodCallExpressionVertex potentialCall : potentialInternalCallers) {
            // If the method's name is wrong, it's not a match.
            // TODO: When we add support for constructors/static methods, this check must change.
            if (!potentialCall.getMethodName().equalsIgnoreCase(methodVertex.getName())) {
                continue;
            }

            // If the arity is wrong, then it's not a match, but rather a call to another
            // overload of the method.
            // The method call has a number of children equal to one more than the
            // arity of the method being called.
            List<ChainedVertex> parameters = potentialCall.getParameters();
            if (parameters.size() != methodVertex.getArity()) {
                continue;
            }

            // If the arity check was satisfied and the method accepts parameters, then
            // a more thorough check is required to make sure the parameters are
            // of the appropriate types.
            // TODO: PROPERLY IMPLEMENT AND ENABLE THESE CHECKS.
//            if (methodVertex.getArity() > 0 && !parameterTypesMatch(methodVertex, potentialCall)) {
//                continue;
//            }

            // If we're at this point, then all of our checks were satisfied,
            // and this method call seems to be an invocation of our method.
            return true;
        }
        // If we're at this point, none of the method calls satisfied our checks.
        return false;
    }

    private List<MethodCallExpressionVertex> getInternalCalls(String definingType) {
        // First, check if we've got anything for the desired type.
        // If so, we can just return that.
        if (this.internalMethodCallsByDefiningType.containsKey(definingType)) {
            return this.internalMethodCallsByDefiningType.get(definingType);
        }
        // Otherwise, we'll need to do a query for internal calls.
        // An internal call is any method call whose reference is `this` or empty.
        String[] desiredReferenceTypes =
                new String[] {
                    NodeType.THIS_VARIABLE_EXPRESSION, NodeType.EMPTY_REFERENCE_EXPRESSION
                };
        List<MethodCallExpressionVertex> internalCalls =
                SFVertexFactory.loadVertices(
                        g,
                        g.V().where(
                                        H.has(
                                                NodeType.METHOD_CALL_EXPRESSION,
                                                Schema.DEFINING_TYPE,
                                                definingType))
                                // NOTE: There's no .hasLabel(String...) method, hence the call to
                                // .hasLabel(String,String...).
                                .where(
                                        __.repeat(__.out(Schema.CHILD))
                                                .until(
                                                        __.hasLabel(
                                                                desiredReferenceTypes[0],
                                                                desiredReferenceTypes))
                                                .count()
                                                .is(P.gt(0))));

        // Cache the result, then return it.
        this.internalMethodCallsByDefiningType.put(definingType, internalCalls);
        return internalCalls;
    }

    private boolean parameterTypesMatch(
            MethodVertex method, MethodCallExpressionVertex potentialCall) {
        // Get lists of the expected and actual parameters.
        List<ParameterVertex> expectedParameters = method.getParameters();
        List<ChainedVertex> actualParameters = potentialCall.getParameters();
        // For each pair of parameters...
        for (int i = 0; i < expectedParameters.size(); i++) {
            ParameterVertex expectedParameter = expectedParameters.get(i);
            ChainedVertex actualParameter = actualParameters.get(i);
            // Make sure that this pair matches.
            Optional<BaseSFVertex> actualParameterDeclaration = findDeclaration(actualParameter);
            if (actualParameterDeclaration.isPresent()) {
                String type = getDeclaredType(actualParameterDeclaration.get());
                if (type.equalsIgnoreCase("")) {
                    // TODO: We return true here to avoid throwing false positives as we
                    //  implement more functionality. At a certain point, this will need
                    //  to change.
                    return true;
                }
                if (!expectedParameter.getCanonicalType().equalsIgnoreCase(type)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getDeclaredType(BaseSFVertex vertex) {
        if (vertex instanceof Typeable) {
            return ((Typeable) vertex).getCanonicalType();
        } else if (vertex instanceof MethodVertex) {
            return ((MethodVertex) vertex).getReturnType();
        } else if (vertex instanceof UserClassVertex || vertex instanceof UserInterfaceVertex) {
            // Can cast to NamedVertex since both classes implement NamedVertex.
            return ((NamedVertex) vertex).getName();
        } else {
            return null;
        }
    }

    /** Identify the vertex that represents a declaration of the thing being invoked. */
    private Optional<BaseSFVertex> findDeclaration(ChainedVertex chainedVertex) {
        if (chainedVertex instanceof LiteralExpressionVertex) {
            return findDeclaration((LiteralExpressionVertex<?>) chainedVertex);
        } else if (chainedVertex instanceof VariableExpressionVertex) {
            return findDeclaration((VariableExpressionVertex) chainedVertex);
        } else if (chainedVertex instanceof MethodCallExpressionVertex) {
            return findDeclaration((MethodCallExpressionVertex) chainedVertex);
        }
        return Optional.empty();
    }

    /**
     * Identify the vertex that represents a declaration of the LiteralExpressionVertex being
     * referenced.
     */
    private Optional<BaseSFVertex> findDeclaration(
            LiteralExpressionVertex<?> literalExpressionVertex) {
        // Literal expressions are their own type declaration.
        return Optional.of(literalExpressionVertex);
    }

    /**
     * Identify the vertex that represents a declaration of the VariableExpressionVertex being
     * referenced.
     */
    private Optional<BaseSFVertex> findDeclaration(VariableExpressionVertex vev) {
        // If we can't locate the declaration, we'll want to return an empty
        // Optional for safety.
        Optional<BaseSFVertex> declaration = Optional.empty();

        // In most cases, we'll need the parent class and parent method.
        // So let's get those at the start.
        UserClassVertex parentClass =
                vev.getParentClass().orElseThrow(() -> new UnexpectedException(vev));
        Optional<MethodVertex> parentMethod = vev.getParentMethod();
        // We need to identify the class where the property is declared.
        // Start by looking at the enclosed ReferenceExpression.
        AbstractReferenceExpressionVertex arev = vev.getReferenceExpression();
        if (arev instanceof EmptyReferenceExpressionVertex) {
            // An EmptyReferenceExpressionVertex means that this variable isn't being referenced
            // as a property of anything.
            // (i.e., it's referenced as `x` instead of `whatever.x`.)
            // In this case, one of three possibilities is true. They are evaluated in this order,
            // to avoid issues with conflicting names.
            // 1. The variable is declared within this method.
            if (parentMethod.isPresent()) {
                declaration =
                        getVariableDeclarationVertex(
                                g.V(parentMethod.get().getId()), vev.getName());
            }
            // 2. The variable is a property of this class, being referenced with an implicit
            // `this`.
            if (!declaration.isPresent()) {
                declaration = getPropertyDeclarationVertex(g.V(parentClass.getId()), vev.getName());
            }
            // 3. The variable is a property of a class that this class inherits from, and
            // referenced with an implicit `this`/`super`.
            if (!declaration.isPresent()) {
                // TODO: HANDLE THIS CASE.
            }
            // 4. The variable is a static property of an outer class that encloses this class,
            //    and is referenced with an implicit mention of the outer class.
            if (!declaration.isPresent()) {
                // TODO: HANDLE THIS CASE.
            }
        } else if (arev instanceof ReferenceExpressionVertex) {
            ReferenceExpressionVertex rev = (ReferenceExpressionVertex) arev;
            Optional<BaseSFVertex> hostDeclaration = followReferenceChain(rev);
            if (!hostDeclaration.isPresent()) {
                return Optional.empty();
            }
            String hostType = getDeclaredType(hostDeclaration.get());
            declaration = getPropertyDeclarationVertex(hostType, vev.getName());
        }
        return declaration;
    }

    /**
     * Identify the vertex that represents a declaration of the VariableExpressionVertex being
     * referenced.
     */
    private Optional<BaseSFVertex> findDeclaration(MethodCallExpressionVertex mcev) {
        // If we can't locate the declaration, we'll want to return an empty
        // Optional for safety.
        Optional<BaseSFVertex> declaration = Optional.empty();

        // In most cases, we'll need the parent class. So let's get that at the start here.
        UserClassVertex parentClass =
                mcev.getParentClass().orElseThrow(() -> new UnexpectedException(mcev));
        // We need to identify the class where the method is declared.
        // Start by looking at the enclosed ReferenceExpression.
        AbstractReferenceExpressionVertex arev = mcev.getReferenceExpression();
        if (arev instanceof EmptyReferenceExpressionVertex) {
            // An EmptyReferenceExpressionVertex means that this method isn't being called as a
            // property of anything.
            // (i.e., it's called as `someMethod()` instead of `whatever.someMethod()`)
            // In this case, one of three possibilities is true. They are evaluated in this order,
            // to avoid issues with conflicting names.
            // 1. The method is declared on this class, and referenced with an implicit `this`.
            declaration =
                    getMethodDeclarationVertex(g.V(parentClass.getId()), mcev.getMethodName());
            // 2. The method is declared on a class that this class inherits from, and referenced
            //    with an implicit `this`/`super`.
            if (!declaration.isPresent()) {
                // TODO: HANDLE THIS CASE
            }
            // 3. The method is a static method declared on an outer class that contains this class,
            //    and referenced with an implicit mention of the outer class.
            if (!declaration.isPresent()) {
                // TODO: HANDLE THIS CASE
            }
        } else if (arev instanceof ReferenceExpressionVertex) {
            ReferenceExpressionVertex rev = (ReferenceExpressionVertex) arev;
            Optional<BaseSFVertex> hostDeclaration = followReferenceChain(rev);
            if (!hostDeclaration.isPresent()) {
                return Optional.empty();
            }
            String hostType = getDeclaredType(hostDeclaration.get());
            declaration = getMethodDeclarationVertex(hostType, mcev.getMethodName());
        }
        return declaration;
    }

    private Optional<BaseSFVertex> followReferenceChain(ReferenceExpressionVertex rev) {
        // Track the current link in the reference chain as we go.
        Optional<BaseSFVertex> currentLink = Optional.empty();
        // We'll need the list of names in this reference chain.
        List<String> referenceNames = rev.getNames();
        // Sometimes, identifying the starting link will involve processing the first
        // link, so this value might get overridden.
        int linkIdx = 0;

        // The starting link in the chain could be one of a few things.
        if (rev.getThisVariableExpression().isPresent()) {
            // For a `this` expression, the starting link is the class containing this
            // reference. We don't need to change the link index.
            BaseSFVertex parentClass = rev.getParentClass().get();
            currentLink = Optional.of(parentClass);
        } else if (!rev.getChildren().isEmpty()) {
            // For non-`this` expressions with children, the child is our starting link.
            // So we should resolve the child with a recursive call to `findDeclaration`,
            // and we don't need to change the link index.
            currentLink = findDeclaration((ChainedVertex) rev.getChild(0));
        } else if (!referenceNames.isEmpty()) {
            // If there are no children, we should start with the first reference name,
            // which could be one of the following.
            // Check all options in this order, to avoid issues with conflicting names.
            Optional<MethodVertex> parentMethod = rev.getParentMethod();
            if (parentMethod.isPresent()) {
                // If the reference occurs inside a method, it may refer to a variable
                // defined in that method.
                currentLink =
                        getVariableDeclarationVertex(
                                g.V(parentMethod.get().getId()), referenceNames.get(0));
            }

            Optional<UserClassVertex> parentClass = rev.getParentClass();
            if (!currentLink.isPresent() && parentClass.isPresent()) {
                // A reference occurring within a class could be a reference to
                // any of the following:
                // 1. A property of this class.
                currentLink =
                        getPropertyDeclarationVertex(
                                g.V(parentClass.get().getId()), referenceNames.get(0));

                // 2. A property of a class extended by this class.
                if (!currentLink.isPresent()) {
                    // TODO: HANDLE THIS CASE
                }

                // 3. A property of an outer class enclosing this class.
                if (!currentLink.isPresent()) {
                    // TODO: HANDLE THIS CASE
                }
            }

            if (!currentLink.isPresent()) {
                // Any reference at all could be a static reference to a class.
                BaseSFVertex classDeclaration =
                        SFVertexFactory.loadSingleOrNull(
                                g,
                                g.V().where(
                                                H.has(
                                                        Arrays.asList(
                                                                NodeType.USER_CLASS,
                                                                NodeType.USER_INTERFACE),
                                                        Schema.NAME,
                                                        referenceNames.get(0))));
                if (classDeclaration != null) {
                    currentLink = Optional.of(classDeclaration);
                }
            }

            // At this point, we've already processed the first name, so we want
            // to start with the second one.
            linkIdx = 1;
        }

        // Now, we can start trying to follow the chain.
        while (linkIdx < referenceNames.size()) {
            // If we've lost the chain, just abort.
            if (!currentLink.isPresent()) {
                return Optional.empty();
            }
            // Otherwise, try to follow to the next link.
            String nextName = referenceNames.get(linkIdx);
            String linkType = getDeclaredType(currentLink.get());
            currentLink = getPropertyDeclarationVertex(linkType, nextName);
            linkIdx += 1;
        }
        return currentLink;
    }

    private Optional<BaseSFVertex> getPropertyDeclarationVertex(
            GraphTraversal<Vertex, Vertex> baseTraversal, String propertyName) {
        return getDeclarationVertex(baseTraversal, propertyName, new String[] {NodeType.FIELD});
    }

    private Optional<BaseSFVertex> getPropertyDeclarationVertex(
            String className, String propertyName) {
        return getPropertyDeclarationVertex(
                g.V().where(
                                H.has(
                                        Arrays.asList(NodeType.USER_CLASS, NodeType.USER_INTERFACE),
                                        Schema.NAME,
                                        className)),
                propertyName);
    }

    private Optional<BaseSFVertex> getVariableDeclarationVertex(
            GraphTraversal<Vertex, Vertex> baseTraversal, String variableName) {
        return getDeclarationVertex(
                baseTraversal,
                variableName,
                new String[] {NodeType.VARIABLE_DECLARATION, NodeType.PARAMETER});
    }

    private Optional<BaseSFVertex> getMethodDeclarationVertex(
            GraphTraversal<Vertex, Vertex> baseTraversal, String methodName) {
        return getDeclarationVertex(baseTraversal, methodName, new String[] {NodeType.METHOD});
    }

    private Optional<BaseSFVertex> getMethodDeclarationVertex(String className, String methodName) {
        return getMethodDeclarationVertex(
                g.V().where(
                                H.has(
                                        Arrays.asList(NodeType.USER_CLASS, NodeType.USER_INTERFACE),
                                        Schema.NAME,
                                        className)),
                methodName);
    }

    private Optional<BaseSFVertex> getDeclarationVertex(
            GraphTraversal<Vertex, Vertex> baseTraversal,
            String declarationName,
            String[] declarationLabels) {
        List<BaseSFVertex> vertices =
                SFVertexFactory.loadVertices(
                        g,
                        baseTraversal
                                .repeat(__.out(Schema.CHILD))
                                .emit()
                                .where(
                                        H.has(
                                                Arrays.asList(declarationLabels),
                                                Schema.NAME,
                                                declarationName)));
        if (vertices.isEmpty()) {
            // If we found no vertices, return an empty Optional.
            return Optional.empty();
        } else if (vertices.size() == 1) {
            // If we found one vertex, return it.
            return Optional.of(vertices.get(0));
        } else {
            // If we found neither, we've got a problem.
            throw new UnexpectedException(declarationName);
        }
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

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final UnusedMethodRule INSTANCE = new UnusedMethodRule();
    }
}
