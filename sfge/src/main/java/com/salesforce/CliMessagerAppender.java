package com.salesforce;

import com.salesforce.config.SfgeConfigProvider;
import com.salesforce.messaging.CliMessager;
import com.salesforce.messaging.EventKey;
import java.io.Serializable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Custom log4j2 appender to send logs as realtime events through {@link CliMessager}. This helps
 * streamline logs displayed to commandline users. Invoked from log4j.xml.
 */
@Plugin(
        name = "CliMessagerAppender",
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE)
public class CliMessagerAppender extends AbstractAppender {

    private final boolean shouldLogWarningsOnVerbose;

    @PluginFactory
    public static CliMessagerAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter) {
        if (name == null) {
            // Assign default name to avoid complaining
            name = "CliMessagerAppender";
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new CliMessagerAppender(name, filter, layout, true);
    }

    protected CliMessagerAppender(
            String name,
            Filter filter,
            Layout<? extends Serializable> layout,
            final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions, null);
        this.shouldLogWarningsOnVerbose = SfgeConfigProvider.get().shouldLogWarningsOnVerbose();
    }

    /**
     * {@link CliMessagerAppender} decrements the log level while publishing to CLI. Warning is
     * reduced to Info, Error is reduced to Warning, Fatal is reduced to Error.
     *
     * @param event that was published from code
     */
    @Override
    public void append(LogEvent event) {
        Level level = event.getLevel();
        if (Level.WARN.equals(level) && this.shouldLogWarningsOnVerbose) {
            CliMessager.postMessage(
                    "SFGE Warn as Info", EventKey.INFO_GENERAL, getEventMessage(event));
        } else if (Level.ERROR.equals(level)) {
            CliMessager.postMessage(
                    "SFGE Error as Warning", EventKey.WARNING_GENERAL, getEventMessage(event));
        } else if (Level.FATAL.equals(level)) {
            CliMessager.postMessage(
                    "SFGE Fatal as Error", EventKey.ERROR_GENERAL, getEventMessage(event));
        } else {
            // TODO: revisit how the outliers are handled
            error(
                    String.format(
                            "Unable to log less than WARN level [{}]: {}",
                            event.getLevel(),
                            getEventMessage(event)));
        }
    }

    private String getEventMessage(LogEvent event) {
        return event.getMessage().getFormattedMessage();
    }
}
