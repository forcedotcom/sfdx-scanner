package com.salesforce.config;

/**
 * Provides runtime information about configuration. This information may come from different
 * sources such as config files or environment variables.
 */
public interface SfgeConfig {
    /** Should be used to set the number of threads that can be spawned to execute rules. */
    int getRuleThreadCount();

    /** Should be used to set the timeout for rule execution threads. */
    long getRuleThreadTimeout();

    /** Should be used to check if Warning Violations are disabled. */
    boolean isWarningViolationDisabled();

    /**
     * Indicates if Warn level logs to log4j should be forwarded to CLI as well when verbose is
     * enabled
     */
    boolean shouldLogWarningsOnVerbose();

    /**
     * Should be used to set the level of increments at which path analysis progress update is
     * provided
     */
    int getProgressIncrements();

    /** Stack depth upto which Graph Engine attempts to walk. */
    int getStackDepthLimit();

    /** Limit to control the growth of path expansion to help alleviate OutOfMemoryError. */
    int getPathExpansionLimit();

    /** Filename of data that stores Files to Entries * */
    String getFilesToEntriesCacheLocation();

    /** Indicates if caching should be disabled in the current run * */
    boolean isCachingDisabled();
}
