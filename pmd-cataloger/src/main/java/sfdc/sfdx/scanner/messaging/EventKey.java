package sfdc.sfdx.scanner.messaging;
import static sfdc.sfdx.scanner.messaging.SfdxMessager.*;

public enum EventKey {
	// MAKE SURE messageKey OF EVERY VALUE ADDED HERE HAS AN ENTRY IN 'messages/EventKeyTemplates.json'!
	WARNING_INVALID_CAT_SKIPPED("warning.invalidCategorySkipped", 1, MessageType.WARNING, MessageHandler.UX, true),
	WARNING_INVALID_RULESET_SKIPPED("warning.invalidRulesetSkipped", 1, MessageType.WARNING, MessageHandler.UX, true),
	WARNING_XML_DROPPED("warning.xmlDropped", 1, MessageType.WARNING, MessageHandler.UX, true),
	INFO_JAR_AND_XML_PROCESSED("info.jarAndXmlProcessed", 2, MessageType.INFO, MessageHandler.UX, true),
	ERROR_INTERNAL_UNEXPECTED("error.internal.unexpectedError", 1, MessageType.ERROR, MessageHandler.INTERNAL, false),
	ERROR_INTERNAL_MAIN_INVALID_ARGUMENT("error.internal.mainInvalidArgument", 1, MessageType.ERROR, MessageHandler.INTERNAL, false),
	ERROR_INTERNAL_JSON_WRITE_FAILED("error.internal.jsonWriteFailed", 1, MessageType.ERROR, MessageHandler.INTERNAL, false),
	ERROR_INTERNAL_CLASSPATH_DOES_NOT_EXIST("error.internal.classpathDoesNotExist", 1, MessageType.ERROR, MessageHandler.INTERNAL, false),
	ERROR_INTERNAL_XML_MISSING_IN_CLASSPATH("error.internal.xmlMissingInClasspath", 1, MessageType.ERROR, MessageHandler.INTERNAL, false),
	ERROR_EXTERNAL_JAR_NOT_READABLE("error.external.jarNotReadable", 1, MessageType.ERROR, MessageHandler.UX, false),
	ERROR_EXTERNAL_DIR_NOT_READABLE("error.external.dirNotReadable", 1, MessageType.ERROR, MessageHandler.UX, false),
	ERROR_EXTERNAL_MULTIPLE_RULE_DESC("error.external.multipleRuleDesc", 2, MessageType.ERROR, MessageHandler.UX, false),
	ERROR_EXTERNAL_RECURSION_LIMIT("error.external.recursionLimitReached", 2, MessageType.ERROR, MessageHandler.UX, false),
	ERROR_EXTERNAL_XML_NOT_READABLE("error.external.xmlNotReadable", 2, MessageType.ERROR, MessageHandler.UX, false),
	ERROR_EXTERNAL_XML_NOT_PARSABLE("error.external.xmlNotParsable", 2, MessageType.ERROR, MessageHandler.UX, false);

	String messageKey;
	int argCount;
	MessageType messageType;
	MessageHandler messageHandler;
	boolean verbose;//true: only when verbose is true, false: ignores verbose flag and always prints

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
