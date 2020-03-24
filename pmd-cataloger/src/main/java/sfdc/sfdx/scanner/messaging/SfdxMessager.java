package sfdc.sfdx.scanner.messaging;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import sfdc.sfdx.scanner.EventKey;
import sfdc.sfdx.scanner.SfdxScannerException;

enum MessageType {
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

  public void uxWarn(EventKey key, List<String> args) {
    uxWarn(key, args, false);
  }

  public void uxWarn(EventKey key, List<String> args, boolean verbose) {
    // TODO: delete in the next iteration
    System.err.println(formatMessage(key, args, MessageType.WARNING, MessageHandler.UX, verbose));
    final Message message = new Message(key, args, "", MessageType.WARNING, MessageHandler.UX, verbose);
    MESSAGES.add(message);
  }

  public void uxError(SfdxScannerException exception) {
    final Message message = new Message(exception.getEventKey(), exception.getArgs(), exception.getFullStacktrace(), MessageType.ERROR, MessageHandler.UX, false);
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

  // TODO: delete in the next iteration
  private String formatMessage(EventKey key, List<String> args, MessageType type, MessageHandler handler, boolean verbose) {
    // A message is created by serializing an SfdxMessage instance and sandwiching it between the START and END strings.
    return START + new Message(key, args, "", type, handler, verbose).toJson() + END;
  }


}

