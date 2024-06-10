import {Ux} from '@salesforce/sf-plugins-core';
import {AnyJson} from '@salesforce/ts-types';

/**
 * Interface for objects that display output information to users. E.g., a class that prints to the CLI would implement
 * this interface.
 * Contrast with a {@code Viewer} implementation, e.g. {@link RuleViewer}, which is responsible for arranging data and
 * handing it off to an underlying {@code Display} implementation.
 */
export interface Display {
	/**
	 * Output message to stdout (non-blocking) only if the "--json" flag is not present.
	 */
	displayInfo(msg: string): void;

	/**
	 * Output styled header to stdout only if the "--json" flag is not present.
	 */
	displayStyledHeader(headerText: string): void;

	/**
	 * Output object to stdout only if the "--json" flag is not present.
	 */
	displayStyledObject(obj: AnyJson, keys?: string[]): void;

	/**
	 * Output table to stdout only if the "--json" flag is not present.
	 */
	displayTable<R extends Ux.Table.Data>(data: R[], columns: Ux.Table.Columns<R>): void;
}

export class UxDisplay implements Display {
	private readonly displayable: Displayable;

	public constructor(displayable: Displayable) {
		this.displayable = displayable;
	}

	public displayInfo(message: string): void {
		this.displayable.log(message);
	}

	public displayStyledHeader(headerText: string): void {
		this.displayable.styledHeader(headerText);
	}

	public displayStyledObject(obj: AnyJson, keys?: string[]): void {
		if (obj == null || (keys != null && keys.length === 0)) {
			return;
		}
		if (keys) {
			// At time of writing (June 2024), the UX's underlying `styledObject()` method prints the keys
			// in alphabetical order, which is oftentimes an undesirable behavior for us. Hence, this workaround
			// where the keys are isolated into separate objects and logged one-at-a-time.
			for (const key of keys) {
				this.displayable.styledObject({
					// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment -- We know the key's value is JSON-compatible.
					[key]: obj[key]
				})
			}
		} else {
			this.displayable.styledObject(obj);
		}
	}

	public displayTable<R extends Ux.Table.Data>(data: R[], columns: Ux.Table.Columns<R>): void {
		this.displayable.table(data, columns);
	}
}

export interface Displayable {
	// Output message to stdout (non-blocking) only when "--json" flag is not present.      [Implemented by Command]
	log(message?: string): void;

	// Output stylized header to stdout only when "--json" flag is not present.             [Implemented by SfCommand]
	styledHeader(headerText: string): void;

	// Output stylized object to stdout only when "--json" flag is not present.             [Implemented by SfCommand]
	styledObject(obj: AnyJson): void;

	// Output table to stdout only when "--json" flag is not present.                       [Implemented by SfCommand]
	table<R extends Ux.Table.Data>(data: R[], columns: Ux.Table.Columns<R>, options?: Ux.Table.Options): void;
}
