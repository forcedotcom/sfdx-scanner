package com.salesforce.graph.symbols;

public final class DefaultNoOpScope extends AbstractDefaultNoOpScope {
    public static DefaultNoOpScope getInstance() {
        return DefaultNoOpScope.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final DefaultNoOpScope INSTANCE = new DefaultNoOpScope();
    }

    private DefaultNoOpScope() {}
}
