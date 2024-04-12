package com.salesforce.graph.ops.expander;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.ops.MethodUtil;
import com.salesforce.graph.ops.directive.EngineDirective;
import com.salesforce.graph.ops.directive.EngineDirectiveCommand;
import com.salesforce.graph.symbols.*;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.ThrowStatementVertex;
import java.util.*;
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
     * @return A collection of paths that were accepted, and reasons the rest were rejected
     */
    public static ApexPathCollector expand(
            GraphTraversalSource g, ApexPath path, ApexPathExpanderConfig config) {
        if (path.endsInException()) {
            // Filter out any paths in the original method that ends in an exception
            ThrowStatementVertex throwStatementVertex = path.getThrowStatementVertex().get();
            logFilteredOutPath(throwStatementVertex);
            // Return an empty results collection.
            return new ApexPathCollector();
        } else {
            final PathExpansionRegistry registry = new PathExpansionRegistry();
            ApexPathExpansionHandler handler = new ApexPathExpansionHandler(config, registry);
            final ApexPathCollector results = handler._expand(g, path, config);
            // Clean up registry to remove any lingering references
            registry.clear();
            return results;
        }
    }

    private static void logFilteredOutPath(ThrowStatementVertex throwStatementVertex) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "Filtering out path that ends in an exception. throwStatement="
                            + throwStatementVertex);
        }
    }

    private static class ApexPathExpansionHandler {
        private final PathExpansionRegistry registry;

        /**
         * The {@code ApexPathCollapser} that keeps track of all paths and attempts to collapse them
         * whenever a forked method has returned a result.
         */
        private final ApexPathCollapser apexPathCollapser;

        private ApexPathExpansionHandler(
                ApexPathExpanderConfig config, PathExpansionRegistry registry) {

            this.registry = registry;

            if (config.getDynamicCollapsers().isEmpty()) {
                this.apexPathCollapser = NoOpApexPathCollapser.getInstance();
            } else {
                this.apexPathCollapser =
                        new ApexPathCollapserImpl(config.getDynamicCollapsers(), registry);
            }
        }

        private ApexPathCollector _expand(
                GraphTraversalSource g, ApexPath path, ApexPathExpanderConfig config) {
            ApexPathCollector pathCollector = new ApexPathCollector();
            try {
                expand(g, path, config, pathCollector);
                return pathCollector;
            } catch (RuntimeException ex) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(
                            "Incomplete. Current PathCollector size="
                                    + pathCollector.getAcceptedResults().size(),
                            ex);
                }
                throw ex;
            }
        }

        /**
         * Convert the {@code path} to one or more ApexPathExpanders. The number of
         * ApexPathExpanders is dependent on the initial method that is used as an entry point.
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
                ApexPathCollector resultCollector) {
            // Seed the stack with the initial paths
            Stack<ApexPathExpander> apexPathExpanders = new Stack<>();

            final MethodVertex method = path.getMethodVertex().get();
            final String className = method.getDefiningType();
            if (method.isStatic()) {
                final ApexPathExpander apexPathExpander =
                        new ApexPathExpander(g, apexPathCollapser, path, config, registry);
                apexPathExpanders.push(apexPathExpander);
            } else {
                if (method instanceof MethodVertex.ConstructorVertex) {
                    final ApexPathExpander apexPathExpander =
                            new ApexPathExpander(g, apexPathCollapser, path, config, registry);
                    apexPathExpanders.push(apexPathExpander);
                } else {
                    final List<MethodVertex.ConstructorVertex> constructors =
                            MethodUtil.getNonDefaultConstructors(g, className);
                    if (constructors.isEmpty()) {
                        final ApexPathExpander apexPathExpander =
                                new ApexPathExpander(g, apexPathCollapser, path, config, registry);
                        apexPathExpanders.push(apexPathExpander);
                    } else {
                        // Expand by number of constructors * number of paths
                        for (MethodVertex.ConstructorVertex constructor : constructors) {
                            for (ApexPath constructorPath :
                                    ApexPathUtil.getForwardPaths(g, constructor, false)) {
                                final ApexPath clonedPath = path.deepClone();
                                clonedPath.setConstructorPath(constructorPath);
                                final ApexPathExpander apexPathExpander =
                                        new ApexPathExpander(
                                                g, apexPathCollapser, clonedPath, config, registry);
                                apexPathExpanders.push(apexPathExpander);
                            }
                        }
                    }
                }
            }

            expand(apexPathExpanders, resultCollector);
        }

        private void expand(
                Stack<ApexPathExpander> apexPathExpanders, ApexPathCollector resultCollector) {

            // Continue while there is work to do. This stack is updated as the path is forked.
            // Forked expanders are pushed to the front of the stack, causing the paths to be
            // traversed
            // depth first in order
            // to keep the peak number of active expanders lower.
            PendingForkStack pendingForkStack = new PendingForkStack(apexPathCollapser);
            Optional<ApexPathExpander> nextExpander;
            while ((nextExpander = getNextExpander(apexPathExpanders, pendingForkStack))
                    .isPresent()) {
                ApexPathExpander apexPathExpander = nextExpander.get();
                ContextProviders.CLASS_STATIC_SCOPE.push(apexPathExpander);
                ContextProviders.ENGINE_DIRECTIVE_CONTEXT.push(apexPathExpander);
                try {
                    // Configure all class instantiation paths before calling into the
                    // symbolProviderVisitor. This will
                    // ensure that the state is correct after any ForkedExceptions are thrown
                    final ApexPath topMostPath = apexPathExpander.getTopMostPath();
                    final MethodVertex method = topMostPath.getMethodVertex().get();
                    // Push any stack directives found on the initial method which is being
                    // traversed
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
                        final ApexPath constructorPath =
                                topMostPath.getConstructorPath().orElse(null);
                        if (constructorPath != null) {
                            // Visit the constructor path by itself. This is a case where we are
                            // walking
                            // the constructor
                            // but don't know which values were passed to the constructor. Create an
                            // indeterminant
                            // MethodInvocationScope and push that onto the stack of
                            // ClassInstanceScope.
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
                        apexPathExpander.finished();
                    } else {
                        resultCollector.collectAccepted(apexPathExpander);
                    }
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("expand-Finished.");
                    }
                } catch (PathExcludedException ex) {
                    apexPathCollapser.removeExistingExpander(apexPathExpander);
                    // Excluding a path rejects it.
                    resultCollector.collectRejected(apexPathExpander, ex);
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("expand-Excluded. ex=" + ex);
                    }
                } catch (PathCollapsedException ex) {
                    apexPathCollapser.removeExistingExpander(apexPathExpander);
                    // Collapsing a path rejects it.
                    resultCollector.collectRejected(apexPathExpander, ex);
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("expand-Collapsed. ex=" + ex);
                    }
                } catch (ReturnValueInvalidCollapsedException ex) {
                    apexPathCollapser.removeExistingExpander(apexPathExpander);
                    // Paths with invalid returns are rejected.
                    resultCollector.collectRejected(apexPathExpander, ex);
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
                    resultCollector.collectAccepted(apexPathExpander);
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("expand-Recursion. ex=" + ex);
                    }
                } catch (NullValueAccessedException ex) {
                    apexPathCollapser.removeExistingExpander(apexPathExpander);
                    // Paths terminated for NullPointerExceptions are rejected.
                    resultCollector.collectRejected(apexPathExpander, ex);
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("expand-NullAccess. ex=" + ex);
                    }
                } catch (StackDepthLimitExceededException ex) {
                    apexPathCollapser.removeExistingExpander(apexPathExpander);
                    // Paths exceeding stack depth limit are rejected.
                    resultCollector.collectRejected(apexPathExpander, ex);
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("expand-StackDepthLimit. ex=" + ex);
                    }
                } catch (MethodPathForkedException ex) {
                    pendingForkStack.addStackFrame(ex);
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
                            && !resultCollector.removeAccepted(pathExpander)) {
                        // TODO: Throw
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("Unable to find apexPathExpander=" + pathExpander);
                        }
                    }
                    pathExpander.finished();
                }
            }

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "Path info. completedApexPathExpanders="
                                + resultCollector.acceptedSize()
                                + "; rejectedApexPathExpanders="
                                + resultCollector.rejectedSize());
            }
        }

        private Optional<ApexPathExpander> getNextExpander(
                Stack<ApexPathExpander> baseExpanderStack, PendingForkStack pendingForkStack) {
            Optional<ApexPathExpander> nextExpander = pendingForkStack.getNextExpander();
            if (nextExpander.isPresent()) {
                return nextExpander;
            } else if (!baseExpanderStack.isEmpty()) {
                return Optional.of(baseExpanderStack.pop());
            } else {
                return Optional.empty();
            }
        }
    }

    private static class PendingForkStack {
        private final Stack<PendingForkStackFrame> stack;
        private final ApexPathCollapser apexPathCollapser;

        PendingForkStack(ApexPathCollapser apexPathCollapser) {
            stack = new Stack<>();
            this.apexPathCollapser = apexPathCollapser;
        }

        void addStackFrame(MethodPathForkedException ex) {
            stack.push(new PendingForkStackFrame(ex, apexPathCollapser));
        }

        Optional<ApexPathExpander> getNextExpander() {
            // Base Case: The stack is empty, so there are no more frames to inspect.
            // Return an empty optional.
            if (stack.empty()) {
                return Optional.empty();
            }
            PendingForkStackFrame frame = stack.peek();
            Optional<ApexPathExpander> nextExpanderOptional = frame.getNextExpander();
            if (nextExpanderOptional.isEmpty()) {
                // Recursive Case: The current frame is empty. Pop it off, and call recursively
                // to move to the next frame.
                stack.pop();
                return getNextExpander();
            } else {
                // Base Case: The current frame returned an Expander. Return it.
                return nextExpanderOptional;
            }
        }
    }

    private static class PendingForkStackFrame {

        private final MethodPathForkedException ex;
        private final ApexPathCollapser apexPathCollapser;
        private int idx;

        PendingForkStackFrame(MethodPathForkedException ex, ApexPathCollapser apexPathCollapser) {
            this.ex = ex;
            this.apexPathCollapser = apexPathCollapser;
            this.idx = 0;
        }

        private boolean done() {
            return idx >= ex.getPaths().size();
        }

        Optional<ApexPathExpander> getNextExpander() {
            // Base Case: There are no more unprocessed paths on this frame.
            if (done()) {
                // Finish the expander and return an empty optional.
                ex.getApexPathExpander().finished();
                return Optional.empty();
            }
            ApexPath nextPotentialPath = ex.getPaths().get(idx);
            if (nextPotentialPath.endsInException()) {
                // Recursive Case: This particular path ends in an exception, so we skip it, log it,
                // and do a recursive call.
                logFilteredOutPath(nextPotentialPath.getThrowStatementVertex().get());
                idx += 1;
                return getNextExpander();
            } else {
                DeepCloneContextProvider.establish();
                ApexPathExpander tentativeResult;
                try {
                    tentativeResult = new ApexPathExpander(ex.getApexPathExpander(), ex, idx);
                } finally {
                    DeepCloneContextProvider.release();
                }
                if (tentativeResult.getTopMostPath().endsInException()) {
                    // Recursive case: The topmost path ends in an exception, so we skip it, log it,
                    // and do a recursive call.
                    logFilteredOutPath(
                            tentativeResult.getTopMostPath().getThrowStatementVertex().get());
                    idx += 1;
                    tentativeResult.finished();
                    return getNextExpander();
                } else {
                    // Base Case: We have an expander.
                    apexPathCollapser.pathForked(
                            ex.getForkEvent(),
                            ex.getApexPathExpander(),
                            Collections.singletonList(tentativeResult));
                    idx += 1;
                    return Optional.of(tentativeResult);
                }
            }
        }
    }
}
