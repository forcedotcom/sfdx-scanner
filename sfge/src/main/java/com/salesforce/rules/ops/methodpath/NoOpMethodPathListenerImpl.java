package com.salesforce.rules.ops.methodpath;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.vertex.InvocableVertex;

public final class NoOpMethodPathListenerImpl implements MethodPathListener {
    private NoOpMethodPathListenerImpl() {}

    @Override
    public void onMethodPathFork(
            ApexPath currentPath, ApexPath newMethodPath, InvocableVertex invocableVertex) {
        // Do nothing. Intentionally blank.
    }

    @Override
    public void beforePathStart(ApexPath path) {
        // Do nothing. Intentionally blank.
    }

    private static class LazyInstance {
        private static NoOpMethodPathListenerImpl noOpMethodPathListener;

        static NoOpMethodPathListenerImpl get() {
            if (noOpMethodPathListener == null) {
                noOpMethodPathListener = new NoOpMethodPathListenerImpl();
            }

            return noOpMethodPathListener;
        }
    }

    public static NoOpMethodPathListenerImpl get() {
        return LazyInstance.get();
    }
}
