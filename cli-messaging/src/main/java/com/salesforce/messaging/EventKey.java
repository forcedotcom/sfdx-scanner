package com.salesforce.messaging;
import static com.salesforce.messaging.Message.*;

public enum EventKey {
	// MAKE SURE `messageKey` OF EVERY VALUE ADDED HERE HAS AN ENTRY IN 'messages/EventKeyTemplates.js'!
	WARNING_MULTIPLE_METHOD_TARGET_MATCHES("warning.multipleMethodTargetMatches", 2, MessageType.WARNING, MessageHandler.UX, true),
	WARNING_NO_METHOD_TARGET_MATCHES("warning.noMethodTargetMatches", 1, MessageType.WARNING, MessageHandler.UX, false);

	final String messageKey;
	final int argCount;
	final MessageType messageType;
	final MessageHandler messageHandler;
	final boolean verbose;//true: only when verbose is true, false: ignores verbose flag and always prints

	EventKey(String messageKey, int argCount, MessageType messageType, MessageHandler messageHandler, boolean verbose) {
		this.messageKey = messageKey;
		this.argCount = argCount;
		this.messageType = messageType;
		this.messageHandler = messageHandler;
		this.verbose = verbose;
	}

	public String getMessageKey() {
		return this.messageKey;
	}

	public int getArgCount() {
		return this.argCount;
	}

	public MessageType getMessageType() {
		return this.messageType;
	}

	public MessageHandler getMessageHandler() {
		return this.messageHandler;
	}

	public boolean isVerbose() {
		return this.verbose;
	}
}
