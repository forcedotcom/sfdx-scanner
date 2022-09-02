package com.salesforce.logging;

import com.salesforce.messaging.CliMessager;
import com.salesforce.messaging.EventKey;
import org.apache.logging.log4j.Logger;

public class LoggerUtil {

    public static void warnAndPostTelemetry(Logger logger, String msg) {
        CliMessager.postMessage("Telemetry data", EventKey.INFO_TELEMETRY, msg);
        if (logger.isWarnEnabled()) {
            logger.warn(msg);
        }
    }

    private LoggerUtil() {}
}
