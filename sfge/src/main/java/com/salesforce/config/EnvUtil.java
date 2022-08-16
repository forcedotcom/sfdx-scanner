package com.salesforce.config;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.TimeUnit;

public final class EnvUtil {
    private static final String ENV_RULE_THREAD_COUNT = "SFGE_RULE_THREAD_COUNT";
    private static final String ENV_RULE_THREAD_TIMEOUT = "SFGE_RULE_THREAD_TIMEOUT";
    private static final String ENV_RULE_ENABLE_WARNING_VIOLATION =
            "SFGE_RULE_ENABLE_WARNING_VIOLATION";
    private static final String ENV_IGNORE_PARSE_ERRORS = "SFGE_IGNORE_PARSE_ERRORS";
    private static final String ENV_LOG_WARNINGS_ON_VERBOSE = "SFGE_LOG_WARNINGS_ON_VERBOSE";
    private static final String ENV_PROGRESS_INCREMENTS = "SFGE_PROGRESS_INCREMENTS";

    // TODO: These should move to SfgeConfigImpl and this class should return Optionals
    @VisibleForTesting
    static final int DEFAULT_RULE_THREAD_COUNT =
            Math.min(Runtime.getRuntime().availableProcessors(), 4);

    @VisibleForTesting
    static final long DEFAULT_RULE_THREAD_TIMEOUT =
            TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES);

    @VisibleForTesting static final boolean DEFAULT_RULE_ENABLE_WARNING_VIOLATION = true;
    @VisibleForTesting static final boolean DEFAULT_IGNORE_PARSE_ERRORS = false;
    @VisibleForTesting static final boolean DEFAULT_LOG_WARNINGS_ON_VERBOSE = false;
    @VisibleForTesting static final int DEFAULT_PROGRESS_INCREMENTS = 10;

    /**
     * Returns the value of the {@link #ENV_RULE_THREAD_COUNT} environment variable if set, else
     * {@link #DEFAULT_RULE_THREAD_COUNT}. Should be used to set the number of threads that can be
     * spawned to execute rules.
     */
    static int getRuleThreadCount() {
        return getIntOrDefault(ENV_RULE_THREAD_COUNT, DEFAULT_RULE_THREAD_COUNT);
    }

    /**
     * Returns the value of the {@link #ENV_RULE_THREAD_TIMEOUT} environment variable if set, else
     * {@link #DEFAULT_RULE_THREAD_TIMEOUT}. Should be used to set the timeout for rule execution
     * threads.
     */
    static long getRuleThreadTimeout() {
        return getLongOrDefault(ENV_RULE_THREAD_TIMEOUT, DEFAULT_RULE_THREAD_TIMEOUT);
    }

    /**
     * Indicates if info-level violations are enabled
     *
     * @return value of {@link #ENV_RULE_ENABLE_WARNING_VIOLATION} env variable. If it is not set,
     *     {@link #DEFAULT_RULE_ENABLE_WARNING_VIOLATION}
     */
    static boolean isWarningViolationEnabled() {
        return getBoolOrDefault(
                ENV_RULE_ENABLE_WARNING_VIOLATION, DEFAULT_RULE_ENABLE_WARNING_VIOLATION);
    }

    /**
     * Indicates if a Jorje parse error causes the entire process to stop
     *
     * @return value of {@link #ENV_IGNORE_PARSE_ERRORS} env variable. If it is not set, {@link
     *     #DEFAULT_IGNORE_PARSE_ERRORS}
     */
    static boolean shouldIgnoreParseErrors() {
        return getBoolOrDefault(ENV_IGNORE_PARSE_ERRORS, DEFAULT_IGNORE_PARSE_ERRORS);
    }

    /**
     * Indicates if SFGE should log internal warnings on --verbose
     *
     * @return value of {@link #ENV_LOG_WARNINGS_ON_VERBOSE} env variable. If it is not set, {@link
     *     #DEFAULT_LOG_WARNINGS_ON_VERBOSE}
     */
    static boolean shouldLogWarningsOnVerbose() {
        return getBoolOrDefault(ENV_LOG_WARNINGS_ON_VERBOSE, DEFAULT_LOG_WARNINGS_ON_VERBOSE);
    }

    /**
     * Gets the level of increments at which path analysis progress update is provided. Applicable
     * only with --verbose.
     *
     * @return value of {@link #ENV_PROGRESS_INCREMENTS} environment variable if set, else {@link
     *     #DEFAULT_PROGRESS_INCREMENTS}
     */
    static int getProgressIncrements() {
        return getIntOrDefault(ENV_PROGRESS_INCREMENTS, DEFAULT_PROGRESS_INCREMENTS);
    }

    private static int getIntOrDefault(String name, int defaultValue) {
        String strVal = System.getProperty(name);
        return strVal == null ? defaultValue : Integer.parseInt(strVal);
    }

    private static long getLongOrDefault(String name, long defaultValue) {
        String strVal = System.getProperty(name);
        return strVal == null ? defaultValue : Long.parseLong(strVal);
    }

    private static boolean getBoolOrDefault(String name, boolean defaultValue) {
        String strVal = System.getProperty(name);
        return strVal == null ? defaultValue : Boolean.parseBoolean(strVal);
    }

    private EnvUtil() {}
}
