package com.salesforce.exception;

import com.salesforce.telemetry.TelemetryUtil;

public abstract class SfgeRuntimeException extends RuntimeException {
    public SfgeRuntimeException() {
        super();
        TelemetryUtil.postExceptionTelemetry(
                this.getClass().getSimpleName() + ": No message available");
    }

    public SfgeRuntimeException(Throwable cause) {
        super(cause);
        String telemetryMessage =
                String.format("%s: %s", this.getClass().getSimpleName(), cause.getMessage());
        TelemetryUtil.postExceptionTelemetry(telemetryMessage);
    }

    public SfgeRuntimeException(String msg) {
        super(msg);
        String telemetryMessage = String.format("%s: %s", this.getClass().getSimpleName(), msg);
        TelemetryUtil.postExceptionTelemetry(telemetryMessage);
    }

    public SfgeRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
        String telemetryMessage = String.format("%s: %s", this.getClass().getSimpleName(), msg);
        TelemetryUtil.postExceptionTelemetry(telemetryMessage);
    }
}
