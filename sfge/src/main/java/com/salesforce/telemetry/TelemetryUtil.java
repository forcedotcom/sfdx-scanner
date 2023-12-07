package com.salesforce.telemetry;

import com.google.gson.Gson;
import com.salesforce.exception.SfgeRuntimeException;
import com.salesforce.messaging.CliMessager;
import com.salesforce.messaging.EventKey;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public final class TelemetryUtil {
    private static final String MAIN_THREAD_NAME = "main";

    /**
     * Posts a telemetry event in response to a {@link org.apache.logging.log4j.Logger#warn} call.
     *
     * @param message - The message being logged.
     * @param cause - The exception attached to the log, if available.
     */
    public static void postWarningTelemetry(String message, @Nullable Throwable cause) {
        EventType eventType =
                isMainThread() ? EventType.MAIN_THREAD_WARNING : EventType.RULE_THREAD_WARNING;
        StackTraceElement[] trace =
                cause == null ? Thread.currentThread().getStackTrace() : cause.getStackTrace();
        postTelemetry(message, trace, eventType);
    }

    /**
     * Posts a telemetry event in response to an {@link
     * com.salesforce.exception.SfgeRuntimeException} being thrown.
     *
     * @param runtimeException - The exception being thrown.
     */
    public static void postExceptionTelemetry(SfgeRuntimeException runtimeException) {
        postExceptionTelemetry(runtimeException, null);
    }

    /**
     * Posts a telemetry event in response to an {@link
     * com.salesforce.exception.SfgeRuntimeException} being thrown.
     *
     * @param runtimeException - The exception being thrown.
     * @param cause - The exception that caused the runtimeException, if available.
     */
    public static void postExceptionTelemetry(
            SfgeRuntimeException runtimeException, @Nullable Throwable cause) {
        EventType eventType =
                isMainThread() ? EventType.MAIN_THREAD_EXCEPTION : EventType.RULE_THREAD_EXCEPTION;
        StackTraceElement[] trace =
                cause == null ? runtimeException.getStackTrace() : cause.getStackTrace();
        postTelemetry(runtimeException.getMessage(), trace, eventType);
    }

    private static void postTelemetry(
            String message, StackTraceElement[] trace, EventType eventType) {
        TelemetryData telemetryData = new TelemetryData(message, trace, eventType);
        CliMessager.postMessage(
                "TelemetryData", EventKey.INFO_TELEMETRY, new Gson().toJson(telemetryData));
    }

    /** Returns a boolean indicating whether this thread is the main thread. */
    private static boolean isMainThread() {
        // Assumption: The name of the main thread will consistently be "main".
        // If this assumption is invalidated, this code must change.
        return MAIN_THREAD_NAME.equalsIgnoreCase(Thread.currentThread().getName());
    }

    /** An object that can be used as the base for a Salesforce CLI telemetry event. */
    private static class TelemetryData {
        /** Necessary property for telemetry objects. */
        private final String eventName = "SFGE_TELEMETRY";

        private final String message;
        private final EventType eventType;
        private final String stackTrace;

        public TelemetryData(String message, StackTraceElement[] trace, EventType eventType) {
            this.message = message;
            this.eventType = eventType;
            this.stackTrace =
                    Arrays.stream(trace)
                            // We want five-ish stack frames of meaningful context, and it's
                            // possible for the first frame to just be a TelemetryUtil method.
                            // So we'll get the first six frames.
                            .limit(6)
                            .map(StackTraceElement::toString)
                            .collect(Collectors.joining("\n"));
        }
    }

    private enum EventType {
        /**
         * Indicates that an unsupported scenario was encountered in the main thread, and in
         * response a warning was logged but execution continued to the best of its ability.
         */
        MAIN_THREAD_WARNING,
        /**
         * Indicates that an unsupported scenario was encountered in a rule executor thread, and in
         * response a warning was logged but execution continued to the best of its ability.
         */
        RULE_THREAD_WARNING,
        /**
         * Indicates than an unsupported scenario was encountered in the main thread, and that an
         * exception was thrown in response. This typically means that the process as a whole was
         * forced to exit unsuccessfully.
         */
        MAIN_THREAD_EXCEPTION,
        /**
         * Indicates that an unsupported scenario was encountered in a rule executor thread, and
         * that an exception was thrown in response. This typically means that rules can no longer
         * meaningfully execute against the relevant path entrypoint, but other entrypoints can
         * still be evaluated.
         */
        RULE_THREAD_EXCEPTION;
    }

    private TelemetryUtil() {}
}
