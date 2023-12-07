package com.salesforce.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

public class CliMessager {
	// The START string gives us something to scan for when we're processing output.
	private static final String START = "SF-START";
	// The END string lets us know when a message stops, which should prevent bugs involving multi-line output.
	private static final String END = "SF-END";

    private static final String REALTIME_START = "SFCA-REALTIME-START";
    private static final String REALTIME_END = "SFCA-REALTIME-END";

    /* Deprecated: Don't maintain state in a class that's essentially used as a utility.*/
    @Deprecated
	private static final List<Message> MESSAGES = new ArrayList<>();

    /**
     * Deprecated - switch to static invocation of {@link #postMessage(String, EventKey, String...)}
     */
    @Deprecated
	public static CliMessager getInstance() {
		return LazyHolder.INSTANCE;
	}

	/**
	 * Add exception to pass onto Typescript layer.
	 * Will be treated as an Error based on the properties set
	 * in EventKey. Please make sure that EventKey is correct and is
	 * in sync with messages/EventKeyTemplates.json
	 *
	 * @param exception to send to Typescript layer
	 */
    @Deprecated
	public void addMessage(MessagePassableException exception) {
		final EventKey eventKey = exception.getEventKey();
		addMessage(
			exception.getFullStacktrace(),
			eventKey,
			exception.getArgs());
	}

	/**
	 * Add message to pass onto Typescript layer.
	 * Make sure EventKey is updated with messages/EventKeyTemplates.json
	 * and has correct properties in the enum.
	 *
	 * @param internalLog Information for internal use. Will be logged but not displayed to user
	 * @param eventKey    EventKey to display to user
	 * @param args        String args passed to the EventKey to make the displayed message meaningful
	 */
    @Deprecated
	public void addMessage(String internalLog, EventKey eventKey, String... args) {
        final Message message = createMessage(internalLog, eventKey, args);
        MESSAGES.add(message);
	}

    /**
     * Publish formatted stdout message to pass onto Typescript layer.
     * Make sure EventKey is updated with messages/EventKeyTemplates.json
     * and has correct properties in the enum.
     *
     * @param internalLog Information for internal use. Will be logged but not displayed to user
     * @param eventKey    EventKey to display to user
     * @param args        String args passed to the EventKey to make the displayed message meaningful
     */
    public static void postMessage(String internalLog, EventKey eventKey, String... args) {
        final Message message = createMessage(internalLog, eventKey, args);
        final List<Message> messages = Lists.newArrayList(message);

        final String messageAsJson = new Gson().toJson(messages);
        System.out.println(REALTIME_START + messageAsJson + REALTIME_END);
    }

    private static Message createMessage(String internalLog, EventKey eventKey, String[] args) {
        // Developer error if eventKey was not added to exception and we'll get a bunch of NPEs
        assert (eventKey != null);
        // Confirm that the correct number of arguments for the message has been provided
        // If this fails, this would be a developer error
        assert (eventKey.getArgCount() == args.length);

        final Message message = new Message(
            eventKey.getMessageKey(),
            Arrays.asList(args),
            internalLog,
            eventKey.getMessageType(),
            eventKey.getMessageHandler(),
            eventKey.isVerbose());
        return message;
    }

    /**
	 * Convert all messages stored by the instance into a JSON-formatted string, enclosed in the start and end strings.
	 * Java code can use this method to log the messages to console, and TypeScript code can seek the start and stop
	 * strings to get an array of messages that can be deserialized.
	 * @return
	 */
    @Deprecated
	public String getAllMessagesWithFormatting() {
		final String messagesAsJson = getMessagesAsJson();
		return START + messagesAsJson + END;
	}

    @Deprecated
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

	private static final class LazyHolder {
		// Postpone initialization until first use
		private static final CliMessager INSTANCE = new CliMessager();
	}
}
