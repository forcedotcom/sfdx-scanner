package com.salesforce.messaging;

import java.time.Instant;
import java.util.List;

public class Message {
	final private String messageKey;
	final private List<String> args;
	final private String internalLog;
	final private MessageType type;
	final private MessageHandler handler;
	final private boolean verbose;
	final private long time;

	Message(String messageKey, List<String> args, String internalLog, MessageType type, MessageHandler handler, boolean verbose) {
		this.messageKey = messageKey;
		this.args = args;
		this.internalLog = internalLog;
		this.type = type;
		this.handler = handler;
		this.verbose = verbose;
		this.time = Instant.now().toEpochMilli();
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

	enum MessageHandler {
		UX,
		INTERNAL
	}

	enum MessageType {
		INFO,
		WARNING,
		ERROR
	}
}
