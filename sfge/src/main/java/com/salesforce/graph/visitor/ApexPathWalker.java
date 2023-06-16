package com.salesforce.graph.visitor;

import com.salesforce.Collectible;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.SfgeInterruptedException;
import com.salesforce.exception.SfgeRuntimeException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.MethodUtil;
import com.salesforce.graph.ops.expander.RecursionDetectedException;
import com.salesforce.graph.symbols.AbstractClassInstanceScope;
import com.salesforce.graph.symbols.AbstractClassScope;
import com.salesforce.graph.symbols.ClassStaticScope;
import com.salesforce.graph.symbols.ClassStaticScopeProvider;
import com.salesforce.graph.symbols.CloningSymbolProvider;
import com.salesforce.graph.symbols.ContextProviders;
import com.salesforce.graph.symbols.MethodInvocationScope;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
import com.salesforce.graph.vertex.ThrowStatementVertex;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.salesforce.rules.ops.methodpath.MethodPathListener;
import com.salesforce.rules.ops.methodpath.NoOpMethodPathListenerImpl;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public final class ApexPathWalker implements ClassStaticScopeProvider {
    private static final Logger LOGGER = LogManager.getLogger(ApexPathWalker.class);

    private final GraphTraversalSource g;
    private final ApexPath topMostPath;
    private final PathVertexVisitor visitor;
    private final SymbolProviderVertexVisitor symbolProviderVisitor;
    private final SymbolProvider symbolProvider;
    private final MethodPathListener methodPathListener;
    /** Map used to implement ClassStaticScopeProvider */
    private final TreeMap<String, ClassStaticScope> classStaticScopes;

    private ApexPathWalker(
        GraphTraversalSource g,
        ApexPath topMostPath,
        PathVertexVisitor visitor,
        SymbolProviderVertexVisitor symbolProviderVisitor,
        SymbolProvider symbolProvider) {
        this(g, topMostPath, visitor, symbolProviderVisitor, NoOpMethodPathListenerImpl.get(), symbolProvider);
    }

    public ApexPathWalker(
        GraphTraversalSource g,
        ApexPath topMostPath,
        PathVertexVisitor visitor,
        SymbolProviderVertexVisitor symbolProviderVisitor,
        MethodPathListener methodPathListener, SymbolProvider symbolProvider) {
        this.g = g;
        this.topMostPath = topMostPath;
        this.visitor = visitor;
        this.symbolProviderVisitor = symbolProviderVisitor;
        this.methodPathListener = methodPathListener;
        this.symbolProvider = symbolProvider;
        this.classStaticScopes = CollectionUtil.newTreeMap();
    }

    /**
     * Delegates to {@link #walkPath(GraphTraversalSource, ApexPath, PathVertexVisitor,
     * SymbolProviderVertexVisitor, SymbolProvider)} wrapping {@link
     * SymbolProviderVertexVisitor#getSymbolProvider()} in a {@link CloningSymbolProvider}.
     */
    public static void walkPath(
            GraphTraversalSource g,
            ApexPath path,
            PathVertexVisitor visitor,
            SymbolProviderVertexVisitor symbolVisitor) {
        walkPath(
                g,
                path,
                visitor,
                symbolVisitor,
                new CloningSymbolProvider(symbolVisitor.getSymbolProvider()));
    }

    /**
     * Walk the given path with {@code visitor}, {@code symbolVisitor}, and {@code symbolProvider}.
     */
    public static void walkPath(
        GraphTraversalSource g,
        ApexPath path,
        PathVertexVisitor visitor,
        SymbolProviderVertexVisitor symbolVisitor,
        SymbolProvider symbolProvider) {
        walkPath(g, path, visitor, symbolVisitor, NoOpMethodPathListenerImpl.get(), symbolProvider);
    }

    /**
     * Walk the given path with {@code visitor}, {@code symbolVisitor}, {@code methodPathListener} and {@code symbolProvider}.
     */
    public static void walkPath(
        GraphTraversalSource g,
        ApexPath path,
        PathVertexVisitor visitor,
        SymbolProviderVertexVisitor symbolVisitor,
        MethodPathListener methodPathListener,
        SymbolProvider symbolProvider) {
        final ApexPathWalker apexPathWalker =
                new ApexPathWalker(g, path, visitor, symbolVisitor, methodPathListener, symbolProvider);
        try {
            ContextProviders.CLASS_STATIC_SCOPE.push(apexPathWalker);
            apexPathWalker.walk(path);
        } catch (ThrowStatementVertexVisitedException ex) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Path terminated with exception. vertex=" + ex.vertex);
            }
        } finally {
            ContextProviders.CLASS_STATIC_SCOPE.pop();
        }
    }

    private void walk(ApexPath path) {
        StopWatch stopWatch = StopWatch.createStarted();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting. id=" + path.getStableId());
        }
        BaseSFVertex firstVertex = path.firstVertex();
        SymbolProvider started = symbolProviderVisitor.start(firstVertex);
        // Use a CloningSymbolProvider because the visitors want a point in time snapshot of
        // ApexValues, not the mutated state
        if (started instanceof AbstractClassScope) {
            AbstractClassScope abstractClassScope = (AbstractClassScope) started;
            // The static initializer should be walked before any method is invoked
            getClassStaticScope(abstractClassScope.getClassName());
            if (abstractClassScope instanceof AbstractClassInstanceScope) {
                // Walk the initialization and constructor paths if they exist
                final AbstractClassInstanceScope classInstanceScope =
                        (AbstractClassInstanceScope) abstractClassScope;
                final Collectible<ApexPath> initializationPath =
                        topMostPath.getInstanceInitializationPath().orElse(null);
                if (initializationPath != null && initializationPath.getCollectible() != null) {
                    visit(initializationPath.getCollectible());
                }
                final ApexPath constructorCollectible =
                        topMostPath.getConstructorPath().orElse(null);
                if (constructorCollectible != null) {
                    final ApexPath apexPath = constructorCollectible.getCollectible();
                    final MethodInvocationScope methodInvocationScope =
                            MethodUtil.getIndeterminantMethodInvocationScope(
                                    apexPath.getMethodVertex().get());
                    classInstanceScope.pushMethodInvocationScope(methodInvocationScope);
                    try {
                        visit(apexPath);
                    } finally {
                        classInstanceScope.popMethodInvocationScope(null);
                    }
                }
            }
        }
        visit(path);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "Finished. id="
                            + path.getStableId()
                            + ", time(milliseconds)="
                            + stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }

    private void visit(ApexPath path) {
        // A path is not considered a child, the first vertex is always visited
        visit(path, true, true);
    }

    private void visit(ApexPath path, boolean callSymbolProvider, boolean callVisitor) {
        for (BaseSFVertex vertex : path.verticesInCurrentMethod()) {
            visit(path, vertex, callSymbolProvider, callVisitor);
        }
    }

    private void visit(
            ApexPath path, BaseSFVertex vertex, boolean callSymbolProvider, boolean callVisitor) {
        if (Thread.interrupted()) {
            throw new SfgeInterruptedException();
        }

        if (!callSymbolProvider && !callVisitor) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Skipping vertex=" + vertex);
            }
            return;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Visiting vertex=" + vertex);
        }

        // Read ahead to determine if a method call would cause recursion. Don't follow the path if
        // that is the case.
        ApexPath methodPath = null;
        InvocableVertex invocable = null;
        if (vertex instanceof InvocableVertex) {
            invocable = (InvocableVertex) vertex;
            // Call out to the path represented by a method if it is available
            try {
                methodPath = path.resolveInvocableCall(invocable).orElse(null);
            } catch (RecursionDetectedException ex) {
                visitor.recursionDetected(
                        path, (MethodCallExpressionVertex) invocable, ex.getPathWithRecursion());
                return;
            }
        }

        boolean symbolProviderVisitChildren =
                callSymbolProvider && vertex.visit(symbolProviderVisitor);
        boolean visitorVisitChildren = callVisitor && vertex.visit(visitor, symbolProvider);

        for (BaseSFVertex child : vertex.getChildren()) {
            visit(path, child, symbolProviderVisitChildren, visitorVisitChildren);
        }

        // Call out to the path represented by a method if it is available
        if (methodPath != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Starting method path from ApexPath" + path.getStableId() + ". methodCall="
                                + vertex
                                + ", firstVertex="
                                + methodPath.firstVertex());
            }

            // Notify method path listener
            methodPathListener.onMethodPathFork(path, methodPath, invocable);

            // Initialize method invocation's static scope
            MethodVertex method = methodPath.getMethodVertex().get();
            String className = method.getDefiningType();

            getClassStaticScope(className);
            symbolProviderVisitor.beforeMethodCall(invocable, methodPath.getMethodVertex().get());
            if (vertex instanceof NewObjectExpressionVertex) {
                NewObjectExpressionVertex newObjectExpression = (NewObjectExpressionVertex) vertex;
                Collectible<ApexPath> apexPath =
                        path.getNewInstanceToInitializationPath(newObjectExpression).orElse(null);
                if (apexPath.getCollectible() != null) {
                    visit(apexPath.getCollectible());
                }
            }

            visit(methodPath);
            symbolProviderVisitor.afterMethodCall(invocable, methodPath.getMethodVertex().get());
        }

        if (callSymbolProvider) {
            vertex.afterVisit(symbolProviderVisitor);
        }

        if (callVisitor) {
            vertex.afterVisit(visitor, symbolProvider);
        }

        if (callSymbolProvider) {
            symbolProviderVisitor.popScope(vertex);
        }

        if (vertex instanceof ThrowStatementVertex) {
            throw new ThrowStatementVertexVisitedException((ThrowStatementVertex) vertex);
        }
    }

    /** Get a static class scope. Reuse any existing ones in order to maintain state. */
    @Override
    public Optional<ClassStaticScope> getClassStaticScope(String className) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("getClassStaticScope. className=" + className);
        }
        final ClassStaticScope classStaticScope =
                classStaticScopes.computeIfAbsent(
                        className, k -> ClassStaticScope.getOptional(g, className).orElse(null));
        if (classStaticScope != null
                && classStaticScope.getState().equals(AbstractClassScope.State.UNINITIALIZED)) {
            classStaticScope.setState(AbstractClassScope.State.INITIALIZING);
            final String fullClassName = classStaticScope.getClassName();
            final Collectible<ApexPath> apexPath =
                    topMostPath.getStaticInitializationPath(fullClassName).orElse(null);
            if (apexPath != null && apexPath.getCollectible() != null) {
                symbolProviderVisitor.pushScope(classStaticScope);
                visit(apexPath.getCollectible());
                symbolProviderVisitor.popScope(classStaticScope);
            }
            classStaticScope.setState(AbstractClassScope.State.INITIALIZED);
        }
        return Optional.ofNullable(classStaticScope);
    }

    /**
     * This exception is thrown after a {@link ThrowStatementVertex} is visited. No other vertices
     * should be visited after this vertex is visited. TODO: This is simplistic, we need to handle
     * try/catch
     */
    private static final class ThrowStatementVertexVisitedException extends SfgeRuntimeException {
        private final ThrowStatementVertex vertex;

        private ThrowStatementVertexVisitedException(ThrowStatementVertex vertex) {
            // NOTE: We're very deliberately NOT calling `super()` here, since this
            // exception type doesn't require telemetry.
            this.vertex = vertex;
        }
    }
}
