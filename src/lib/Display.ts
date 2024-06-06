import {Ux} from '@salesforce/sf-plugins-core';
import {AnyJson} from '@salesforce/ts-types';

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
	displayStyledObject(obj: AnyJson): void;

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

	public displayStyledObject(obj: AnyJson): void {
		this.displayable.styledObject(obj);
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
