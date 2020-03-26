package sfdc.sfdx.scanner.messaging;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Parses messages/EventKeyTemplates.json and confirms that enums defined in EventKey
 * are valid
 */
@RunWith(Parameterized.class)
public class EventKeyTest {
  private static final String INFO = "info";
  private static final String WARNING = "warning";
  private static final String ERROR_EXTERNAL = "error.external";
  private static final String ERROR_INTERNAL = "error.internal";

  // Current path is sfdx-scanner/pmd-cataloger
  private static final String MESSAGES_FILE = "../messages/EventKeyTemplates.json";

  JSONObject jsonObject = null;

  @Before
  public void extractMessagesJson() throws IOException, ParseException {
    final Path path = Paths.get(MESSAGES_FILE);
    assertTrue("Invalid test setup. File does not exist: " + MESSAGES_FILE, Files.exists(path));
    final String jsonContent = new String(Files.readAllBytes(path));
    jsonObject = (JSONObject) new JSONParser().parse(jsonContent);
    assertNotNull("Invalid test setup. Messages json has not been parsed correctly. Please check validity of " + MESSAGES_FILE, jsonObject);
  }

  @Test
  public void verifyKeyInJson() {
    // Split messageKey into levels
    final String messageKey = eventKey.getMessageKey();
    final String[] levels = messageKey.split("\\.");

    // Loop through JSON to verify presence of each level
    int idx = 0;
    JSONObject currentJsonContent = this.jsonObject;
    while (idx < levels.length - 1) {
      currentJsonContent = (JSONObject) currentJsonContent.get(levels[idx]);
      assertNotNull("Level " + levels[idx] + " not found. Recheck value of messageKey " + messageKey + " in EventKey." + eventKey, currentJsonContent);
      idx++;
    }
    final Object lastLevel = currentJsonContent.get(levels[levels.length - 1]);
    assertNotNull("messageKey " + messageKey + " does not exist. Recheck EventKey." + eventKey, lastLevel);
    assertTrue("Message value should be a String for messageKey " + messageKey + " in EventKey." + eventKey, lastLevel instanceof String);
  }

  @Test
  public void verifyInfo() {
    if (!eventKey.getMessageKey().startsWith(INFO)) {
      return;
    }
    assertEquals("Unexpected messageType on EventKey." + eventKey, MessageType.INFO, eventKey.getMessageType());
    assertTrue("Verbose value on INFO messages are expected to be True. Please recheck EventKey." + eventKey, eventKey.isVerbose());
  }

  @Test
  public void verifyWarning() {
    if (!eventKey.getMessageKey().startsWith(WARNING)) {
      return;
    }
    assertEquals("Unexpected messageType on EventKey." + eventKey, MessageType.WARNING, eventKey.getMessageType());
    // No verbose check since we don't have a rule yet
  }

  @Test
  public void verifyErrorExternal() {
    if (!eventKey.getMessageKey().startsWith(ERROR_EXTERNAL)) {
      return;
    }
    assertEquals("Unexpected messageType on EventKey." + eventKey, MessageType.ERROR, eventKey.getMessageType());
    assertFalse("Verbose value on external error messages should be True. Please recheck EventKey." + eventKey, eventKey.isVerbose());
    assertEquals("MessageHandler on external error messages should be UX. Please recheck EventKey." + eventKey, MessageHandler.UX, eventKey.getMessageHandler());
  }

  @Test
  public void verifyErrorInternal() {
    if (!eventKey.getMessageKey().startsWith(ERROR_INTERNAL)) {
      return;
    }
    assertEquals("Unexpected messageType on EventKey." + eventKey, MessageType.ERROR, eventKey.getMessageType());
    assertFalse("Verbose value on internal error messages should be True. Please recheck EventKey." + eventKey, eventKey.isVerbose());
    assertEquals("MessageHandler on internal error messages should be INTERNAL. Please recheck EventKey." + eventKey, MessageHandler.INTERNAL, eventKey.getMessageHandler());
  }

  // Needed to make this test run dynamically for each EntryKey value

  private EventKey eventKey;

  public EventKeyTest(EventKey eventKey) {
    this.eventKey = eventKey;
  }

  @Parameters
  public static Collection<Object[]> fetchEventKeys() {
    Collection<Object[]> data =  new ArrayList<>();
    for (EventKey eventKey: EventKey.values()) {
      data.add(new Object[]{ eventKey });
    }
    return data;
  }
}
