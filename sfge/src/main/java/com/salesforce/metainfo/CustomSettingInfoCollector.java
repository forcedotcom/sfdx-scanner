package com.salesforce.metainfo;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Collects names of custom settings from project information. This is done by looking at object
 * meta xml files. If the Xml contains a customSettingsType node, we have a custom setting. The
 * file's name indicates the name of the custom setting.
 */
public class CustomSettingInfoCollector extends XmlMetaInfoCollector {
    private static final Logger LOGGER = LogManager.getLogger(CustomSettingInfoCollector.class);
    private static final String CUSTOM_OBJECT_ROOT = "CustomObject";
    private static final String CUSTOM_SETTINGS_TYPE_NODE = "customSettingsType";
    private static final String OBJECT_FILE_NAME_SUFFIX = ".object-meta.xml";
    private static final String OBJECT_FILE_NAME_PATTERN = OBJECT_FILE_NAME_SUFFIX + "$";
    private static final String CUSTOM_SETTING_PATTERN_STRING = "(.*)" + OBJECT_FILE_NAME_PATTERN;
    private static final Pattern CUSTOM_SETTING_PATTERN =
            Pattern.compile(CUSTOM_SETTING_PATTERN_STRING, Pattern.CASE_INSENSITIVE);

    @Override
    HashSet<String> getPathPatterns() {
        final HashSet<String> pathPatterns = new HashSet<>();
        // Pattern to check if filename ends with .object-meta.xml
        pathPatterns.add(OBJECT_FILE_NAME_PATTERN);
        return pathPatterns;
    }

    @Override
    void collectMetaInfo(Path path, Document xmlDocument) {
        // Check if we are indeed looking into a CustomObject xml
        final String baseNode = xmlDocument.getFirstChild().getNodeName();
        if (!CUSTOM_OBJECT_ROOT.equalsIgnoreCase(baseNode)) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Xml[{}] does not have CustomObject base node: {}", path, baseNode);
            }
            return;
        }

        // Check if it has CustomSettingsType
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Checking path {} if it is a custom settings", path);
        }
        final NodeList customSettingsTypeElements =
                xmlDocument.getElementsByTagName(CUSTOM_SETTINGS_TYPE_NODE);
        if (customSettingsTypeElements.getLength() > 0) {
            // We have a custom setting
            // TODO: having multiple customSettingsType is incorrect. But should we check that here?

            final Optional<String> customSettingNameOptional = getCustomSettingName(path);
            if (customSettingNameOptional.isPresent()) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            "Identified {} as a custom settings. Thread: {}",
                            customSettingNameOptional.get(),
                            Thread.currentThread());
                }
                collectedMetaInfo.add(customSettingNameOptional.get());
            }
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} is not a Custom settings", path);
            }
        }
    }

    /** @return Get custom settings name from filename */
    private Optional<String> getCustomSettingName(Path path) {
        final String fileName = path.getName(path.getNameCount() - 1).toString();
        final Matcher matcher = CUSTOM_SETTING_PATTERN.matcher(fileName);
        if (matcher.matches()) {
            return Optional.of(matcher.group(1));
        }

        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(
                    "Could not determine custom setting name from filename {}. Full path: {}",
                    fileName,
                    path);
        }
        return Optional.empty();
    }

    protected static final class LazyHolder {
        // Postpone initialization until first use.
        protected static final CustomSettingInfoCollector INSTANCE =
                new CustomSettingInfoCollector();
    }

    static CustomSettingInfoCollector getInstance() {
        return CustomSettingInfoCollector.LazyHolder.INSTANCE;
    }
}
