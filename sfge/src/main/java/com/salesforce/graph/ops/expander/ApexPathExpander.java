package com.salesforce.graph.ops.expander;

import com.salesforce.Collectible;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.ProgrammingException;
import com.salesforce.exception.SfgeInterruptedException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ApexPathVertexMetaInfo;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.ops.ClassUtil;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.ops.EngineDirectiveUtil;
import com.salesforce.graph.ops.GraphUtil;
import com.salesforce.graph.ops.MethodUtil;
import com.salesforce.graph.ops.directive.EngineDirective;
import com.salesforce.graph.ops.directive.EngineDirectiveCommand;
import com.salesforce.graph.ops.expander.switches.ApexPathCaseStatementExcluder;
import com.salesforce.graph.symbols.AbstractClassScope;
import com.salesforce.graph.symbols.ClassInstanceScope;
import com.salesforce.graph.symbols.ClassStaticScope;
import com.salesforce.graph.symbols.ClassStaticScopeProvider;
import com.salesforce.graph.symbols.ContextProviders;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.EngineDirectiveContext;
import com.salesforce.graph.symbols.EngineDirectiveContextProvider;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BlockStatementVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.graph.vertex.UserClassVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import com.salesforce.graph.vertex.VertexPredicate;
import com.salesforce.graph.vertex.WhenBlockVertex;
import com.salesforce.graph.visitor.PathVertex;
import com.salesforce.graph.visitor.VertexPredicateVisitor;
import com.salesforce.rules.AbstractPathBasedRule;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Responsible for tracking the work done in expanding an ApexPath and the method calls contained in
 * that path. An ApexPath initially begins with the vertices of a single method. This class is
 * responsible for resolving which {@link MethodVertex} an {@link InvocableVertex} resolves to. A
 * {@link MethodPathForkedException} is thrown if the Method has more than one path. This results in
 * N new ApexPathExpanders where N is the number of paths in the method. This continues until all
 * possible paths have been mapped. This class throws exceptions at various points in time which are
 * caught by the {@link ApexPathUtil} to either collapse or fork the current path. This class calls
 * into {@link ApexPathCollapser} in order to provide information which may also result in the
 * current path getting collapsed.
 */
final class ApexPathExpander implements ClassStaticScopeProvider, EngineDirectiveContextProvider {
    private static final Logger LOGGER = LogManager.getLogger(ApexPathExpander.class);

    /** Used to give each object a unique id */
    private static final AtomicLong ID_GENERATOR = new AtomicLong();

    /** Graph which owns the path */
    private final GraphTraversalSource g;

    /**
     * Object which maintains state about all ApexPathExpanders that are related to the first path
     * which is being expanded.
     */
    private final ApexPathCollapser apexPathCollapser;

    /** Dynamically generated id used to establish object equality */
    private final Long id;

    /**
     * The vertex that caused a {@link MethodPathForkedException} to be thrown and the ForkEvent
     * that was included in the exception. This ForkEvent creates a common ancestry for all
     * ApexPathExpanders that were a result of the same ForkEvent. These ancestors are the set
     * considered when collapsing paths based on returne results.
     */
    private final HashMap<PathVertex, ForkEvent> forkEvents;

    /** Stores the result returned from a method if that method caused a fork */
    private final HashMap<PathVertex, Optional<ApexValue<?>>> forkResults;

    /**
     * Path that contains the first vertex which will be traversed. It starts out as the original
     * path to traverse, paths for class initialization paths are pushed onto this stack before they
     * are visited, and popped when completed.
     */
    private final Stack<ApexPath> topMostPath;

    /** Configuration for this expander */
    private final ApexPathExpanderConfig config;

    /**
     * Keep track of whether {@link BaseSFVertex#visit(SymbolProviderVertexVisitor)} was invoked and
     * what the result for the PathVertex was. Used to avoid calling the same method twice after a
     * path is forked.
     */
    private final HashMap<PathVertex, Boolean> visitCalled;

    /**
     * Keep track of whether {@link BaseSFVertex#afterVisit(SymbolProviderVertexVisitor)} was
     * invoked and what the result for the PathVertex was. Used to avoid calling the same method
     * twice after a path is forked.
     */
    private final HashSet<PathVertex> afterVisitCalled;

    /**
     * Keep track of whether {@link SymbolProviderVertexVisitor#beforeMethodCall(InvocableVertex,
     * MethodVertex)} was invoked and what the result for the PathVertex was. Used to avoid calling
     * the same method twice after a path is forked.
     */
    private final HashSet<PathInvocableCall> beforeMethodCalled;

    /**
     * Keep track of whether {@link SymbolProviderVertexVisitor#afterMethodCall(InvocableVertex,
     * MethodVertex)}} was invoked and what the result for the PathVertex was. Used to avoid calling
     * the same method twice after a path is forked.
     */
    private final HashSet<PathInvocableCall> afterMethodCalled;

    /**
     * The first vertex in each path currently on the call stack. Used to avoid recursive calls. An
     * exception will be thrown if inserting into this stack would create a duplicate entry.
     */
    private final Stack<BlockStatementVertex> pathBlockStatements;

    /**
     * Keep track of the static classes that are in a state of initialization. This prevents
     * recursive reinitialization of those classes if they are referred to by other classes or
     * themselves in the case of singletons.
     */
    private final TreeSet<String> currentlyInitializingStaticClasses;

    /**
     * Track classes whose static scopes have been initialized. This would help double visiting some
     * nodes.
     */
    private final TreeSet<String> alreadyInitializedStaticClasses;

    /** The symbol provider that corresponds to the {@link #topMostPath} */
    private /*finalTODO Add clear method to SymbolProviderVertexVisitor*/
    SymbolProviderVertexVisitor symbolProviderVisitor;

    /** Maintain information that is then set on the ApexPath */
    private final ApexPathVertexMetaInfo apexPathVertexMetaInfo;

    /** Map used to implement ClassStaticScopeProvider */
    private final TreeMap<String, ClassStaticScope> classStaticScopes;

    /**
     * Initial scope that is instantiated based on the vertex passed to {@link #start(BaseSFVertex)}
     */
    private SymbolProvider startScope;

    private final EngineDirectiveContext engineDirectiveContext;

    private final int hash;

    ApexPathExpander(
            GraphTraversalSource g,
            ApexPathCollapser apexPathCollapser,
            ApexPath topMostPath,
            ApexPathExpanderConfig config) {
        this.id = ID_GENERATOR.incrementAndGet();
        this.hash = Objects.hashCode(this.id);
        this.g = g;
        this.apexPathCollapser = apexPathCollapser;
        this.forkEvents = new LinkedHashMap<>();
        this.forkResults = new HashMap<>();
        this.topMostPath = new Stack<>();
        this.topMostPath.push(topMostPath);
        this.config = config;
        this.symbolProviderVisitor =
                new DefaultSymbolProviderVertexVisitor(
                        g, config.instantiateClassScope(g).orElse(null));
        this.visitCalled = new HashMap<>();
        this.afterVisitCalled = new HashSet<>();
        this.beforeMethodCalled = new HashSet<>();
        this.afterMethodCalled = new HashSet<>();
        this.pathBlockStatements = new Stack<>();
        this.apexPathVertexMetaInfo = new ApexPathVertexMetaInfo();
        this.classStaticScopes = CollectionUtil.newTreeMap();
        this.engineDirectiveContext = new EngineDirectiveContext();
        this.currentlyInitializingStaticClasses = CollectionUtil.newTreeSet();
        this.alreadyInitializedStaticClasses = CollectionUtil.newTreeSet();
    }

    /**
     * Constructor used when a path forks and a new path needs to be created.
     *
     * @param other original path which forks
     * @param ex exception that signaled the fork. Contains information necessary to create the fork
     * @param pathIndex the Exception contains an array of forks, indicates which fork to use
     */
    ApexPathExpander(ApexPathExpander other, MethodPathForkedException ex, int pathIndex) {
        this.id = ID_GENERATOR.incrementAndGet();
        this.hash = Objects.hashCode(this.id);
        this.g = other.g;
        this.apexPathCollapser = other.apexPathCollapser;
        this.forkEvents = CloneUtil.cloneHashMap(other.forkEvents);
        PathVertex pathVertex = ex.getForkEvent().getPathVertex();
        this.forkEvents.put(pathVertex, ex.getForkEvent());
        this.forkResults = CloneUtil.cloneHashMap(other.forkResults);
        if (this.forkResults.containsKey(pathVertex)) {
            throw new UnexpectedException("Duplicate pathVertex. vertex=" + pathVertex);
        }
        this.forkResults.put(pathVertex, null);
        this.symbolProviderVisitor =
                (SymbolProviderVertexVisitor)
                        ((DeepCloneable) other.symbolProviderVisitor).deepClone();

        // The clone map will allow us to correlate the old paths to the newly cloned paths
        this.topMostPath = CloneUtil.cloneStack(other.topMostPath);
        this.config = CloneUtil.cloneImmutable(other.config);

        // Find the method call that caused the fork and hook up the path
        BaseSFVertex topLevelVertex = ex.getTopLevelVertex();
        InvocableVertex invocable = ex.getInvocable();
        Pair<BaseSFVertex, InvocableVertex> topLevelPair = Pair.of(topLevelVertex, invocable);
        ApexPath methodPath = ex.getPaths().get(pathIndex);
        if (!methodPath.getMethodVertex().isPresent()) {
            throw new UnexpectedException("Wrong constructor");
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "Creating fork. pathIndex="
                            + pathIndex
                            + ", of="
                            + ex.getPaths().size()
                            + ", method="
                            + methodPath.getMethodVertex().get().toSimpleString());
        }
        ApexPath pathWithFork =
                this.getTopMostPath().getPathWithStableId(ex.getPathWithFork().getStableId());
        pathWithFork.putInvocableExpression(topLevelPair, methodPath);
        if (methodPath.endsInException()) {
            // Configure the path to end in an exception and set all of the instance properties to
            // empty since we will
            // not be processing any further.
            pathWithFork.putPathEndsInException(topLevelVertex);
            this.visitCalled = new HashMap<>();
            this.beforeMethodCalled = new HashSet<>();
            this.afterVisitCalled = new HashSet<>();
            this.afterMethodCalled = new HashSet<>();
            this.pathBlockStatements = new Stack<>();
            this.engineDirectiveContext = new EngineDirectiveContext();
        } else {
            this.visitCalled = CloneUtil.cloneHashMap(other.visitCalled);
            this.afterVisitCalled = CloneUtil.cloneHashSet(other.afterVisitCalled);
            this.beforeMethodCalled = CloneUtil.cloneHashSet(other.beforeMethodCalled);
            this.afterMethodCalled = CloneUtil.cloneHashSet(other.afterMethodCalled);
            // Intentionally not cloning, these will be regenerated as the path is re-walked after a
            // fork
            this.pathBlockStatements = new Stack<>();
            this.engineDirectiveContext = CloneUtil.clone(other.engineDirectiveContext);
        }
        this.apexPathVertexMetaInfo = CloneUtil.clone(other.apexPathVertexMetaInfo);
        this.classStaticScopes = CloneUtil.cloneTreeMap(other.classStaticScopes);
        // TODO: too many casts
        this.startScope = (SymbolProvider) CloneUtil.clone((DeepCloneable) other.startScope);
        this.currentlyInitializingStaticClasses =
                CloneUtil.cloneTreeSet(other.currentlyInitializingStaticClasses);
        this.alreadyInitializedStaticClasses =
                CloneUtil.cloneTreeSet(other.alreadyInitializedStaticClasses);
    }

    /**
     * Release all of the intermediate data that is no longer needed in order to relieve memory
     * pressure.
     */
    void finished() {
        this.visitCalled.clear();
        this.afterVisitCalled.clear();
        this.beforeMethodCalled.clear();
        this.afterMethodCalled.clear();
        this.pathBlockStatements.clear();
        this.classStaticScopes.clear();
        // TODO: Add clear method
        this.symbolProviderVisitor = null;
        this.engineDirectiveContext.clear();
        // TODO: Add clear method
        this.startScope = null;
    }

    /** Get a static class scope. Reuse any existing ones in order to maintain state. */
    @Override
    @SuppressWarnings(
            "PMD.GuardLogStatement") // Top level logger check would guard the inner log line as
    // well
    public Optional<ClassStaticScope> getClassStaticScope(String className) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("getClassStaticScope. className=" + className);
            if (!classStaticScopes.containsKey(className)) {
                LOGGER.trace(
                        "Class not found. className={}, initializedClasses={}",
                        className,
                        classStaticScopes.keySet());
            }
        }
        return Optional.ofNullable(classStaticScopes.get(className));
    }

    @Override
    public EngineDirectiveContext getEngineDirectiveContext() {
        return engineDirectiveContext;
    }

    /**
     * Obtain a {@link ClassStaticScope} that corresponds to {@code className} and initialize it if
     * required.
     */
    @SuppressWarnings(
            "PMD.DoNotThrowExceptionInFinally") // Exception thrown only during coding phase
    void initializeClassStaticScope(String className)
            throws PathExcludedException, MethodPathForkedException,
                    ReturnValueInvalidCollapsedException, RecursionDetectedException,
                    PathCollapsedException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("initializeClassStaticScope. className=" + className);
        }
        ClassStaticScope classStaticScope =
                classStaticScopes.computeIfAbsent(
                        className, k -> ClassStaticScope.getOptional(g, className).orElse(null));
        if (classStaticScope != null) {
            final String fullClassName = classStaticScope.getClassName();
            if (currentlyInitializingStaticClasses.contains(fullClassName)) {
                if (LOGGER.isInfoEnabled()) {
                    // The class is already being initialized, let that initialization complete
                    LOGGER.info("Skipping recursive initialization. className=" + fullClassName);
                }
                return;
            }
            try {
                currentlyInitializingStaticClasses.add(fullClassName);
                Collectible<ApexPath> apexPath = null;
                if (classStaticScope.getState().equals(AbstractClassScope.State.UNINITIALIZED)) {
                    // Nothing has occurred yet, setup the paths and push the class onto the scope
                    // stack
                    classStaticScope.setState(AbstractClassScope.State.INITIALIZING);
                    apexPath =
                            getTopMostPath()
                                    .getStaticInitializationPath(fullClassName)
                                    .orElse(null);
                    if (apexPath == null) {
                        apexPath =
                                ClassStaticScope.getInitializationPath(g, fullClassName)
                                        .orElse(null);
                        getTopMostPath()
                                .putStaticInitializationPath(
                                        className,
                                        apexPath != null ? apexPath : ApexPath.NULL_VALUE);
                        symbolProviderVisitor.pushScope(classStaticScope);
                    }
                } else if (classStaticScope
                        .getState()
                        .equals(AbstractClassScope.State.INITIALIZING)) {
                    // The class has been previously pushed onto the scope stack but a forked
                    // exception was thrown.
                    // Retrieve the path and continue walking it.
                    apexPath = getTopMostPath().getStaticInitializationPath(className).get();
                }

                if (!classStaticScope.getState().equals(AbstractClassScope.State.INITIALIZED)
                        && !alreadyInitializedStaticClasses.contains(fullClassName)) {
                    if (apexPath != null && apexPath.getCollectible() != null) {
                        visit(apexPath.getCollectible());
                    }
                    symbolProviderVisitor.popScope(classStaticScope);
                    classStaticScope.setState(AbstractClassScope.State.INITIALIZED);
                    alreadyInitializedStaticClasses.add(fullClassName);
                }
            } finally {
                if (!currentlyInitializingStaticClasses.remove(fullClassName)
                        && alreadyInitializedStaticClasses.contains(fullClassName)) {
                    throw new ProgrammingException(
                            "Set did not contain class. values="
                                    + currentlyInitializingStaticClasses);
                }
            }
        }
    }

    /**
     * Visit all vertices in a path. Maintain {@link #pathBlockStatements} which avoids recursion.
     */
    void visit(ApexPath path)
            throws PathExcludedException, MethodPathForkedException, RecursionDetectedException,
                    ReturnValueInvalidCollapsedException, PathCollapsedException {
        boolean push = path.firstVertex() instanceof BlockStatementVertex;
        List<EngineDirective> engineDirectives = null;
        if (push) {
            pathBlockStatements.push((BlockStatementVertex) path.firstVertex());
            MethodVertex methodVertex = path.getMethodVertex().get();
            engineDirectives =
                    methodVertex.getEngineDirectives(EngineDirectiveCommand.DISABLE_STACK);
            if (!engineDirectives.isEmpty()) {
                engineDirectiveContext.push(engineDirectives);
            }
        }
        for (BaseSFVertex vertex : path.verticesInCurrentMethod()) {
            PathVertex pathVertex = new PathVertex(path, vertex);
            // We can skip the vertex if #afterVisit has already been called
            if (!afterVisitCalled.contains(pathVertex)) {
                visit(path, vertex);
            }
        }
        if (push) {
            if (!engineDirectives.isEmpty()) {
                engineDirectiveContext.pop(engineDirectives);
            }
            pathBlockStatements.pop();
        }
    }

    /** Get the path on the top of the stack. */
    ApexPath getTopMostPath() {
        return topMostPath.peek();
    }

    ApexPathVertexMetaInfo getApexPathVertexMetaInfo() {
        return apexPathVertexMetaInfo;
    }

    SymbolProviderVertexVisitor getSymbolProviderVisitor() {
        return symbolProviderVisitor;
    }

    Long getId() {
        return id;
    }

    Optional<ApexValue<?>> getForkResult(PathVertex pathVertex) {
        return forkResults.get(pathVertex);
    }

    Map<PathVertex, ForkEvent> getForkEvents() {
        return forkEvents;
    }

    /** */
    SymbolProvider start(BaseSFVertex vertex) {
        if (startScope == null) {
            startScope = symbolProviderVisitor.start(vertex);
        }
        return startScope;
    }

    @SuppressWarnings("PMD.PreserveStackTrace") // Logic relies on throwing new exceptions
    private void visit(ApexPath path, BaseSFVertex vertex)
            throws PathExcludedException, MethodPathForkedException, RecursionDetectedException,
                    ReturnValueInvalidCollapsedException, PathCollapsedException {
        if (ContextProviders.CLASS_STATIC_SCOPE.get() != this) {
            throw new UnexpectedException("ClassStaticScopeContexts don't match");
        }

        if (Thread.interrupted()) {
            throw new SfgeInterruptedException();
        }

        PathInvocableCall pathInvocableCall = null;
        PathVertex pathVertex = new PathVertex(path, vertex);

        boolean visitChildren =
                visitCalled.computeIfAbsent(
                        pathVertex,
                        k -> {
                            BaseSFVertex predicateVertex = pathVertex.getVertex();
                            for (VertexPredicate predicate : config.getVertexPredicates()) {
                                // Keep track of the vertex if any predicate is interested. The
                                // result is stored as a PredicateMatch,
                                // allowing the caller to know which predicate is interested in
                                // which vertex without the need to call
                                // #test a second time
                                if (predicate.test(predicateVertex)) {
                                    predicate.accept(
                                            new VertexPredicateVisitor() {
                                                @Override
                                                public void defaultVisit(
                                                        VertexPredicate predicate) {
                                                    // TODO: This is a test only path
                                                    apexPathVertexMetaInfo.addVertex(
                                                            predicate, pathVertex);
                                                }

                                                @Override
                                                public void visit(AbstractPathBasedRule rule) {
                                                    if (EngineDirectiveUtil.isRuleEnabled(
                                                            vertex, rule)) {
                                                        apexPathVertexMetaInfo.addVertex(
                                                                predicate, pathVertex);
                                                    } else {
                                                        if (LOGGER.isInfoEnabled()) {
                                                            LOGGER.info(
                                                                    rule.getDescriptor().getName()
                                                                            + " is disabled for vertex="
                                                                            + predicateVertex);
                                                        }
                                                    }
                                                }
                                            });
                                }
                            }
                            return vertex.visit(symbolProviderVisitor);
                        });

        if (visitChildren) {
            for (BaseSFVertex child : vertex.getChildren()) {
                visit(path, child);
            }
        }

        if (vertex instanceof VariableExpressionVertex.Single) {
            VariableExpressionVertex.Single variableExpression =
                    (VariableExpressionVertex.Single) vertex;
            String symbolicName = variableExpression.getSymbolicName().orElse(null);
            String fullName = variableExpression.getFullName();
            // This might be a reference to static reference in another class
            if (fullName.contains(".") && symbolicName != null) {
                initializeClassStaticScope(symbolicName);
            }
        }

        ApexPath methodPath = null;
        InvocableVertex invocable = null;
        if (vertex instanceof InvocableVertex) {
            invocable = (InvocableVertex) vertex;
            pathInvocableCall = new PathInvocableCall(path, invocable);
            // Only try to resolve the method if it hasn't already been called
            if (!afterMethodCalled.contains(pathInvocableCall)) {
                methodPath = resolveMethodCall(path, invocable).orElse(null);
                if (methodPath != null) {
                    try {
                        // TODO: Can this happen in other cases?
                        if (invocable instanceof MethodCallExpressionVertex) {
                            checkForRecursion(methodPath, (MethodCallExpressionVertex) invocable);
                        }
                    } catch (RecursionDetectedException ex) {
                        // Set the path as ending in recursion and rethrow the exception
                        BaseSFVertex topLevelVertex =
                                GraphUtil.getControlFlowVertex(g, vertex)
                                        .orElseThrow(() -> new UnexpectedException(ex));
                        Pair<BaseSFVertex, MethodCallExpressionVertex> topLevelPair =
                                Pair.of(topLevelVertex, (MethodCallExpressionVertex) invocable);
                        path.putPathEndsInRecursion(topLevelPair);
                        throw ex;
                    }
                }
            }
        }

        // Follow the path if it exists and the current path isn't the terminal method call
        if (methodPath != null) {
            // Add the initializer paths before storing the beforeMethodCalled. This will allow us
            // to start off at
            // the same place if a forked path exception is thrown
            final MethodVertex method = methodPath.getMethodVertex().get();
            final String methodDefiningType = method.getDefiningType();
            UserClassVertex userClass = null;
            if (vertex instanceof NewObjectExpressionVertex) {
                final NewObjectExpressionVertex newObjectExpression =
                        (NewObjectExpressionVertex) vertex;
                userClass = ClassUtil.getUserClass(g, newObjectExpression).orElse(null);
                if (userClass != null) {
                    initializeClassStaticScope(userClass.getDefiningType());

                    // Map the NewObjectExpressionVertex to the instance initialization path for the
                    // class that corresponds
                    // the the NewObjectExpressionVertex
                    Collectible<ApexPath> apexPath =
                            path.getNewInstanceToInitializationPath(newObjectExpression)
                                    .orElse(null);
                    if (apexPath == null) {
                        apexPath =
                                ClassInstanceScope.getInitializationPath(
                                                g, userClass.getDefiningType())
                                        .orElse(null);
                        path.putNewObjectExpression(
                                newObjectExpression,
                                apexPath != null ? apexPath : ApexPath.NULL_VALUE);
                    }
                }
            } else {
                initializeClassStaticScope(methodDefiningType);
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "Starting method path. methodCall="
                                + vertex
                                + ", firstVertex="
                                + methodPath.firstVertex());
            }

            if (beforeMethodCalled.add(pathInvocableCall)) {
                symbolProviderVisitor.beforeMethodCall(
                        invocable, methodPath.getMethodVertex().get());
            }

            if (vertex instanceof NewObjectExpressionVertex && userClass != null) {
                NewObjectExpressionVertex newObjectExpression = (NewObjectExpressionVertex) vertex;
                Collectible<ApexPath> apexPath =
                        path.getNewInstanceToInitializationPath(newObjectExpression).orElse(null);
                if (apexPath != null && apexPath.getCollectible() != null) {
                    visit(apexPath.getCollectible());
                }
            }

            visit(methodPath);

            if (afterMethodCalled.add(pathInvocableCall)) {
                Optional<ApexValue<?>> lastReturnValue =
                        symbolProviderVisitor.afterMethodCall(
                                invocable, methodPath.getMethodVertex().get());
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(
                            "lastReturnValue="
                                    + lastReturnValue.orElse(null)
                                    + ", methodCall"
                                    + invocable);
                }
                // Ask the collapsers if the invoked method returned a useful value. If not, the
                // path will not be considered
                for (ApexReturnValuePathCollapser collapser : config.getReturnValueCollapsers()) {
                    boolean isValid = false;
                    try {
                        collapser.checkValid(methodPath.getMethodVertex().get(), lastReturnValue);
                        isValid = true;
                    } catch (ReturnValueInvalidException ex) {
                        if (methodPath.endsInException()) {
                            throw new UnexpectedException(methodPath);
                        }
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(ex.getMessage());
                        }
                    }

                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(
                                "Path isValid="
                                        + isValid
                                        + ", method="
                                        + methodPath.getMethodVertex()
                                        + ", lastReturnValue="
                                        + lastReturnValue.orElse(null)
                                        + ", collapser="
                                        + collapser.getClass().getSimpleName());
                    }

                    if (!isValid) {
                        // TODO: This might be too aggressive, it could remove the method call, but
                        // not remove the
                        // whole path
                        throw new ReturnValueInvalidCollapsedException(
                                forkEvents.get(pathVertex), methodPath, lastReturnValue);
                    }
                }
                if (forkResults.containsKey(pathVertex)) {
                    if (forkResults.put(pathVertex, lastReturnValue) != null) {
                        throw new UnexpectedException(
                                "Duplicated return result. vertex=" + pathVertex);
                    }
                    apexPathCollapser.resultReturned(
                            this, forkEvents.get(pathVertex), lastReturnValue);
                }
            }
        }

        if (afterVisitCalled.add(pathVertex)) {
            vertex.afterVisit(symbolProviderVisitor);
            if (vertex instanceof StandardConditionVertex.Negative
                    || vertex instanceof StandardConditionVertex.Positive) {
                // Filter out paths where the standard condition is guaranteed not to execute
                for (ApexPathStandardConditionExcluder conditionExcluder :
                        config.getConditionExcluders()) {
                    // This will throw an exception if the path should be excluded
                    conditionExcluder.exclude(
                            (StandardConditionVertex) vertex,
                            symbolProviderVisitor.getSymbolProvider());
                }
                // Allow the constrainers to update any variables that were referenced in the
                // StandardCondition. This needs
                // to happen after the excluders, because the excluders will remove any invalid
                // paths based on previously
                // added constraints
                // IMPORTANT: This code doesn't exist in ApexPathWalker. The constraints aren't
                // available to the Rules
                for (ApexValueConstrainer valueConstrainer : config.getValueConstrainers()) {
                    valueConstrainer.constrain(
                            (StandardConditionVertex) vertex,
                            symbolProviderVisitor.getSymbolProvider());
                }
            } else if (vertex instanceof WhenBlockVertex) {
                for (ApexPathCaseStatementExcluder caseStatementExcluder :
                        config.getCaseStatementExcluders()) {
                    // This will throw an exception if the path should be excluded
                    caseStatementExcluder.exclude(
                            (WhenBlockVertex) vertex, symbolProviderVisitor.getSymbolProvider());
                }
            }
            symbolProviderVisitor.popScope(vertex);
        }
    }

    /**
     * Throw an exception if recursion is detected. Recursion is detected by keeping a stack of the
     * first vertex in all paths currently on the call stack.
     */
    private void checkForRecursion(
            ApexPath methodPath, MethodCallExpressionVertex methodCallExpression)
            throws RecursionDetectedException {
        BaseSFVertex firstVertex = methodPath.firstVertex();
        if (pathBlockStatements.contains(firstVertex)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "Skipping recursive path. methodCall="
                                + methodCallExpression
                                + ", firstVertex="
                                + firstVertex
                                + ", callstack="
                                + pathBlockStatements);
            }
            throw new RecursionDetectedException(methodPath, methodCallExpression);
        }
    }

    /**
     * Returns a result if the method resolves to 0 or 1 paths, else throws an exception indicating
     * that the path needs to fork.
     */
    private Optional<ApexPath> resolveMethodCall(ApexPath path, InvocableVertex invocable)
            throws MethodPathForkedException, RecursionDetectedException {
        Optional<ApexPath> result = path.resolveInvocableCall(invocable);

        if (!result.isPresent()) {
            List<ApexPath> paths =
                    MethodUtil.getPaths(
                            g,
                            (ChainedVertex) invocable,
                            symbolProviderVisitor.getSymbolProvider());

            if (!paths.isEmpty()) {
                BaseSFVertex topLevelVertex =
                        GraphUtil.getControlFlowVertex(g, (BaseSFVertex) invocable)
                                .orElseThrow(() -> new UnexpectedException(invocable));

                if (paths.size() > 1) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(
                                "Forking. invocableExpression="
                                        + invocable
                                        + ", paths="
                                        + paths.size());
                    }
                    throw new MethodPathForkedException(
                            this, path, topLevelVertex, invocable, paths);
                } else {
                    // The path resolves to a method with a single path. Update the existing path to
                    // point to the
                    // method's path and return the method's path.
                    ApexPath resolvedPath = paths.get(0);
                    path.putInvocableExpression(Pair.of(topLevelVertex, invocable), resolvedPath);
                    result = Optional.of(resolvedPath);
                }
            }
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApexPathExpander that = (ApexPathExpander) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
