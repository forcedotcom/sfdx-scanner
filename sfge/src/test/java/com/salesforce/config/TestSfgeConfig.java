package com.salesforce.config;

/**
 * Test implementation that reduces the number of methods needed when a test wants to override
 * behavior
 */
public class TestSfgeConfig implements SfgeConfig {
    @Override
    public int getRuleThreadCount() {
        return SfgeConfigImpl.getInstance().getRuleThreadCount();
    }

    @Override
    public long getRuleThreadTimeout() {
        return SfgeConfigImpl.getInstance().getRuleThreadTimeout();
    }

    @Override
    public boolean isWarningViolationDisabled() {
        return SfgeConfigImpl.getInstance().isWarningViolationDisabled();
    }

    @Override
    public boolean shouldLogWarningsOnVerbose() {
        return SfgeConfigImpl.getInstance().shouldLogWarningsOnVerbose();
    }

    @Override
    public int getProgressIncrements() {
        return SfgeConfigImpl.getInstance().getProgressIncrements();
    }

    @Override
    public int getStackDepthLimit() {
        return SfgeConfigImpl.getInstance().getStackDepthLimit();
    }

    @Override
    public int getPathExpansionLimit() {
        return SfgeConfigImpl.getInstance().getPathExpansionLimit();
    }

    @Override
    public String getCacheDir() {
        // Use an alternate directory for tests
        return ".sfge-cache-test";
    }

    @Override
    public String getFilesToEntriesCacheData() {
        return SfgeConfigImpl.getInstance().getFilesToEntriesCacheData();
    }
}
