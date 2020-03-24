package sfdc.sfdx.scanner.messaging;

import com.google.gson.Gson;
import sfdc.sfdx.scanner.EventKey;

import java.time.Instant;
import java.util.List;

public class Message {
  private EventKey key;
  private List<String> args;
  private String log;
  private MessageType type;
  private MessageHandler handler;
  private boolean verbose;
  private long time;

  Message(EventKey key, List<String> args, String log, MessageType type, MessageHandler handler, boolean verbose) {
    this.key = key;
    this.args = args;
    this.log = log;
    this.type = type;
    this.handler = handler;
    this.time = Instant.now().toEpochMilli();
    this.verbose = verbose;
  }

  String toJson() {
    return new Gson().toJson(this);
  }

  public EventKey getKey() {
    return key;
  }

  public List<String> getArgs() {
    return args;
  }

  public String getLog() {
    return log;
  }

}
