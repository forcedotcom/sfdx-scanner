package sfdc.sfdx.scanner.pmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.salesforce.messaging.CliMessager;
import com.salesforce.messaging.EventKey;
import com.salesforce.messaging.Message;
import com.salesforce.messaging.MessagePassableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MainMessagesTest {

	@BeforeEach
	@AfterEach
	public void clearMessages() {
		CliMessager.getInstance().resetMessages();
	}

	@Test
	public void verifySfdxScannerExceptionsToMessages() {
		final EventKey expectedEventKey = EventKey.ERROR_INTERNAL_UNEXPECTED;
		final String[] expectedArgs = {"dummy arg"};
		final MessagePassableException exception = new MessagePassableException(expectedEventKey, expectedArgs);

		// Setup mock
		final Main.Dependencies dependencies = setupMockToThrowException(exception);

		// Execute
		new Main(dependencies).mainInternal(new String[]{"apex=blah"});

		// Validate
		final List<Message> messages = getMessages();
		assertEquals(1, messages.size(), "Unexpected count of messages");
		final Message actualMessage = messages.get(0);

		// Validate message
		assertEquals(expectedEventKey.getMessageKey(), actualMessage.getMessageKey(), "Unexpected eventKey in message");
		assertEquals(actualMessage.getArgs().get(0), expectedArgs[0], "Unexpected args in message");
	}

	@Test
	public void verifyAnyThrowableAddedToMessages() {
		final RuntimeException exception = new RuntimeException("Some dummy message");
		final Main.Dependencies dependencies = setupMockToThrowException(exception);

		// Execute
		new Main(dependencies).mainInternal(new String[]{"apex=blah"});

		// Validate
		List<Message> messages = getMessages();
		assertEquals(1, messages.size(), "Unexpected count of messages");
		final Message actualMessage = messages.get(0);

		// Validate message
		assertEquals(EventKey.ERROR_INTERNAL_UNEXPECTED.getMessageKey(), actualMessage.getMessageKey(), "Unexpected eventKey in message when handling uncaught exception");
		final String actualLog = actualMessage.getInternalLog();
		assertTrue(actualLog.contains(exception.getMessage()), "log field of message should contain message from actual exception");
	}

	private Main.Dependencies setupMockToThrowException(Exception exception) {
		Main.Dependencies dependencies = mock(Main.Dependencies.class);
		final PmdRuleCataloger prc = mock(PmdRuleCataloger.class);
		doThrow(exception).when(prc).catalogRules();
		doReturn(prc).when(dependencies).getPmdRuleCataloger(any());
		return dependencies;
	}

	private List<Message> getMessages() {
		final String messagesInJson = CliMessager.getInstance().getAllMessages();
		assertNotNull(messagesInJson);

		// Deserialize JSON to verify further
		final List<Message> messages = new Gson().fromJson(messagesInJson, new TypeToken<List<Message>>() {
		}.getType());
		assertNotNull(messages);
		return messages;
	}
}
