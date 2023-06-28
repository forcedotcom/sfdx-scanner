package sfdc.sfdx.scanner.telemetry;

import com.google.gson.Gson;
import com.salesforce.messaging.CliMessager;
import com.salesforce.messaging.EventKey;

public class TelemetryUtil {

    public static void postTelemetry(AbstractTelemetryData data) {
        CliMessager.postMessage("TelemetryData", EventKey.INFO_TELEMETRY, new Gson().toJson(data));
    }

    public abstract static class AbstractTelemetryData {
        /** Necessary property for telemetry objects. */
        private final String eventName = "PMD_CATALOGER_TELEMETRY";
    }
}
