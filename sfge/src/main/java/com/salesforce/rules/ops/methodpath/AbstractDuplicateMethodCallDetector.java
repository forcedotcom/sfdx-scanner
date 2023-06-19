package com.salesforce.rules.ops.methodpath;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodVertex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractDuplicateMethodCallDetector implements MethodPathListener {

    private static final Logger LOGGER =
            LogManager.getLogger(AbstractDuplicateMethodCallDetector.class);
    protected final Multimap<String, InvocableVertex> methodCallToInvocationOccurrence;

    protected AbstractDuplicateMethodCallDetector() {
        methodCallToInvocationOccurrence = ArrayListMultimap.create();
    }

    /**
     * Execute action to be performed before processing the vertex further
     *
     * @param vertex InvocableVertex that's forked
     */
    protected abstract void performPreAction(InvocableVertex vertex);

    /**
     * Checks if a vertex should be ignored.
     *
     * @param vertex to examine
     * @return true if vertex can be ignored.
     */
    protected abstract boolean shouldIgnoreMethod(InvocableVertex vertex);

    /**
     * Performs additional action with the vertex when a duplicate call is detected.
     *
     * @param key that represents this invocable.
     * @param vertex that had a duplicate call.
     */
    protected abstract void performActionForDetectedDuplication(String key, InvocableVertex vertex);

    @Override
    public void beforePathStart(ApexPath path) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Before path start, clearing method calls made.");
        }
        methodCallToInvocationOccurrence.clear();
    }

    @Override
    public void onMethodPathFork(
            ApexPath currentPath, ApexPath newMethodPath, InvocableVertex invocableVertex) {
        performPreAction(invocableVertex);

        final MethodVertex resolvedMethod = newMethodPath.getMethodVertex().get();
        final String newPathMethodUniqueKey = resolvedMethod.generateUniqueKey();
        final String key = newPathMethodUniqueKey;

        if (shouldIgnoreMethod(invocableVertex)) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Method call ignored: " + key);
            }
            return;
        }

        // Detect if this method has been visited before.
        // Fyi, boolean return value of put method has not been reliable.
        boolean visitedBefore = false;
        if (methodCallToInvocationOccurrence.containsKey(key)) {
            visitedBefore = true;
        }
        methodCallToInvocationOccurrence.put(key, invocableVertex);

        if (visitedBefore) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Method has been visited before: " + key);
            }
            performActionForDetectedDuplication(key, invocableVertex);
        }
    }
}
