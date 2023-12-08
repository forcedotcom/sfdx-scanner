package com.salesforce.messaging;

import org.json.simple.parser.ParseException;

import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.salesforce.messaging.Message.*;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses messages/EventKeyTemplates.md and confirms that enums defined in EventKey
 * are valid
 */
public class EventKeyTest {
    private static final String INFO_PREFIX = "info";
    private static final String WARNING_PREFIX = "warning";
    private static final String ERROR_EXTERNAL_PREFIX = "error.external";
    private static final String ERROR_INTERNAL_PREFIX = "error.internal";

    // Current path is sfdx-scanner/pmd-cataloger
    private static final String MESSAGES_FILE = "../messages/EventKeyTemplates.md";

    /**
     * This list will hold the keys that reside in {@code <projectroot>/messages/EventKeyTemplates.md}.
     */
    Set<String> eventKeyTemplatesMdKeys = null;

    @BeforeEach
    public void extractMessagesJson() throws IOException, ParseException {
        final Path path = Paths.get(MESSAGES_FILE);
        assertThat("Invalid test setup. File does not exist: " + MESSAGES_FILE, Files.exists(path), is(true));
        final List<String> fileLines = Files.readAllLines(path);
        eventKeyTemplatesMdKeys = fileLines.stream().filter(s -> s.startsWith("#")).map(s -> s.substring(1).trim()).collect(Collectors.toSet());
    }

    /**
     * Verifies that every {@link EventKey}'s {@link EventKey#getMessageKey()} result corresponds
     * to an entry in {@code ./messages/EventKeyTemplates.md}.
     * @param eventKey
     */
    @ParameterizedTest(name = "eventKey={0}")
    @MethodSource("getAllEventKeyValues")
    public void verifyKeyInJson(EventKey eventKey) {
        final String messageKey = eventKey.getMessageKey();
        assertThat("EventKey." + eventKey.name() + "'s messageKey property is missing from `./messages/EventKeyTemplates.md.", messageKey, is(in(eventKeyTemplatesMdKeys)));
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
