package com.salesforce.graph.ops.expander;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.ops.MethodUtil;
import com.salesforce.graph.ops.directive.EngineDirective;
import com.salesforce.graph.ops.directive.EngineDirectiveCommand;
import com.salesforce.graph.symbols.ClassInstanceScope;
import com.salesforce.graph.symbols.ContextProviders;
import com.salesforce.graph.symbols.DeepCloneContextProvider;
import com.salesforce.graph.symbols.MethodInvocationScope;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.ThrowStatementVertex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * This class orchestrates the expansion of paths utilizing the {@link ApexPathExpander} and {@link
 * ApexPathCollapser} classes.
 */
public final class ApexPathExpanderUtil {
    private static final Logger LOGGER = LogManager.getLogger(ApexPathExpanderUtil.class);

    /**
     * Traverses a path in order to resolve method calls and create forks of paths where an invoked
     * method consists of one or more paths. A PathForkedException is caught when the path reaches a
     * fork. All items and their state are cloned at that point and the Path is traversed again.
     * Various Maps and Sets are maintained to avoid calling the same method multiple times.
     *
     * <p>Path Traversal is completed when one of the following occurs.
     *
     * <p>
     *
     * <ul>
     *   <li>All vertices are visited
     *   <li>A path ends in an exception
     *   <li>Recursion is detected
     * </ul>
     *
     * @return A list of paths that represent all possible combinations of execution
     */
    public static List<ApexPath> expand(
            GraphTraversalSource g, ApexPath path, ApexPathExpanderConfig config) {
        if (path.endsInException()) {
            // Filter out any paths in the original method that ends in an exception
            ThrowStatementVertex throwStatementVertex = path.getThrowStatementVertex().get();
            logFilteredOutPath(throwStatementVertex);
            return Collections.emptyList();
        } else {
            ApexPathExpanderUtil apexPathExpanderUtil = new ApexPathExpanderUtil(config);
            return apexPathExpanderUtil._expand(g, path, config);
        }
    }

    private static void logFilteredOutPath(ThrowStatementVertex throwStatementVertex) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "Filtering out path that ends in an exception. throwStatement="
                            + throwStatementVertex);
        }
    }

    /** Used to give each object a unique id */
    private static final AtomicLong ID_GENERATOR = new AtomicLong();

    private final Long pathExpansionId;

    private ApexPathExpanderUtil(ApexPathExpanderConfig config) {
        this.pathExpansionId = ID_GENERATOR.incrementAndGet();
        final ApexPathCollapser apexPathCollapser;
        if (config.getDynamicCollapsers().isEmpty()) {
            apexPathCollapser = NoOpApexPathCollapser.getInstance();
        } else {
            apexPathCollapser = new ApexPathCollapserImpl(config.getDynamicCollapsers());
        }

        PathExpansionRegistry.registerPathCollapser(pathExpansionId, apexPathCollapser);
    }

    private List<ApexPath> _expand(
            GraphTraversalSource g, ApexPath path, ApexPathExpanderConfig config) {
        ApexPathCollector pathCollector = new ApexPathCollector();
        try {
            expand(g, path, config, pathCollector);
            return pathCollector.getResults();
        } catch (RuntimeException ex) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(
                        "Incomplete. Current PathCollector size="
                                + pathCollector.getResults().size(),
                        ex);
            }
            throw ex;
        }
    }

    /**
     * Convert the {@code path} to one or more ApexPathExpanders. The number of ApexPathExpanders is
     * dependent on the initial method that is used as an entry point.
     *
     * <ul>
     *   <li>Static entry point: Single ApexPathExpander
     *   <li>Constructor entry point: Single ApexPathExpander
     *   <li>Instance entry point: Num Constructor Paths
     * </ul>
     */
    private void expand(
            GraphTraversalSource g,
            ApexPath path,
            ApexPathExpanderConfig config,
            ResultCollector resultCollector) {
        // Seed the stack with the initial paths
        Stack<ApexPathExpander> apexPathExpanders = new Stack<>();

        final MethodVertex method = path.getMethodVertex().get();
        final String className = method.getDefiningType();
        if (method.isStatic()) {
            final ApexPathExpander apexPathExpander =
                    new ApexPathExpander(g, pathExpansionId, path, config);
            apexPathExpanders.push(apexPathExpander);
        } else {
            if (method instanceof MethodVertex.ConstructorVertex) {
                final ApexPathExpander apexPathExpander =
                        new ApexPathExpander(g, pathExpansionId, path, config);
                apexPathExpanders.push(apexPathExpander);
            } else {
                final List<MethodVertex.ConstructorVertex> constructors =
                        MethodUtil.getNonDefaultConstructors(g, className);
                if (constructors.isEmpty()) {
                    final ApexPathExpander apexPathExpander =
                            new ApexPathExpander(g, pathExpansionId, path, config);
                    apexPathExpanders.push(apexPathExpander);
                } else {
                    // Expand by number of constructors * number of paths
                    for (MethodVertex.ConstructorVertex constructor :
                            MethodUtil.getNonDefaultConstructors(g, className)) {
                        for (ApexPath constructorPath :
                                ApexPathUtil.getForwardPaths(g, constructor, false)) {
                            final ApexPath clonedPath = path.deepClone();
                            clonedPath.setConstructorPath(constructorPath);
                            final ApexPathExpander apexPathExpander =
                                    new ApexPathExpander(g, pathExpansionId, clonedPath, config);
                            apexPathExpanders.push(apexPathExpander);
                        }
                    }
                }
            }
        }

        expand(apexPathExpanders, resultCollector);
    }

    private void expand(
            Stack<ApexPathExpander> apexPathExpanders, ResultCollector resultCollector) {
        final ApexPathCollapser apexPathCollapser =
                PathExpansionRegistry.lookupPathCollapser(pathExpansionId);
        // Continue while there is work to do. This stack is updated as the path is forked.
        // Forked expanders are pushed to the front of the stack, causing the paths to be traversed
        // depth first in order
        // to keep the peak number of active expanders lower.
        while (!apexPathExpanders.isEmpty()) {
            ApexPathExpander apexPathExpander = apexPathExpanders.pop();
            ContextProviders.CLASS_STATIC_SCOPE.push(apexPathExpander);
            ContextProviders.ENGINE_DIRECTIVE_CONTEXT.push(apexPathExpander);
            try {
                // Configure all class instantiation paths before calling into the
                // symbolProviderVisitor. This will
                // ensure that the state is correct after any ForkedExceptions are thrown
                final ApexPath topMostPath = apexPathExpander.getTopMostPath();
                final MethodVertex method = topMostPath.getMethodVertex().get();
                // Push any stack directives found on the initial method which is being traversed
                final List<EngineDirective> engineDirectives =
                        method.getEngineDirectives(EngineDirectiveCommand.DISABLE_STACK);
                if (!engineDirectives.isEmpty()) {
                    apexPathExpander.getEngineDirectiveContext().push(engineDirectives);
                }

                apexPathExpander.initializeClassStaticScope(method.getDefiningType());

                SymbolProvider currentScope = apexPathExpander.start(topMostPath.firstVertex());
                if (currentScope instanceof ClassInstanceScope) {
                    final ClassInstanceScope classScope = (ClassInstanceScope) currentScope;
                    final ApexPath initializationPath =
                            topMostPath.getInstanceInitializationPath().orElse(null);
                    if (initializationPath != null) {
                        apexPathExpander.visit(initializationPath);
                    }
                    final ApexPath constructorPath = topMostPath.getConstructorPath().orElse(null);
                    if (constructorPath != null) {
                        // Visit the constructor path by itself. This is a case where we are walking
                        // the constructor
                        // but don't know which values were passed to the constructor. Create an
                        // indeterminant
                        // MethodInvocationScope and push that onto the stack of ClassInstanceScope.
                        final MethodInvocationScope methodInvocationScope =
                                MethodUtil.getIndeterminantMethodInvocationScope(
                                        constructorPath.getMethodVertex().get());
                        classScope.pushMethodInvocationScope(methodInvocationScope);
                        try {
                            apexPathExpander.visit(constructorPath);
                        } finally {
                            classScope.popMethodInvocationScope(null);
                        }
                    }
                }

                apexPathExpander.visit(topMostPath);
                if (apexPathExpander.getTopMostPath().endsInException()) {
                    // Filter out any paths in the original method that ends in an exception
                    final ThrowStatementVertex throwStatementVertex =
                            apexPathExpander.getTopMostPath().getThrowStatementVertex().get();
                    logFilteredOutPath(throwStatementVertex);
                } else {
                    resultCollector.collect(apexPathExpander);
                }
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("expand-Finished.");
                }
            } catch (PathExcludedException ex) {
                apexPathCollapser.removeExistingExpander(apexPathExpander);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("expand-Excluded. ex=" + ex);
                }
            } catch (PathCollapsedException ex) {
                apexPathCollapser.removeExistingExpander(apexPathExpander);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("expand-Collapsed. ex=" + ex);
                }
            } catch (ReturnValueInvalidCollapsedException ex) {
                apexPathCollapser.removeExistingExpander(apexPathExpander);
                ApexValue<?> returnValue = ex.getReturnValue().orElse(null);
                if (ex.getPath().getMethodVertex().isPresent()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                                "expand-InvalidValue. pathMethod="
                                        + ex.getPath().getMethodVertex().get().toSimpleString()
                                        + ", returnValue="
                                        + returnValue);
                    }
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                                "expand-InvalidValue. pathDefiningType="
                                        + ex.getPath().firstVertex().getDefiningType()
                                        + ", returnValue="
                                        + returnValue);
                    }
                }
            } catch (RecursionDetectedException ex) {
                apexPathCollapser.removeExistingExpander(apexPathExpander);
                resultCollector.collect(apexPathExpander);
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("expand-Recursion. ex=" + ex);
                }
            } catch (NullValueAccessedException ex) {
                apexPathCollapser.removeExistingExpander(apexPathExpander);
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("expand-NullAccess. ex=" + ex);
                }
            } catch (MethodPathForkedException ex) {
                List<ApexPathExpander> forkedPathExpanders = new ArrayList<>();
                for (ApexPathExpander forkedApexPathExpander : getForkedExpanders(ex)) {
                    if (forkedApexPathExpander.getTopMostPath().endsInException()) {
                        // This happens when a path invokes a method that only throws an exception
                        ThrowStatementVertex throwStatementVertex =
                                forkedApexPathExpander
                                        .getTopMostPath()
                                        .getThrowStatementVertex()
                                        .get();
                        logFilteredOutPath(throwStatementVertex);
                    } else {
                        apexPathExpanders.push(forkedApexPathExpander);
                        forkedPathExpanders.add(forkedApexPathExpander);
                    }
                }
                apexPathCollapser.pathForked(
                        ex.getForkEvent(), apexPathExpander, forkedPathExpanders);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("expand-Forked. ex=" + ex);
                }
            } catch (RuntimeException ex) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(
                            "Incomplete. Current ApexPathExpanders size="
                                    + apexPathExpanders.size(),
                            ex);
                }
                throw ex;
            } finally {
                ContextProviders.CLASS_STATIC_SCOPE.pop();
                ContextProviders.ENGINE_DIRECTIVE_CONTEXT.pop();
            }

            for (ApexPathExpander pathExpander : apexPathCollapser.clearCollapsedExpanders()) {
                if (!apexPathExpanders.remove(pathExpander)
                        && !resultCollector.remove(pathExpander)) {
                    // TODO: Throw
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Unable to find apexPathExpander=" + pathExpander);
                    }
                }
            }
        }

        // Clear ApexPathCollapser for this pathExpansion effort
        PathExpansionRegistry.clear();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Path info. completedApexPathExpanders=" + resultCollector.size());
        }
    }

    /** Returns the number of ApexPathExpanders indicated by the exception */
    private List<ApexPathExpander> getForkedExpanders(MethodPathForkedException ex) {
        List<ApexPathExpander> result = new ArrayList<>();

        // TODO: Efficiency. We could iterate to size() - 1 and reuse the existing path
        for (int i = 0; i < ex.getPaths().size(); i++) {
            ApexPath forkedPath = ex.getPaths().get(i);
            if (forkedPath.endsInException()) {
                logFilteredOutPath(forkedPath.getThrowStatementVertex().get());
                continue;
            }
            // Establish a context so that objects passed by reference are only cloned once
            DeepCloneContextProvider.establish();
            try {
                result.add(new ApexPathExpander(ex.getApexPathExpander(), ex, i));
            } finally {
                DeepCloneContextProvider.release();
            }
        }

        return result;
    }
}
