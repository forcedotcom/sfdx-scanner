import {Display} from '../../src/lib/Display';
import {Ux} from '@salesforce/sf-plugins-core';
import {AnyJson} from "@salesforce/ts-types";

export class StubDisplay implements Display {
	private displayEvents: DisplayEvent[] = [];

	public displayInfo(message: string): void {
		this.displayEvents.push({
			type: DisplayEventType.INFO,
			data: message
		});
	}

	/**
	 * Output styled header to stdout only if the "--json" flag is not present.
	 */
	public displayStyledHeader(headerText: string): void {
		this.displayEvents.push({
			type: DisplayEventType.STYLED_HEADER,
			data: headerText
		});
	}

	/**
	 * Output object to stdout only if the "--json" flag is not present.
	 */
	public displayStyledObject(obj: AnyJson): void {
		this.displayEvents.push({
			type: DisplayEventType.STYLED_OBJECT,
			data: JSON.stringify(obj)
		});
	}

	/**
	 * Output table to stdout only if the "--json" flag is not present.
	 */
	public displayTable<R extends Ux.Table.Data>(data: R[], _columns: Ux.Table.Columns<R>): void {
		this.displayEvents.push({
			type: DisplayEventType.TABLE,
			data: JSON.stringify(data)
		});
	}

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
