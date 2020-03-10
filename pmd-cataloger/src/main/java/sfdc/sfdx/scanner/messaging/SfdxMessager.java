package sfdc.sfdx.scanner.messaging;

import com.google.gson.Gson;

enum MessageType {
  WARNING
}

enum MessageHandler {
  UX
}

public class SfdxMessager {
  // The START string gives us something to scan for when we're processing output.
  private static final String START = "SFDX-START";
  // The END string lets us know when a message stops, which should prevent bugs involving multi-line output.
  private static final String END = "SFDX-END";

  private static SfdxMessager INSTANCE = null;

  public static SfdxMessager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new SfdxMessager();
    }
    return INSTANCE;
  }

  public void uxWarn(String msg) {
    uxWarn(msg, false);
  }

  public void uxWarn(String msg, boolean verbose) {
    System.out.println(formatMessage(msg, MessageType.WARNING, MessageHandler.UX, verbose));
  }

  private String formatMessage(String msg, MessageType type, MessageHandler handler, boolean verbose) {
    // A message is created by serializing an SfdxMessage instance and sandwiching it between the START and END strings.
    return START + new SfdxMessage(msg, type, handler, verbose).toJson() + END;
  }
}

class SfdxMessage {
  private MessageType type;
  private MessageHandler handler;
  private boolean verbose;
  private String msg;

  SfdxMessage(String msg, MessageType type, MessageHandler handler, boolean verbose) {
    this.type = type;
    this.handler = handler;
    this.verbose = verbose;
    this.msg = msg;
  }

  String toJson() {
    return new Gson().toJson(this);
  }
}
