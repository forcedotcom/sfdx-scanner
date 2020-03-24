package sfdc.sfdx.scanner.pmd;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.After;
import org.junit.Test;
import sfdc.sfdx.scanner.EventKey;
import sfdc.sfdx.scanner.SfdxScannerException;
import sfdc.sfdx.scanner.messaging.Message;
import sfdc.sfdx.scanner.messaging.SfdxMessager;

import java.util.List;

import static org.mockito.Mockito.*;

public class MainMessagesTest {

  @After
  public void clearMessages() {
    SfdxMessager.getInstance().resetMessages();
  }

  @Test
  public void verifySfdxScannerExceptionsToMessages() {
    final EventKey expectedEventKey = EventKey.ERROR_UNEXPECTED;
    final String[] expectedArgs = {"dummy arg"};
    final SfdxScannerException exception = new SfdxScannerException(expectedEventKey, expectedArgs);

    // Setup mock
    final Main.Dependencies dependencies = setupMockToThrowException(exception);

    // Execute
    new Main(dependencies).mainInternal(new String[]{"apex=blah"});

    // Validate
    final List<Message> messages = getMessages();
    assertEquals("Unexpected count of messages", 1, messages.size());
    final Message actualMessage = messages.get(0);

    // Validate message
    assertEquals("Unexpected eventKey in message", expectedEventKey, actualMessage.getKey());
    assertEquals("Unexpected args in message", actualMessage.getArgs().get(0), expectedArgs[0]);
  }

  @Test
  public void verifyAnyThrowableAddedToMessages() {
    final RuntimeException exception = new RuntimeException("Some dummy message");
    final Main.Dependencies dependencies = setupMockToThrowException(exception);

    // Execute
    new Main(dependencies).mainInternal(new String[]{"apex=blah"});

    // Validate
    List<Message> messages = getMessages();
    assertEquals("Unexpected count of messages", 1, messages.size());
    final Message actualMessage = messages.get(0);

    // Validate message
    assertEquals("Unexpected eventKey in message when handling uncaught exception", EventKey.ERROR_UNEXPECTED, actualMessage.getKey());
    final String actualLog = actualMessage.getLog();
    assertTrue("log field of message should contain message from actual exception", actualLog.contains(exception.getMessage()));
  }

  private Main.Dependencies setupMockToThrowException(Exception exception) {
    Main.Dependencies dependencies = mock(Main.Dependencies.class);
    final PmdRuleCataloger prc = mock(PmdRuleCataloger.class);
    doThrow(exception).when(prc).catalogRules();
    doReturn(prc).when(dependencies).getPmdRuleCataloger(any());
    return dependencies;
  }

  private List<Message> getMessages() {
    final String messagesInJson = SfdxMessager.getInstance().getAllMessages();
    assertNotNull(messagesInJson);

    // Deserialize JSON to verify further
    final List<Message> messages = new Gson().fromJson(messagesInJson, new TypeToken<List<Message>>(){}.getType());
    assertNotNull(messages);
    return messages;
  }
}
