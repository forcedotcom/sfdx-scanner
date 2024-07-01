import {Display} from '../../src/lib/Display';
import {Ux} from '@salesforce/sf-plugins-core';
import {AnyJson} from "@salesforce/ts-types";

/**
 * Implementation of {@link Display} that tracks every call in an array and allows assertions against them.
 */
export class SpyDisplay implements Display {
	private displayEvents: DisplayEvent[] = [];

	/**
	 * Track that the provided message was displayed as an Error-level output.
	 */
	public displayError(message: string): void {
		this.displayEvents.push({
			type: DisplayEventType.ERROR,
			data: message
		});
	}

	/**
	 * Track that the provided message was displayed as a Warn-level output.
	 */
	public displayWarning(message: string): void {
		this.displayEvents.push({
			type: DisplayEventType.WARN,
			data: message
		});
	}

	/**
	 * Track that the provided message was displayed as an Info-level output.
	 */
	public displayInfo(message: string): void {
		this.displayEvents.push({
			type: DisplayEventType.INFO,
			data: message
		});
	}

	public displayLog(message: string): void {
		this.displayEvents.push({
			type: DisplayEventType.LOG,
			data: message
		});
	}

	/**
	 * Track that the provided text was displayed as a header.
	 */
	public displayStyledHeader(headerText: string): void {
		this.displayEvents.push({
			type: DisplayEventType.STYLED_HEADER,
			data: headerText
		});
	}

	/**
	 * Track that the provided object was displayed with the provided keys.
	 */
	public displayStyledObject(obj: AnyJson, keys?: string[]): void {
		this.displayEvents.push({
			type: DisplayEventType.STYLED_OBJECT,
			data: JSON.stringify({obj, keys})
		});
	}

	/**
	 * Track that the provided table data was displayed.
	 */
	public displayTable<R extends Ux.Table.Data>(data: R[], columns: Ux.Table.Columns<R>): void {
		const columnNames: string[] = Object.values(columns).map(column => column.header || '');
		this.displayEvents.push({
			type: DisplayEventType.TABLE,
			data: JSON.stringify({
				columns: columnNames,
				rows: data
			})
		});
	}

	/**
	 * Track that the spinner was started with the provided message, and optionally status.
	 */
	public spinnerStart(msg: string, status?: string): void {
		this.displayEvents.push({
			type: DisplayEventType.SPINNER_START,
			data: JSON.stringify({
				msg,
				status
			})
		});
	}

	/**
	 * Track that the spinner was updated to the provided status.
	 */
	public spinnerUpdate(status: string): void {
		this.displayEvents.push({
			type: DisplayEventType.SPINNER_UPDATE,
			data: status
		});
	}

	/**
	 * Track that the spinner was stopped with the provided status.
	 * @param status
	 */
	public spinnerStop(status: string): void {
		this.displayEvents.push({
			type: DisplayEventType.SPINNER_STOP,
			data: status
		});
	}

	public getDisplayEvents(): DisplayEvent[] {
		return this.displayEvents;
	}
}

export enum DisplayEventType {
	ERROR,
	WARN,
	INFO,
	LOG,
	STYLED_HEADER,
	STYLED_OBJECT,
	TABLE,
	SPINNER_START,
	SPINNER_UPDATE,
	SPINNER_STOP
}

export type DisplayEvent = {
	type: DisplayEventType;
	data: string;
}
