package com.salesforce.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;

public class CliMessager {
	// The START string gives us something to scan for when we're processing output.
	private static final String START = "SFDX-START";
	// The END string lets us know when a message stops, which should prevent bugs involving multi-line output.
	private static final String END = "SFDX-END";

	private static final List<Message> MESSAGES = new ArrayList<>();

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
	public void addMessage(String internalLog, EventKey eventKey, String... args) {
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

	private static final class LazyHolder {
		// Postpone initialization until first use
		private static final CliMessager INSTANCE = new CliMessager();
	}
}
