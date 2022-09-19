package com.salesforce.exception;

import com.salesforce.telemetry.TelemetryUtil;

public abstract class SfgeRuntimeException extends RuntimeException {
    public SfgeRuntimeException() {
        super();
        TelemetryUtil.postExceptionTelemetry(this);
    }

    public SfgeRuntimeException(Throwable cause) {
        super(cause);
        TelemetryUtil.postExceptionTelemetry(this, cause);
    }

    public SfgeRuntimeException(String msg) {
        super(msg);
        TelemetryUtil.postExceptionTelemetry(this);
    }

    public SfgeRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
        TelemetryUtil.postExceptionTelemetry(this, cause);
    }
}
