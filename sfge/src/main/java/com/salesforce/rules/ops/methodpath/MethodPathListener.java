package com.salesforce.rules.ops.methodpath;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.vertex.InvocableVertex;

/**
 * Listens on events when {@link com.salesforce.graph.visitor.ApexPathWalker} expands method calls.
 */
public interface MethodPathListener {
    /**
     * Invoked when a method call is identified from a path.
     * @param currentPath {@link ApexPath} that's currently walked. This could've been a newMethodPath in an earlier recursion step.
     * @param newMethodPath New {@link ApexPath} that's been identified for the method call.
     * @param invocableVertex Method call or variable invocation that needs a new path to be walked.
     */
    void onMethodPathFork(ApexPath currentPath, ApexPath newMethodPath, InvocableVertex invocableVertex);

    /**
     *
     * @param path
     */
    void beforePathStart(ApexPath path);
}
