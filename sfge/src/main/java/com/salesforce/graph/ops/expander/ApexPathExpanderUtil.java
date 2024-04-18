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
            // Since ApexPathExpanders are so memory intensive, we want to limit the number
            // that exist at once. So we use this stack to lazily create them as needed, storing
            // only the information that is used to create them. This allows us to do a reasonably
            // efficient depth-first expansion.
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
                    // Add this exception to the PendingForkStack, so it can be lazily processed
                    // later.
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

        /**
         * Returns the next {@link ApexPathExpander} to be processed, either by lazily creating it
         * with {@code pendingForkStack} or by popping it off of {@code baseExpanderStack}.
         *
         * @param baseExpanderStack A stack of the base expanders that will need to be processed
         * @param pendingForkStack A stack of fork events that are waiting to be processed.
         */
        private Optional<ApexPathExpander> getNextExpander(
                Stack<ApexPathExpander> baseExpanderStack, PendingForkStack pendingForkStack) {
            // If the pending fork stack can give us an expander, we should use that in order to
            // keep the expansion truly depth-first.
            Optional<ApexPathExpander> nextExpander = pendingForkStack.getNextExpander();
            if (nextExpander.isPresent()) {
                return nextExpander;
            } else if (!baseExpanderStack.isEmpty()) {
                // Otherwise, if there are still expanders in the base stack, use one of those.
                return Optional.of(baseExpanderStack.pop());
            } else {
                // Otherwise, we're just done.
                return Optional.empty();
            }
        }
    }

    /**
     * Since {@link ApexPathExpander}s have a large memory footprint, we want to minimize the number
     * of instances that exist simultaneously. This class allows us to lazily create the expanders
     * as needed, and perform a true depth-first expansion.
     */
    private static class PendingForkStack {
        /**
         * We use a Stack instead of a List to allow for a true depth-first expansion. i.e., when a
         * new fork is encountered, it's added to the top of the stack, meaning it gets processed
         * first.
         */
        private final Stack<PendingForkStackFrame> stack;
        /** The collapser is present to allow each stack frame to handle forks. */
        private final ApexPathCollapser apexPathCollapser;

        PendingForkStack(ApexPathCollapser apexPathCollapser) {
            stack = new Stack<>();
            this.apexPathCollapser = apexPathCollapser;
        }

        /**
         * Use the provide Exception to create a new {@link PendingForkStackFrame} and add it to the
         * top of the stack.
         *
         * @param ex An exception thrown when a method is determined to fork.
         */
        void addStackFrame(MethodPathForkedException ex) {
            stack.push(new PendingForkStackFrame(ex, apexPathCollapser));
        }

        /**
         * @return An Optional containing either the next {@link ApexPathExpander} to be processed,
         *     or nothing.
         */
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

    /**
     * Each frame in the {@link PendingForkStack} represents a fork event currently being processed.
     */
    private static class PendingForkStackFrame {
        /** The exception that triggered the fork. */
        private final MethodPathForkedException ex;
        /** The collapser used to process forks. */
        private final ApexPathCollapser apexPathCollapser;
        /**
         * The index of the next {@link ApexPath} that should be turned into an {@link
         * ApexPathExpander}.
         */
        private int idx;

        PendingForkStackFrame(MethodPathForkedException ex, ApexPathCollapser apexPathCollapser) {
            this.ex = ex;
            this.apexPathCollapser = apexPathCollapser;
            this.idx = 0;
        }

        /**
         * @return {@code true} if this frame is out of paths to turn into expanders; else {@link
         *     false}.
         */
        private boolean done() {
            return idx >= ex.getPaths().size();
        }

        /**
         * @return An Optional containing either the next {@link ApexPathExpander} or nothing.
         */
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
                    // Make sure to call `finished()` before we discard it, otherwise it won't get
                    // garbage-collected.
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
