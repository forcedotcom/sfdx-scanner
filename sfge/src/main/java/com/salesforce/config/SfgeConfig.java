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

    /** Should be used to check if Warning Violations are enabled. */
    boolean isWarningViolationEnabled();

    /** Indicates if a Jorje parse error causes the entire process to stop. */
    boolean shouldIgnoreParseErrors();
}
