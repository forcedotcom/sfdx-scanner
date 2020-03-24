package sfdc.sfdx.scanner.messaging;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sfdc.sfdx.scanner.EventKey;
import sfdc.sfdx.scanner.SfdxScannerException;

enum MessageType {
  INFO,
  WARNING,
  ERROR
}

enum MessageHandler {
  UX
}

public class SfdxMessager {
  // The START string gives us something to scan for when we're processing output.
  private static final String START = "SFDX-START";
  // The END string lets us know when a message stops, which should prevent bugs involving multi-line output.
  private static final String END = "SFDX-END";

  private static final List<Message> MESSAGES = new ArrayList<>();

  private static SfdxMessager INSTANCE = null;

  public static SfdxMessager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new SfdxMessager();
    }
    return INSTANCE;
  }

  public void uxInfo(boolean verbose, EventKey key, String... args) {
    uxAddMessage(
      verbose,
      MessageType.INFO,
      key,
      args);
  }

  public void uxWarn(boolean verbose, EventKey key, String... args) {
    uxAddMessage(
      verbose,
      MessageType.WARNING,
      key,
      args);
  }

  public void uxError(SfdxScannerException exception) {
    uxAddMessage(
      false,
      MessageType.ERROR,
      MessageHandler.UX,
      exception.getFullStacktrace(),
      exception.getEventKey(),
      exception.getArgs());
  }

  private void uxAddMessage(
    boolean verbose,
    MessageType messageType,
    EventKey key,
    String... args) {
    uxAddMessage(
      verbose,
      messageType,
      MessageHandler.UX,
      "",
      key,
      args);
  }

  private void uxAddMessage(
    boolean verbose,
    MessageType messageType,
    MessageHandler messageHandler,
    String log,
    EventKey eventKey,
    String... args) {
    // Confirm that the correct number of arguments for the message has been provided
    // If this fails, this would be a developer error
    assert (eventKey.getArgCount() == args.length);

    final Message message = new Message(
      eventKey,
      Arrays.asList(args),
      log,
      messageType,
      messageHandler,
      verbose);
    MESSAGES.add(message);
  }


  public String getAllMessagesWithFormatting() {
    final String messagesAsJson = getMessagesAsJson();
    return START + messagesAsJson + END;
  }

  private String getMessagesAsJson() {
    return new Gson().toJson(MESSAGES);
  }


  /**
   * TO BE USED ONLY BY TESTS!
   *
   * @return all messages as JSON without formatting
   */
  public String getAllMessages() {
    return getMessagesAsJson();
  }

  /**
   * TO BE USED ONLY BY TESTS!
   * STAY AWAY!!
   */
  public void resetMessages() {
    MESSAGES.clear();
  }

}

