import {PrettyPrintableError} from "@oclif/core/lib/errors";
import {Spinner} from "@salesforce/sf-plugins-core";

export interface Display {
	/**
	 * Output message to stdout (non-blocking) only if the "--json" flag is not present
	 */
	displayInfo(msg: string, ...args: any[]): void;

	/**
	 * Output message to stdout (non-blocking) only if the "--verbose" flag is present and the "--json" flag is not
	 */
	displayVerboseInfo(msg: string): void;

	/**
	 * Display an error or message as a warning
	 */
	displayWarning(msg: Error | string): void;

	/**
	 * Display an error or message as a warning only if the "--verbose" flag is present
	 */
	displayVerboseWarning(msg: Error | string): void;

	/**
	 * 	Display error and exit. Optionally add a code to error object or exit status.
	 */
	displayErrorAndExit(msg: Error | string, options?: {code?: string; exit?: number; } & PrettyPrintableError): never

	/**
	 * Adds a spinner to the display
	 */
	spinnerStart(msg: string, status?: string): void

	/**
	 * Updates the status of the spinner in the display
	 */
	spinnerUpdate(status: string): void

	/**
	 * Appends a heartbeat signal (like a " .") to the existing status of the spinner in the display
	 */
	spinnerWait(): void

	/**
	 * Stops the spinner in the display
	 */
	spinnerStop(msg: string)
}

export interface Displayable {
	// Display error and exit. Optionally add a code to error object or exit status
	error(input: Error | string,
		  options?: {code?: string; exit?: number; } & PrettyPrintableError): never;

	// Output message to stdout (non-blocking) only when "--json" flag is not present
	log(message?: string, ...args: any[]): void;

	// Display an error or message as a warning
	warn(input: Error | string): Error | string;
}

export class UxDisplay implements Display {
	private readonly displayable: Displayable;
	private readonly spinner: Spinner;
	private readonly isVerboseSet: boolean;

	public constructor(displayable: Displayable, spinner: Spinner, isVerboseSet: boolean) {
		this.displayable = displayable;
		this.spinner = spinner;
		this.isVerboseSet = isVerboseSet;
	}

	public displayInfo(msg: string, ...args: any[]): void {
		this.displayable.log(msg, ...args);
	}

	public displayVerboseInfo(msg: string): void {
		if (this.isVerboseSet) {
			this.displayInfo(msg);
		}
	}

	public displayWarning(msg: Error | string): void {
		this.displayable.warn(msg);
	}

	public displayVerboseWarning(msg: Error | string): void {
		if (this.isVerboseSet) {
			this.displayWarning(msg);
		}
	}

	public displayErrorAndExit(msg: Error | string,
								  options?: {code?: string; exit?: number; } & PrettyPrintableError): never {
		this.displayable.error(msg, options);
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

	public spinnerStop(msg: string) {
		this.spinner.stop(msg);
	}
}
