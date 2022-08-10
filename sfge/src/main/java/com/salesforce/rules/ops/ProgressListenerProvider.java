package com.salesforce.rules.ops;

import com.google.common.annotations.VisibleForTesting;

public class ProgressListenerProvider {
    @VisibleForTesting
    static final ThreadLocal<ProgressListener> PROGRESS_LISTENER =
            ThreadLocal.withInitial(() -> ProgressListenerImpl.getInstance());

    /** Get the ProgressListener for the current thread */
    public static ProgressListener get() {
        return PROGRESS_LISTENER.get();
    }

    private ProgressListenerProvider() {}
}
