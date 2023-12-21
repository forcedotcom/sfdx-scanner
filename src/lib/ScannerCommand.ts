import {SfCommand} from '@salesforce/sf-plugins-core';
import {uxEvents, EVENTS} from './ScannerEvents';
import {initContainer} from '../ioc.config';
import {AnyJson} from '@salesforce/ts-types';
import {Inputs} from '../types';
import {Logger} from '@salesforce/core';
import {Display, Displayable, UxDisplay} from "./Display";
import {Bundle, getMessage} from "../MessageCatalog";


export abstract class ScannerCommand extends SfCommand<AnyJson> implements Displayable {
	protected logger: Logger;
	protected display: Display;

	public async run(): Promise<AnyJson> {
		this.logger = await Logger.child(this.ctor.name);

		const inputs: Inputs = (await this.parse(this.ctor)).flags;
		this.display = new UxDisplay(this, this.spinner, inputs.verbose);

		this.runCommonSteps();
		return await this.runInternal(inputs);
	}

	/**
	 * Command's should implement this method to add their
	 * working steps.
	 */
	abstract runInternal(inputs: Inputs): Promise<AnyJson>;

	/**
	 * Common steps that should be run before every command
	 */
	protected runCommonSteps(): void {
		this.display.displayWarning(getMessage(Bundle.Common, 'surveyRequestMessage'));
		this.buildEventListeners();
	}

	protected async init(): Promise<void> {
		await super.init();
		initContainer();
	}

	// TODO: We should consider refactoring away from events and instead inject the "Display" as a dependency
	// into all of the classes that emit events
	protected buildEventListeners(): void {
		uxEvents.on(EVENTS.INFO_ALWAYS, (msg: string) => this.display.displayInfo(msg));
		uxEvents.on(EVENTS.INFO_VERBOSE, (msg: string) => this.display.displayVerboseInfo(msg));
		uxEvents.on(EVENTS.WARNING_ALWAYS, (msg: string) => this.display.displayWarning(msg));
		uxEvents.on(EVENTS.WARNING_VERBOSE, (msg: string) => this.display.displayVerboseWarning(msg));
		uxEvents.on(EVENTS.START_SPINNER, (msg: string, status: string) => this.display.spinnerStart(msg, status));
		uxEvents.on(EVENTS.UPDATE_SPINNER, (msg: string) => this.display.spinnerUpdate(msg));
		uxEvents.on(EVENTS.WAIT_ON_SPINNER, (msg: string) => this.display.spinnerWait());
		uxEvents.on(EVENTS.STOP_SPINNER, (msg: string) => this.display.spinnerStop(msg));
	}
}
