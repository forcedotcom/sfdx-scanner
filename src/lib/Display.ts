import {Spinner} from "@salesforce/sf-plugins-core";
import {Ux} from "@salesforce/sf-plugins-core/lib/ux";
import {AnyJson} from "@salesforce/ts-types";

export interface Display {
	/**
	 * Output message to stdout (non-blocking) only if the "--json" flag is not present.
	 */
	displayInfo(msg: string): void;

	/**
	 * Output message to stdout (non-blocking) only if the "--verbose" flag is present and the "--json" flag is not.
	 */
	displayVerboseInfo(msg: string): void;

	/**
	 * Display confirmation prompt. Times out and throws after 10s.
	 */
	displayConfirmationPrompt(msg: string): Promise<boolean>;

	/**
	 * Output styled header to stdout only if the "--json" flag is not present.
	 */
	displayStyledHeader(headerText: string): void;

	/**
	 * Output table to stdout only if the "--json" flag is not present.
	 */
	displayTable<R extends Ux.Table.Data>(data: R[], columns: Ux.Table.Columns<R>): void;

	/**
	 * Output object to stdout only if the "--json" flag is not present.
	 */
	displayStyledObject(obj: AnyJson): void;

	/**
	 * Display a message as an error.
	 */
	displayError(msg: string): void;

	/**
	 * Display a message as a warning.
	 */
	displayWarning(msg: string): void;

	/**
	 * Display a message as a warning only if the "--verbose" flag is present.
	 */
	displayVerboseWarning(msg: string): void;

	/**
	 * Display a message as a warning, unless it's been displayed using this method before.
	 */
	displayUniqueWarning(msg: string): void;

	/**
	 * Adds a spinner to the display.
	 */
	spinnerStart(msg: string, status?: string): void

	/**
	 * Updates the status of the spinner in the display.
	 */
	spinnerUpdate(status: string): void

	/**
	 * Appends a heartbeat signal (like a " .") to the existing status of the spinner in the display.
	 */
	spinnerWait(): void

	/**
	 * Stops the spinner in the display.
	 */
	spinnerStop(msg: string): void
}

export class UxDisplay implements Display {
	private readonly displayable: Displayable;
	private readonly spinner: Spinner;
	private readonly isVerboseSet: boolean;
	private uniqueMessageSet: Set<string>;

	public constructor(displayable: Displayable, spinner: Spinner, isVerboseSet: boolean) {
		this.displayable = displayable;
		this.spinner = spinner;
		this.isVerboseSet = isVerboseSet;
		this.uniqueMessageSet = new Set();
	}

	public displayInfo(msg: string): void {
		this.displayable.log(msg);
	}

	public displayVerboseInfo(msg: string): void {
		if (this.isVerboseSet) {
			this.displayInfo(msg);
		}
	}

	public async displayConfirmationPrompt(msg: string): Promise<boolean> {
		return await this.displayable.confirm(msg);
	}

	public displayStyledHeader(headerText: string): void {
		this.displayable.styledHeader(headerText);
	}

	public displayTable<R extends Ux.Table.Data>(data: R[], columns: Ux.Table.Columns<R>): void {
		this.displayable.table(data, columns);
	}

	public displayStyledObject(obj: AnyJson): void {
		this.displayable.styledObject(obj);
	}

	public displayError(msg: string): void {
		this.displayable.error(msg);
	}

	public displayWarning(msg: string): void {
		this.displayable.warn(msg);
	}

	public displayVerboseWarning(msg: string): void {
		if (this.isVerboseSet) {
			this.displayWarning(msg);
		}
	}

	public displayUniqueWarning(msg: string): void {
		if (!this.uniqueMessageSet.has(msg)) {
			this.uniqueMessageSet.add(msg);
			this.displayWarning(msg);
		}
	}

	public spinnerStart(msg: string, status?: string): void {
		this.spinner.start(msg, status);
	}

	public spinnerUpdate(status: string): void {
		this.spinner.status = status;
	}

	public spinnerWait(): void {
		this.spinner.status += ' .';
	}

	public spinnerStop(msg: string): void {
		this.spinner.stop(msg);
	}
}


export interface Displayable {
	// Output message to stdout (non-blocking) only when "--json" flag is not present.      [Implemented by Command]
	log(message?: string): void;

	// Display an error or message as a warning.                                            [Implemented by Command]
	warn(input: string): void;

	// Display an error or message as an error.                                             [Implemented by Command]
	error(message: string): void;

	// Simplified prompt for single-question confirmation. Times out and throws after 10s.  [Implemented by SfCommand]
	confirm(message: string): Promise<boolean>;

	// Output stylized header to stdout only when "--json" flag is not present.             [Implemented by SfCommand]
	styledHeader(headerText: string): void;

	// Output stylized object to stdout only when "--json" flag is not present.             [Implemented by SfCommand]
	styledObject(obj: AnyJson): void;

	// Output table to stdout only when "--json" flag is not present.                       [Implemented by SfCommand]
	table<R extends Ux.Table.Data>(data: R[], columns: Ux.Table.Columns<R>, options?: Ux.Table.Options): void;
}
