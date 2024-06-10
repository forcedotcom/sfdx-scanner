import {Display} from '../../src/lib/Display';
import {Ux} from '@salesforce/sf-plugins-core';
import {AnyJson} from "@salesforce/ts-types";

/**
 * Implementation of {@link Display} that tracks every call in an array and allows assertions against them.
 */
export class StubDisplay implements Display {
	private displayEvents: DisplayEvent[] = [];

	/**
	 * Track that the provided message was displayed as an Info-level output.
	 */
	public displayInfo(message: string): void {
		this.displayEvents.push({
			type: DisplayEventType.INFO,
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
	public displayTable<R extends Ux.Table.Data>(data: R[], _columns: Ux.Table.Columns<R>): void {
		this.displayEvents.push({
			type: DisplayEventType.TABLE,
			data: JSON.stringify(data)
		});
	}

	/**
	 * Verify that the provided {@link DisplayEvent}s were actually displayed in the expected order.
	 * @param expectedDisplayEvents
	 */
	public expectDisplayEvents(expectedDisplayEvents: DisplayEvent[]): void {
		expect(this.displayEvents).toHaveLength(expectedDisplayEvents.length);
		for (let i = 0; i < this.displayEvents.length; i++) {
			expect(this.displayEvents[i]).toEqual(expectedDisplayEvents[i]);
		}
	}
}

export enum DisplayEventType {
	INFO,
	STYLED_HEADER,
	STYLED_OBJECT,
	TABLE
}

export type DisplayEvent = {
	type: DisplayEventType;
	data: string;
}
