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
    public boolean isWarningViolationDisabled() {
        return EnvUtil.isWarningViolationDisabled();
    }

    @Override
    public boolean shouldLogWarningsOnVerbose() {
        return EnvUtil.shouldLogWarningsOnVerbose();
    }

    @Override
    public int getProgressIncrements() {
        return EnvUtil.getProgressIncrements();
    }

    @Override
    public int getStackDepthLimit() {
        return EnvUtil.getStackDepthLimit();
    }

    @Override
    public int getPathExpansionLimit() {
        return EnvUtil.getPathExpansionLimit();
    }

    @Override
    public String getCacheDir() {
        return EnvUtil.getCacheDir();
    }

    @Override
    public String getFilesToEntriesCacheData() {
        return EnvUtil.getFilesToEntriesCacheData();
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
