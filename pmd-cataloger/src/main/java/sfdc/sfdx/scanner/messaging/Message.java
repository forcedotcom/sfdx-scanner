package sfdc.sfdx.scanner.messaging;

import com.google.gson.Gson;
import static sfdc.sfdx.scanner.messaging.SfdxMessager.*;
import java.time.Instant;
import java.util.List;

public class Message {
	private String messageKey;
	private List<String> args;
	private String internalLog;
	private MessageType type;
	private MessageHandler handler;
	private boolean verbose;
	private long time;

	Message(String messageKey, List<String> args, String internalLog, MessageType type, MessageHandler handler, boolean verbose) {
		this.messageKey = messageKey;
		this.args = args;
		this.internalLog = internalLog;
		this.type = type;
		this.handler = handler;
		this.time = Instant.now().toEpochMilli();
		this.verbose = verbose;
	}

	String toJson() {
		return new Gson().toJson(this);
	}

	public String getMessageKey() {
		return messageKey;
	}

	public List<String> getArgs() {
		return args;
	}

	public String getInternalLog() {
		return internalLog;
	}

}
