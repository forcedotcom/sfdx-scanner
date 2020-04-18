import {Logger, LoggerLevel, Messages} from '@salesforce/core';
import {AsyncCreatable} from '@salesforce/kit';
import {RuleEvent} from '../../types';
import {uxEvents} from '../ScannerEvents';


Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'EventKeyTemplates');
const genericMessageKey = 'error.external.genericErrorMessage';

/**
 * Helps with processing output from PmdCatalog java module and converting messages into usable events
 */
export class OutputProcessor extends AsyncCreatable {

	private logger!: Logger;
	private messageLogger!: Logger;
	private initialized: boolean;

	protected async init(): Promise<void> {
		if (this.initialized) return;

		this.logger = await Logger.child('OutputProcessor');
		this.messageLogger = await Logger.child('MessageLog');
		// this.logger.setLevel(LoggerLevel.TRACE);
		this.messageLogger.setLevel(LoggerLevel.TRACE);

		this.initialized = true;
	}

	// We want to find any events that were dumped into stdout or stderr and turn them back into events that can be thrown.
	// As per the convention outlined in SfdxMessager.java, SFDX-relevant messages will be stored in the outputs as JSONs
	// sandwiched between 'SFDX-START' and 'SFDX-END'. So we'll find all instances of that.
	public processOutput(out: string): void {
		this.logger.trace(`stdout: ${out}`);
		if (!out) {
			// Nothing to do here
			return;
		}

		const outEvents: RuleEvent[] = this.getEventsFromString(out);
		this.logger.trace(`Total count of events found: ${outEvents.length}`);

		this.emitEvents(outEvents);
	}

	// TODO: consider moving all message creation logic to a separate place and making this method private
	public emitEvents(outEvents: RuleEvent[]): void {
		this.logger.trace('About to order and emit');
		// If list is empty, we can just be done now.
		if (outEvents.length == 0) {
			return;
		}

		// Iterate over all of the events and throw them as appropriate.
		outEvents.forEach((event) => {
			this.logEvent(event);
			if (event.handler === 'UX') {
				const eventType = `${event.type.toLowerCase()}-${event.verbose ? 'verbose' : 'always'}`;
				this.emitUxEvent(eventType, event.messageKey, event.args);
			} else if (event.handler === 'INTERNAL' && event.type === 'ERROR') {
				this.logger.trace(`Logging error ${event.messageKey} and sending generic error message`);
				this.emitUxEvent(`error-always`, genericMessageKey, []);
			}
		});
	}


	private emitUxEvent(eventType: string, messageKey: string, args: string[]): void {
		this.logger.trace(`Sending new event of type ${eventType} and message ${messageKey}`);
		uxEvents.emit(eventType, messages.getMessage(messageKey, args));
	}

	private getEventsFromString(str: string): RuleEvent[] {
		const events: RuleEvent[] = [];

		const regex = /SFDX-START(.*)SFDX-END/g;
		const headerLength = 'SFDX-START'.length;
		const tailLength = 'SFDX-END'.length;
		const regexMatch = str.match(regex);
		if (!regexMatch || regexMatch.length < 1) {
			this.logger.trace(`No events to log`);
		} else {
			regexMatch.forEach(item => {
				const jsonStr = item.substring(headerLength, item.length - tailLength);
				events.push(...JSON.parse(jsonStr));
			});
		}
		return events;
	}

	private logEvent(event: RuleEvent): void {
		const message = `Event: messageKey = ${event.messageKey}, args = ${event.args}, type = ${event.type}, handler = ${event.handler}, verbose = ${event.verbose}, time = ${event.time}, internalLog = ${event.internalLog}`;
		this.messageLogger.info(message);
	}

}
