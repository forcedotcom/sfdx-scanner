import {SfCommand} from '@salesforce/sf-plugins-core';
import {uxEvents, EVENTS} from './ScannerEvents';
import {initContainer} from '../ioc.config';
import {AnyJson} from '@salesforce/ts-types';
import {Inputs} from '../types';
import {Display, Displayable, UxDisplay} from "./Display";
import {BundleName, getMessage} from "../MessageCatalog";
import {Logger} from "@salesforce/core";


/**
 * Abstract class to help build scanner commands.
 *
 * The responsibilities of this parent class is to:
 *   * Parse the cli input flags
 *   * To construct the Logger and Display instances
 *   * To ask the subclass for the Action instance and invoke the validateInputs and run methods on it.
 *
 * The responsibilities of each subclass is to:
 *   * Define the summary, description, examples, and flags for its command
 *   * Construct the Action associated with its command with all of its runtime dependencies
 *
 * Implementations of ScannerCommand should ideally have zero business logic in them since they should simply serve as
 * runtime dependency injection points to their corresponding Action classes.
 */
export abstract class ScannerCommand extends SfCommand<AnyJson> implements Displayable {

	// It appears we need to explicitly set this in order for the global --json flag to show up in the generated help text
	public static readonly enableJsonFlag = true;

	protected async init(): Promise<void> {
		await super.init();
		initContainer();
	}

	protected abstract createAction(logger: Logger, display: Display) : Action;

	public async run(): Promise<AnyJson> {
		const inputs: Inputs = (await this.parse(this.ctor)).flags;
		const logger: Logger = await Logger.child(this.ctor.name);
		const display: Display = new UxDisplay(this, this.spinner, inputs.verbose as boolean);

		display.displayWarning(getMessage(BundleName.Common, 'surveyRequestMessage'));
		this.buildEventListeners(display);

		const action: Action = this.createAction(logger, display);
		await action.validateInputs(inputs);
		return await action.run(inputs);
	}

	// TODO: Refactor away from events and instead inject the "Display" as a dependency into all of the classes that emit events
	private buildEventListeners(display: Display): void {
		uxEvents.on(EVENTS.INFO_ALWAYS, (msg: string) => display.displayInfo(msg));
		uxEvents.on(EVENTS.INFO_VERBOSE, (msg: string) => display.displayVerboseInfo(msg));
		uxEvents.on(EVENTS.WARNING_ALWAYS, (msg: string) => display.displayWarning(msg));
		uxEvents.on(EVENTS.WARNING_VERBOSE, (msg: string) => display.displayVerboseWarning(msg));
		uxEvents.on(EVENTS.WARNING_ALWAYS_UNIQUE, (msg: string) => display.displayUniqueWarning(msg));
		uxEvents.on(EVENTS.ERROR_ALWAYS, (msg: string) => display.displayError(msg));
		uxEvents.on(EVENTS.START_SPINNER, (msg: string, status: string) => display.spinnerStart(msg, status));
		uxEvents.on(EVENTS.UPDATE_SPINNER, (msg: string) => display.spinnerUpdate(msg));
		uxEvents.on(EVENTS.WAIT_ON_SPINNER, (_msg: string) => display.spinnerWait()); // eslint-disable-line @typescript-eslint/no-unused-vars
		uxEvents.on(EVENTS.STOP_SPINNER, (msg: string) => display.spinnerStop(msg));
	}
}

export interface Action {
	validateInputs(inputs: Inputs): Promise<void>;

	run(inputs: Inputs): Promise<AnyJson>;
}
