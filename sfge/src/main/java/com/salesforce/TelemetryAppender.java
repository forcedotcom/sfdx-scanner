package com.salesforce;

import com.salesforce.telemetry.TelemetryUtil;
import java.io.Serializable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Custom log4j2 appender to send logs as telemetry events through {@link
 * com.salesforce.telemetry.TelemetryUtil}. This helps us capture telemetry events in response to
 * unsupported/pathological scenarios. Invoked from log4j2.xml
 */
@Plugin(
        name = "TelemetryAppender",
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE)
public class TelemetryAppender extends AbstractAppender {
    @PluginFactory
    public static TelemetryAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter) {
        if (name == null) {
            // Assign default name to avoid complaining
            name = "TelemetryAppender";
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new TelemetryAppender(name, filter, layout, true);
    }

    protected TelemetryAppender(
            String name,
            Filter filter,
            Layout<? extends Serializable> layout,
            final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions, null);
    }

    @Override
    public void append(LogEvent event) {
        Level level = event.getLevel();
        if (Level.WARN.equals(level)) {
            String eventMessage = event.getMessage().getFormattedMessage();
            if (eventMessage.toLowerCase().startsWith("todo:")) {
                TelemetryUtil.postWarningTelemetry(
                        this.getLayout().toSerializable(event).toString(), event.getThrown());
            }
        }
    }
}
