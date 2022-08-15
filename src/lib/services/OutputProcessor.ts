import {Logger, LoggerLevel, Messages} from '@salesforce/core';
import {AsyncCreatable} from '@salesforce/kit';
import {RuleEvent} from '../../types';
import {EVENTS, uxEvents} from '../ScannerEvents';


Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'EventKeyTemplates');
const genericMessageKey = 'error.external.genericErrorMessage';

const MESSAGE_START_TAG = 'SFDX-START';
const MESSAGE_END_TAG = 'SFDX-END';
const REALTIME_MESSAGE_START_TAG = 'SFCA-REALTIME-START';
const REALTIME_MESSAGE_END_TAG = 'SFCA-REALTIME-END';

/**
 * Helps with processing output from PmdCatalog java module and converting messages into usable events
 */
export class OutputProcessor extends AsyncCreatable {

	private logger!: Logger;
	private messageLogger!: Logger;
	private initialized: boolean;

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}

		this.logger = await Logger.child('OutputProcessor');
		this.messageLogger = await Logger.child('MessageLog');
		// this.logger.setLevel(LoggerLevel.TRACE);
		this.messageLogger.setLevel(LoggerLevel.TRACE);

		this.initialized = true;
	}

	public isRealtimeOutput(out: string): boolean {
		return out.startsWith(REALTIME_MESSAGE_START_TAG);
	}

	public processOutput(out: string): boolean {
		return this.processAllOutput(out, MESSAGE_START_TAG, MESSAGE_END_TAG);
	}

	public processRealtimeOutput(out: string): boolean {
		return this.processAllOutput(out, REALTIME_MESSAGE_START_TAG, REALTIME_MESSAGE_END_TAG);
	}

	// We want to find any events that were dumped into stdout or stderr and turn them back into events that can be thrown.
	// As per the convention outlined in SfdxMessager.java, SFDX-relevant messages will be stored in the outputs as JSONs
	// sandwiched between a given start tag and end tag. So we'll find all instances of that.
	private processAllOutput(out: string, startTag: string, endTag: string): boolean {
		this.logger.trace(`stdout: ${out}`);
		if (!out) {
			// Nothing to do here
			return false;
		}

		const outEvents: RuleEvent[] = this.getEventsFromString(out, startTag, endTag);
		this.logger.trace(`Total count of events found: ${outEvents.length}`);

		return this.emitEvents(outEvents);
	}

	// TODO: consider moving all message creation logic to a separate place and making this method private
	public emitEvents(outEvents: RuleEvent[]): boolean {
		this.logger.trace('About to order and emit');
		// If list is empty, we can just be done now.
		if (outEvents.length == 0) {
			return false;
		}

		// Iterate over all of the events and throw them as appropriate.
		outEvents.forEach((event) => {
			this.logEvent(event);
			let eventType = '';
			if (event.handler === 'UX_SPINNER') {
				eventType = EVENTS.UPDATE_SPINNER.toString();
			} else if (event.handler === 'UX' || (event.handler === 'INTERNAL' && event.type === 'ERROR')) {
				eventType = `${event.type.toLowerCase()}-${event.verbose ? 'verbose' : 'always'}`;
			}
			this.emitUxEvent(eventType, event.messageKey, event.args);
		});
		return true;
	}


	private emitUxEvent(eventType: string, messageKey: string, args: string[]): void {
		if (eventType === '') {
			this.logger.trace(`No event type requested for message ${messageKey}`);
			return;
		}


		this.logger.trace(`Sending new event of type ${eventType} and message ${messageKey}`);
		let constructedMessage: string = null;
		try {
			// Do this in a try-block so we can fail safely.
			constructedMessage = messages.getMessage(messageKey, args);
		} catch (e) {
			// If we were somehow unable to generate a message, fall back on the generic one, since we know that's valid.
			this.logger.trace(`Could not generate message for event key ${messageKey}. Defaulting to generic error message.`);
			constructedMessage = messages.getMessage(genericMessageKey, []);
		}
		uxEvents.emit(eventType, constructedMessage);
	}

	private getEventsFromString(str: string, startTag: string, endTag: string): RuleEvent[] {
		const events: RuleEvent[] = [];

		const regex = new RegExp(`^${startTag}(.*)${endTag}`, 'g');
		const headerLength = startTag.length;
		const tailLength = endTag.length;
		const regexMatch = str.match(regex);
		if (!regexMatch || regexMatch.length < 1) {
			this.logger.trace(`No events to log`);
		} else {
			regexMatch.forEach(item => {
				const jsonStr = item.substring(headerLength, item.length - tailLength);
				events.push(...JSON.parse(jsonStr) as RuleEvent[]);
			});
		}
		return events;
	}

	private logEvent(event: RuleEvent): void {
		const message = `Event: messageKey = ${event.messageKey}, args = ${JSON.stringify(event.args)}, type = ${event.type}, handler = ${event.handler}, verbose = ${event.verbose.toString()}, time = ${event.time}, internalLog = ${event.internalLog}`;
		this.messageLogger.info(message);
	}

}
