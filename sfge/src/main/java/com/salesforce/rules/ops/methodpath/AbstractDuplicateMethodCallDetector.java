package com.salesforce.rules.ops.methodpath;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.TreeSet;

abstract public class AbstractDuplicateMethodCallDetector implements MethodPathListener {

    private static final Logger LOGGER = LogManager.getLogger(AbstractDuplicateMethodCallDetector.class);
    protected final Multimap<String, InvocableVertex> methodCallToInvocationOccurrence;

    protected AbstractDuplicateMethodCallDetector() {
        methodCallToInvocationOccurrence = ArrayListMultimap.create();
    }


    protected abstract boolean shouldIgnoreMethod(InvocableVertex vertex);

    protected abstract void performActionForDetectedDuplication(InvocableVertex vertex);

    @Override
    public void beforePathStart(ApexPath path) {
        LOGGER.warn("Before path start, clearing methodCallsMade.");
        methodCallToInvocationOccurrence.clear();
    }

    @Override
    public void onMethodPathFork(ApexPath currentPath, ApexPath newMethodPath, InvocableVertex invocableVertex) {
//        final String currentPathMethodUniqueKey = currentPath.getMethodVertex().get().generateUniqueKey();
        final String newPathMethodUniqueKey = newMethodPath.getMethodVertex().get().generateUniqueKey();
        final String key = /*currentPathMethodUniqueKey + "," +*/ newPathMethodUniqueKey;

        if (shouldIgnoreMethod(invocableVertex)) {
            LOGGER.warn("Method call ignored: " + key);
            return;
        }

        LOGGER.warn("Adding method to treeSet: " + key);

        boolean firstTimeVisit = methodCallToInvocationOccurrence.put(key, invocableVertex);

        if (!firstTimeVisit) {
            LOGGER.warn("Method has been visited before: " + key);
            performActionForDetectedDuplication(invocableVertex);
        }
    }
}
