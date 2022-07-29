package com.salesforce.rules;

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
        return false; // TODO: ENABLE THE RULE WHEN READY
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
                    LOGGER.info("Skipping vertex " + candidateVertex.getName() + ", as it is ineligible for analysis.");
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
                // Abstract methods are ineligible.
                || vertex.isAbstract()
                // Path entry points should be skipped.
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
            if (methodVertex.getArity() > 0 && !parameterTypesMatch(methodVertex, potentialCall)) {
                continue;
            }

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
            Optional<BaseSFVertex> actualParameterDeclaration = getTypeDeclaration(actualParameter);
            if (actualParameterDeclaration.isPresent()) {
                String type;
                if (actualParameterDeclaration.get() instanceof Typeable) {
                    type = ((Typeable) actualParameterDeclaration.get()).getCanonicalType();
                } else if (actualParameterDeclaration.get() instanceof MethodVertex) {
                    type = ((MethodVertex) actualParameterDeclaration.get()).getReturnType();
                } else {
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

    private Optional<BaseSFVertex> getTypeDeclaration(ChainedVertex chainedVertex) {
        if (chainedVertex instanceof LiteralExpressionVertex) {
            return getTypeDeclaration((LiteralExpressionVertex<?>) chainedVertex);
        } else if (chainedVertex instanceof VariableExpressionVertex) {
            return getTypeDeclaration((VariableExpressionVertex) chainedVertex);
        } else if (chainedVertex instanceof MethodCallExpressionVertex) {
            return getTypeDeclaration((MethodCallExpressionVertex) chainedVertex);
        }
        return Optional.empty();
    }

    private Optional<BaseSFVertex> getTypeDeclaration(
            LiteralExpressionVertex<?> literalExpressionVertex) {
        // Literal expressions are their own type declaration.
        return Optional.of(literalExpressionVertex);
    }

    private Optional<BaseSFVertex> getTypeDeclaration(
            VariableExpressionVertex variableExpressionVertex) {
        Optional<BaseSFVertex> declaration = Optional.empty();
        // How we handle this variable expression depends on the nature of its reference expression.
        AbstractReferenceExpressionVertex abstractReferenceExpressionVertex =
                variableExpressionVertex.getReferenceExpression();

        if (abstractReferenceExpressionVertex instanceof EmptyReferenceExpressionVertex) {
            // An empty reference expression could be a reference to a local variable, or
            // a class property via an implicit `this`.
            // Check the former, then the latter if necessary.
            declaration = getMethodLevelDeclaration(variableExpressionVertex);
            if (!declaration.isPresent()) {
                declaration = getClassLevelDeclaration(variableExpressionVertex);
            }
        } else if (abstractReferenceExpressionVertex instanceof ReferenceExpressionVertex) {
            // TODO: REWRITE THIS PART
            // A non-empty reference means something of the form `x.y`.
            ReferenceExpressionVertex referenceExpressionVertex =
                    (ReferenceExpressionVertex) abstractReferenceExpressionVertex;
            // Check first for `this.property`.
            if (referenceExpressionVertex.getThisVariableExpression().isPresent()) {
                declaration = getClassLevelDeclaration(variableExpressionVertex);
            } else if (!referenceExpressionVertex.getChildren().isEmpty()
                    && referenceExpressionVertex.getChild(0)
                            instanceof MethodCallExpressionVertex) {
                // Then check for `methodCall().property`.
                MethodCallExpressionVertex nestedMethodCall = referenceExpressionVertex.getChild(0);
                // TODO: USE THIS OPTIONAL TO GET THE TYPE OF THE OBJECT, THEN GET THE TYPE OF THE
                // PROPERTY
                //   BEING REFERENCED.
                Optional<BaseSFVertex> methodHostClass = getTypeDeclaration(nestedMethodCall);
            } else if (referenceExpressionVertex.getNames().size() > 1) {
                return Optional.empty();
            } else {
                // Otherwise, get the declaration of property `y` on host object/class `x`.
                declaration =
                        getPropertyDeclarationOnHost(
                                variableExpressionVertex, referenceExpressionVertex);
            }
        }
        return declaration;
    }

    private Optional<BaseSFVertex> getTypeDeclaration(
            MethodCallExpressionVertex methodCallExpressionVertex) {
        Optional<BaseSFVertex> declaration = Optional.empty();
        // How we handle this method call expression depends on the nature of its reference
        // expression.
        AbstractReferenceExpressionVertex abstractReferenceExpressionVertex =
                methodCallExpressionVertex.getReferenceExpression();
        if (abstractReferenceExpressionVertex instanceof EmptyReferenceExpressionVertex) {
            // An empty reference expression means this is a reference to a class method,
            // using an implicit `this`.
            declaration = getClassLevelDeclaration(methodCallExpressionVertex);
        } else if (abstractReferenceExpressionVertex instanceof ReferenceExpressionVertex) {
            ReferenceExpressionVertex referenceExpressionVertex =
                    (ReferenceExpressionVertex) abstractReferenceExpressionVertex;
            if (referenceExpressionVertex.getThisVariableExpression().isPresent()) {
                // A `this` expression also means this is a reference to a class method.
                declaration = getClassLevelDeclaration(methodCallExpressionVertex);
            }
        }

        return declaration;
    }

    private <T extends BaseSFVertex & NamedVertex>
            Optional<BaseSFVertex> getVariableOrPropertyDeclaration(T variableExpression) {
        // First check for local variables.
        Optional<BaseSFVertex> declaration = getMethodLevelDeclaration(variableExpression);
        if (!declaration.isPresent()) {
            // Then check for class properties.
            declaration = getClassLevelDeclaration(variableExpression);
        }
        return declaration;
    }

    private <T extends BaseSFVertex & NamedVertex> Optional<BaseSFVertex> getMethodLevelDeclaration(
            T variable) {
        // Search this method for a matching variable/parameter declaration.
        MethodVertex parentMethod =
                variable.getParentMethod().orElseThrow(() -> new UnexpectedException(variable));
        return getDeclarationForNamedUsage(
                parentMethod.getId(),
                variable.getName(),
                NodeType.PARAMETER,
                NodeType.VARIABLE_DECLARATION);
    }

    private <T extends BaseSFVertex & NamedVertex> Optional<BaseSFVertex> getClassLevelDeclaration(
            T variable) {
        // Search this class for a field with that name.
        UserClassVertex parentClass =
                variable.getParentClass().orElseThrow(() -> new UnexpectedException(variable));
        return getDeclarationForNamedUsage(parentClass.getId(), variable.getName(), NodeType.FIELD);
    }

    private Optional<BaseSFVertex> getClassLevelDeclaration(
            MethodCallExpressionVertex methodCallExpressionVertex) {
        // Search this class for a field with that name.
        UserClassVertex parentClass =
                methodCallExpressionVertex
                        .getParentClass()
                        .orElseThrow(() -> new UnexpectedException(methodCallExpressionVertex));
        return getDeclarationForNamedUsage(
                parentClass.getId(), methodCallExpressionVertex.getMethodName(), NodeType.METHOD);
    }

    // TODO: REFACTOR THIS WHOLE METHOD.
    private Optional<BaseSFVertex> getPropertyDeclarationOnHost(
            VariableExpressionVertex variable, ReferenceExpressionVertex reference) {
        // The ReferenceExpression is the object/class on which the referenced variable resides.
        // Attempt to find the declaration of a matching object.
        Optional<BaseSFVertex> hostObjectDeclaration = getVariableOrPropertyDeclaration(reference);

        // Identify the class that hosts the property we want.
        String hostType;
        if (hostObjectDeclaration.isPresent() && hostObjectDeclaration.get() instanceof Typeable) {
            // If we found a declaration for the host object, then its type
            // is whatever it was declared as.
            hostType = ((Typeable) hostObjectDeclaration.get()).getCanonicalType();
        } else {
            // If we found no such declaration, see whether we can find a class
            // whose name matches, in which case the reference is to a static
            // property of that class.
            hostType = reference.getName();
        }
        // Now, get the vertex where that class is declared.
        long classId =
                (long) g.V().where(H.has(NodeType.USER_CLASS, Schema.NAME, hostType)).id().next();
        // See if we can find a declaration of the desired property on this class.
        return getDeclarationForNamedUsage(classId, variable.getName(), NodeType.FIELD);
    }

    /**
     * @param parentVertexId - The ID of the root vertex for this search, which will be either a
     *     class vertex or a method vertex.
     * @param referencedName - The name by which the desired property is referenced.
     * @param nodeTypes - The node types that could possibly be the type declaration for the used
     *     vertex.
     * @return
     */
    private Optional<BaseSFVertex> getDeclarationForNamedUsage(
            long parentVertexId, String referencedName, String... nodeTypes) {
        List<BaseSFVertex> vertices =
                SFVertexFactory.loadVertices(
                        g,
                        g.V(parentVertexId)
                                .repeat(__.out(Schema.CHILD))
                                .emit()
                                .where(
                                        H.has(
                                                Arrays.asList(nodeTypes),
                                                Schema.NAME,
                                                referencedName)));
        if (vertices.isEmpty()) {
            // If we found no vertices, return an empty optional.
            return Optional.empty();
        } else if (vertices.size() == 1) {
            // If we found one typeable vertex, return it.
            return Optional.of(vertices.get(0));
        } else {
            // If we found neither of those things, we have a problem.
            throw new UnexpectedException(referencedName);
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
