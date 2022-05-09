package com.salesforce.config;

/**
 * Default implementation of {@link SfgeConfig} that currently only supports environment variables.
 * See {@link EnvUtil}
 */
public final class SfgeConfigImpl implements SfgeConfig {
    @Override
    public int getRuleThreadCount() {
        return EnvUtil.getRuleThreadCount();
    }

    @Override
    public long getRuleThreadTimeout() {
        return EnvUtil.getRuleThreadTimeout();
    }

    @Override
    public boolean isWarningViolationEnabled() {
        return EnvUtil.isWarningViolationEnabled();
    }

    @Override
    public boolean shouldIgnoreParseErrors() {
        return EnvUtil.shouldIgnoreParseErrors();
    }

    static SfgeConfigImpl getInstance() {
        return SfgeConfigImpl.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final SfgeConfigImpl INSTANCE = new SfgeConfigImpl();
    }

    private SfgeConfigImpl() {}
}
