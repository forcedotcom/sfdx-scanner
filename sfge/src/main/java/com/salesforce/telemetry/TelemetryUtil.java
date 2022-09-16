package com.salesforce.telemetry;

import com.google.gson.Gson;
import com.salesforce.messaging.CliMessager;
import com.salesforce.messaging.EventKey;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class TelemetryUtil {
    private static final String MAIN_THREAD_NAME = "main";

    /**
     * Posts a telemetry event in response to a {@link org.apache.logging.log4j.Logger#warn} call.
     *
     * @param message - The message being logged.
     */
    public static void postWarningTelemetry(String message) {
        EventType eventType =
                isMainThread() ? EventType.MAIN_THREAD_WARNING : EventType.RULE_THREAD_WARNING;
        postTelemetry(message, eventType);
    }

    /**
     * Posts a telemetry event in response to an {@link
     * com.salesforce.exception.SfgeRuntimeException} being thrown.
     *
     * @param message - The exception's message, if available.
     */
    public static void postExceptionTelemetry(String message) {
        EventType eventType =
                isMainThread() ? EventType.MAIN_THREAD_EXCEPTION : EventType.RULE_THREAD_EXCEPTION;
        postTelemetry(message, eventType);
    }

    private static void postTelemetry(String message, EventType eventType) {
        TelemetryData telemetryData = new TelemetryData(message, eventType);
        CliMessager.postMessage(
                "TelemetryData", EventKey.INFO_TELEMETRY, new Gson().toJson(telemetryData));
    }

    /** Returns a boolean indicating whether this thread is the main thread. */
    private static boolean isMainThread() {
        // Assumption: The name of the main thread will consistently be "main".
        // If this assumption is invalidated, this code must change.
        return MAIN_THREAD_NAME.equalsIgnoreCase(Thread.currentThread().getName());
    }

    /** An object that can be used as the base for an SFDX telemetry event. */
    private static class TelemetryData {
        /** Necessary property for telemetry objects. */
        private final String eventName = "SFGE_TELEMETRY";

        private final String message;
        private final EventType eventType;
        private final String stackTrace;

        public TelemetryData(String message, EventType eventType) {
            this.message = message;
            this.eventType = eventType;
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            this.stackTrace =
                    Arrays.stream(stackTraceElements)
                            // We want five-ish stack frames of meaningful context, and the first
                            // two
                            // frames are always going to be this constructor and one of the
                            // TelemetryUtil methods (which aren't terribly meaningful). So we'll
                            // get the first seven stack frames.
                            .limit(7)
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
