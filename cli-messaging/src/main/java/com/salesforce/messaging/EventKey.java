package com.salesforce.messaging;
import static com.salesforce.messaging.Message.*;

public enum EventKey {
	// MAKE SURE `messageKey` OF EVERY VALUE ADDED HERE HAS AN ENTRY IN 'messages/EventKeyTemplates.js'!

    /** PMD-CATALOGER RELATED **/
	INFO_GENERAL_INTERNAL_LOG("info.generalInternalLog", 1, MessageType.INFO, MessageHandler.INTERNAL, true),
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
	ERROR_EXTERNAL_DUPLICATE_XML_PATH("error.external.duplicateXmlPath", 3, MessageType.ERROR, MessageHandler.UX, false),
	ERROR_EXTERNAL_MULTIPLE_RULE_DESC("error.external.multipleRuleDesc", 2, MessageType.ERROR, MessageHandler.UX, false),
	ERROR_EXTERNAL_RECURSION_LIMIT("error.external.recursionLimitReached", 2, MessageType.ERROR, MessageHandler.UX, false),
	ERROR_EXTERNAL_XML_NOT_READABLE("error.external.xmlNotReadable", 2, MessageType.ERROR, MessageHandler.UX, false),
	ERROR_EXTERNAL_XML_NOT_PARSABLE("error.external.xmlNotParsable", 2, MessageType.ERROR, MessageHandler.UX, false),




    /** SFGE RELATED **/
    INFO_GENERAL("info.sfgeInfoLog", 1, MessageType.INFO, MessageHandler.UX, true),
    INFO_META_INFO_COLLECTED("info.sfgeMetaInfoCollected", 2, MessageType.INFO, MessageHandler.UX, true),
    INFO_COMPLETED_FILE_COMPILATION("info.sfgeFinishedCompilingFiles", 1, MessageType.INFO, MessageHandler.UX, false),
    INFO_STARTED_BUILDING_GRAPH("info.sfgeStartedBuildingGraph", 0, MessageType.INFO, MessageHandler.UX_SPINNER, false),
    INFO_COMPLETED_BUILDING_GRAPH("info.sfgeFinishedBuildingGraph", 0, MessageType.INFO, MessageHandler.UX_SPINNER, false),
    INFO_PATH_ENTRY_POINTS_IDENTIFIED("info.sfgePathEntryPointsIdentified", 1, MessageType.INFO, MessageHandler.UX, true),
    INFO_PATH_ANALYSIS_PROGRESS("info.sfgeViolationsInPathProgress", 4, MessageType.INFO, MessageHandler.UX_SPINNER, false),
    INFO_COMPLETED_PATH_ANALYSIS("info.sfgeCompletedPathAnalysis", 3, MessageType.INFO, MessageHandler.UX_SPINNER, false),
    WARNING_GENERAL("warning.sfgeWarnLog", 1, MessageType.WARNING, MessageHandler.UX, true),
	WARNING_MULTIPLE_METHOD_TARGET_MATCHES("warning.multipleMethodTargetMatches", 3, MessageType.WARNING, MessageHandler.UX, false),
	WARNING_NO_METHOD_TARGET_MATCHES("warning.noMethodTargetMatches", 2, MessageType.WARNING, MessageHandler.UX, false),
    ERROR_GENERAL("error.internal.sfgeErrorLog", 1, MessageType.ERROR, MessageHandler.UX, false);

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
