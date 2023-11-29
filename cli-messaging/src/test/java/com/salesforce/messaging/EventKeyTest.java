package com.salesforce.messaging;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.salesforce.messaging.Message.*;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Parses messages/EventKeyTemplates.json and confirms that enums defined in EventKey
 * are valid
 */
public class EventKeyTest {
    private static final String INFO_PREFIX = "info";
    private static final String WARNING_PREFIX = "warning";
    private static final String ERROR_EXTERNAL_PREFIX = "error.external";
    private static final String ERROR_INTERNAL_PREFIX = "error.internal";

    // Current path is sfdx-scanner/pmd-cataloger
    private static final String MESSAGES_FILE = "../messages/EventKeyTemplates.js";

    JSONObject jsonObject = null;

    @BeforeEach
    public void extractMessagesJson() throws IOException, ParseException {
        final Path path = Paths.get(MESSAGES_FILE);
        assertThat("Invalid test setup. File does not exist: " + MESSAGES_FILE, Files.exists(path), is(true));
        final String fileContent = new String(Files.readAllBytes(path));
        final String[] fileSplit = fileContent.split("=");
        final int fileParts = fileSplit.length;
        assertThat("Invalid test setup. File has more than one '=', which caused confusion in picking JSON content. Please revisit messages in " + MESSAGES_FILE, fileParts, is(2));
        final String jsonContent = fileSplit[1];
        jsonObject = (JSONObject) new JSONParser().parse(jsonContent);
        assertThat("Invalid test setup. Messages json has not been parsed correctly. Please check validity of " + MESSAGES_FILE, jsonObject, is(notNullValue()));
    }

    @ParameterizedTest(name = "eventKey={0}")
    @MethodSource("getAllEventKeyValues")
    public void verifyKeyInJson(EventKey eventKey) {
        // Split messageKey into levels
        final String messageKey = eventKey.getMessageKey();
        final String[] levels = messageKey.split("\\.");

        // Loop through JSON to verify presence of each level
        int idx = 0;
        JSONObject currentJsonContent = this.jsonObject;
        while (idx < levels.length - 1) {
            currentJsonContent = (JSONObject) currentJsonContent.get(levels[idx]);
            assertThat("Level " + levels[idx] + " not found. Recheck value of messageKey " + messageKey + " in EventKey." + eventKey, currentJsonContent, is(notNullValue()));
            idx++;
        }
        final Object lastLevel = currentJsonContent.get(levels[levels.length - 1]);
        assertThat("messageKey " + messageKey + " does not exist. Recheck EventKey." + eventKey, lastLevel, is(notNullValue()));
        assertThat("Message value should be a String for messageKey " + messageKey + " in EventKey." + eventKey, lastLevel instanceof String, is(true));
    }

    @ParameterizedTest(name = "eventKey={0}")
    @MethodSource("getAllInfoEventKeyValues")
    public void verifyInfo(EventKey eventKey) {
        assertThat("Unexpected messageType on EventKey." + eventKey, eventKey.getMessageType(), is(MessageType.INFO));
        // No verbose check since we don't have a specific rule for verbosity on info
    }

    @ParameterizedTest(name = "eventKey={0}")
    @MethodSource("getAllWarningEventKeyValues")
    public void verifyWarning(EventKey eventKey) {
        assertThat("Unexpected messageType on EventKey." + eventKey, eventKey.getMessageType(), is(MessageType.WARNING));
        // No verbose check since we don't have a specific rule for verbosity on warning
    }

    @ParameterizedTest(name = "eventKey={0}")
    @MethodSource("getAllErrorExternalEventKeyValues")
    public void verifyErrorExternal(EventKey eventKey) {
        assertThat("Unexpected messageType on EventKey." + eventKey, eventKey.getMessageType(), is(MessageType.ERROR));
        assertThat("Verbose value on external error messages should be False. Please recheck EventKey." + eventKey, eventKey.isVerbose(), is(false));
        assertThat("MessageHandler on external error messages should be UX. Please recheck EventKey." + eventKey, eventKey.getMessageHandler(), is(MessageHandler.UX));
    }

    @ParameterizedTest(name = "eventKey={0}")
    @MethodSource("getAllErrorInternalEventKeyValues")
    public void verifyErrorInternal(EventKey eventKey) {
        assertThat("Unexpected messageType on EventKey." + eventKey, eventKey.getMessageType(), is(MessageType.ERROR));
        assertThat("Verbose value on internal error messages should be False. Please recheck EventKey." + eventKey, eventKey.isVerbose(), is(false));
        assertThat("MessageHandler on internal error messages should be INTERNAL. Please recheck EventKey." + eventKey, eventKey.getMessageHandler(), is(MessageHandler.INTERNAL));
    }

    public static Stream<EventKey> getAllEventKeyValues() {
        return Arrays.stream(EventKey.values());
    }

    public static Stream<EventKey> getAllInfoEventKeyValues() {
        return getAllEventKeyValues().filter(eventKey -> eventKey.getMessageKey().startsWith(INFO_PREFIX)
            && eventKey != EventKey.INFO_TELEMETRY);
    }

    public static Stream<EventKey> getAllWarningEventKeyValues() {
        return getAllEventKeyValues().filter(eventKey -> eventKey.getMessageKey().startsWith(WARNING_PREFIX));
    }

    public static Stream<EventKey> getAllErrorExternalEventKeyValues() {
        return getAllEventKeyValues().filter(eventKey -> eventKey.getMessageKey().startsWith(ERROR_EXTERNAL_PREFIX));
    }

    public static Stream<EventKey> getAllErrorInternalEventKeyValues() {
        return getAllEventKeyValues().filter(eventKey -> eventKey.getMessageKey().startsWith(ERROR_INTERNAL_PREFIX));
    }
}
