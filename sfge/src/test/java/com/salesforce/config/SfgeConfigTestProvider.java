package com.salesforce.config;

/**
 * Test implementation that overrides behavior of {@link SfgeConfigProvider}. Allows a test to
 * override the {@link SfgeConfig} object for the current thread. Tests should always call {@link
 * #remove()} after the test has run.
 */
public class SfgeConfigTestProvider {
    /**
     * Set SfgeConfig for the current thread. {@link #remove()} should be called when the test ends.
     */
    public static void set(SfgeConfig sfgeConfig) {
        SfgeConfigProvider.SFGE_CONFIGS.set(sfgeConfig);
    }

    /** Remove the SfgeConfig from the current thread */
    public static void remove() {
        SfgeConfigProvider.SFGE_CONFIGS.remove();
    }
}
