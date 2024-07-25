package com.salesforce.config;

import com.google.common.annotations.VisibleForTesting;
import com.salesforce.graph.ops.registry.RegistryDataLimitCalculator;
import java.io.File;
import java.util.concurrent.TimeUnit;

public final class EnvUtil {
    private static final String ENV_RULE_THREAD_COUNT = "SFGE_RULE_THREAD_COUNT";
    private static final String ENV_RULE_THREAD_TIMEOUT = "SFGE_RULE_THREAD_TIMEOUT";
    private static final String ENV_RULE_DISABLE_WARNING_VIOLATION =
            "SFGE_RULE_DISABLE_WARNING_VIOLATION";
    private static final String ENV_LOG_WARNINGS_ON_VERBOSE = "SFGE_LOG_WARNINGS_ON_VERBOSE";
    private static final String ENV_PROGRESS_INCREMENTS = "SFGE_PROGRESS_INCREMENTS";
    private static final String ENV_STACK_DEPTH_LIMIT = "SFGE_STACK_DEPTH_LIMIT";
    private static final String ENV_PATH_EXPANSION_LIMIT = "SFGE_PATH_EXPANSION_LIMIT";
    private static final String ENV_FILES_TO_ENTRIES_CACHE_LOCATION =
            "SFGE_FILES_TO_ENTRIES_CACHE_LOCATION";
    private static final String ENV_DISABLE_CACHING = "SFGE_DISABLE_CACHING";

    // TODO: These should move to SfgeConfigImpl and this class should return Optionals
    @VisibleForTesting
    static final int DEFAULT_RULE_THREAD_COUNT =
            Math.min(Runtime.getRuntime().availableProcessors(), 4);

    @VisibleForTesting
    static final long DEFAULT_RULE_THREAD_TIMEOUT =
            TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES);

    @VisibleForTesting static final boolean DEFAULT_RULE_DISABLE_WARNING_VIOLATION = false;
    @VisibleForTesting static final boolean DEFAULT_LOG_WARNINGS_ON_VERBOSE = false;
    @VisibleForTesting static final int DEFAULT_PROGRESS_INCREMENTS = 10;

    /** Artificial stack depth limit to keep path expansion under control. */
    @VisibleForTesting static final int DEFAULT_STACK_DEPTH_LIMIT = 450;

    @VisibleForTesting
    static final int DEFAULT_PATH_EXPANSION_LIMIT =
            RegistryDataLimitCalculator.getApexPathExpanderRegistryLimit();

    @VisibleForTesting
    static final String DEFAULT_FILES_TO_ENTRIES_CACHE_LOCATION =
            ".sfge-cache" + File.separator + "fileToEntryMapData.json";

    @VisibleForTesting static final boolean DEFAULT_DISABLE_CACHING = true;

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
     * @return value of {@link #ENV_RULE_DISABLE_WARNING_VIOLATION} env variable. If it is not set,
     *     {@link #DEFAULT_RULE_DISABLE_WARNING_VIOLATION}
     */
    static boolean isWarningViolationDisabled() {
        return getBoolOrDefault(
                ENV_RULE_DISABLE_WARNING_VIOLATION, DEFAULT_RULE_DISABLE_WARNING_VIOLATION);
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

    /**
     * Returns stack depth limit upto which Graph Engine attempts to dig. Depths beyond this have a
     * high probability of throwing a StackOverFlow exception.
     */
    static int getStackDepthLimit() {
        return getIntOrDefault(ENV_STACK_DEPTH_LIMIT, DEFAULT_STACK_DEPTH_LIMIT);
    }

    /**
     * @return Maximum limit that items in registry can reach. Checks for env variable and returns
     *     default when not set.
     */
    static int getPathExpansionLimit() {
        return getIntOrDefault(ENV_PATH_EXPANSION_LIMIT, DEFAULT_PATH_EXPANSION_LIMIT);
    }

    static String getFilesToEntriesCacheLocation() {
        return getStringOrDefault(
                ENV_FILES_TO_ENTRIES_CACHE_LOCATION, DEFAULT_FILES_TO_ENTRIES_CACHE_LOCATION);
    }

    static boolean isCachingDisabled() {
        return getBoolOrDefault(ENV_DISABLE_CACHING, DEFAULT_DISABLE_CACHING);
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

    private static String getStringOrDefault(String name, String defaultValue) {
        String strVal = System.getProperty(name);
        return strVal == null ? defaultValue : strVal;
    }

    private EnvUtil() {}
}
