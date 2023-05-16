import {SfdxCommand} from '@salesforce/command';
import {CategoryFilter, LanguageFilter, RuleFilter, RulesetFilter, RulenameFilter, EngineFilter} from './RuleFilter';
import {uxEvents, EVENTS} from './ScannerEvents';
import {stringArrayTypeGuard} from './util/Utils';
import {initContainer} from '../ioc.config';
import {AnyJson} from '@salesforce/ts-types';

import {Messages} from '@salesforce/core';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);
const commonMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'common');


export abstract class ScannerCommand extends SfdxCommand {

	public async run(): Promise<AnyJson> {
		this.runCommonSteps();
		return await this.runInternal();
	}

	/**
	 * Command's should implement this method to add their
	 * working steps.
	 */
	abstract runInternal(): Promise<AnyJson>;

	/**
	 * Common steps that should be run before every command
	 */
	protected runCommonSteps(): void {
		this.ux.warn(commonMessages.getMessage('surveyRequestMessage'));
		// Bootstrap the IOC container.
		initContainer();
	}

	protected buildRuleFilters(): RuleFilter[] {
		const filters: RuleFilter[] = [];
		// Create a filter for any provided categories.
		if (this.flags.category && stringArrayTypeGuard(this.flags.category) && this.flags.category.length) {
			filters.push(new CategoryFilter(this.flags.category));
		}

		// Create a filter for any provided rulesets.
		if (this.flags.ruleset && stringArrayTypeGuard(this.flags.ruleset) && this.flags.ruleset.length) {
			filters.push(new RulesetFilter(this.flags.ruleset));
		}

		// Create a filter for any provided languages.
		if (this.flags.language && stringArrayTypeGuard(this.flags.language) && this.flags.language.length) {
			filters.push(new LanguageFilter(this.flags.language));
		}

		// Create a filter for provided rule names.
		// NOTE: Only a single rule name can be provided. It will be treated as a singleton list.
		if (this.flags.rulename && typeof this.flags.rulename === 'string') {
			filters.push(new RulenameFilter([this.flags.rulename]));
		}

		// Create a filter for any provided engines.
		if (this.flags.engine && stringArrayTypeGuard(this.flags.engine) && this.flags.engine.length) {
			filters.push(new EngineFilter(this.flags.engine));
		}

		return filters;
	}

	protected displayInfo(msg: string, verboseOnly: boolean): void {
		if (!verboseOnly || this.flags.verbose) {
			this.ux.log(msg);
		}
	}

	protected displayWarning(msg: string, verboseOnly: boolean): void {
		if (!verboseOnly || this.flags.verbose) {
			this.ux.warn(msg);
		}
	}

	protected displayError(msg: string): void {
		this.ux.error(msg);
	}

	protected startSpinner(msg: string, status="Please Wait"): void {
		this.ux.startSpinner(msg, status);
	}

	protected updateSpinner(msg: string): void {
		this.ux.setSpinnerStatus(msg);
	}

	/* eslint-disable-next-line @typescript-eslint/no-unused-vars */
	protected waitOnSpinner(msg: string): void {
		// msg variable is thrown away - please don't send anything here.
		const currentStatus = this.ux.getSpinnerStatus();
		this.ux.setSpinnerStatus(currentStatus + ' .');
	}

	protected stopSpinner(msg: string): void {
		this.ux.stopSpinner(msg);
	}

	protected async init(): Promise<void> {
		await super.init();
		this.buildEventListeners();
	}

	protected buildEventListeners(): void {
		uxEvents.on(EVENTS.INFO_ALWAYS, (msg: string) => this.displayInfo(msg, false));
		uxEvents.on(EVENTS.INFO_VERBOSE, (msg: string) => this.displayInfo(msg, true));
		uxEvents.on(EVENTS.WARNING_ALWAYS, (msg: string) => this.displayWarning(msg, false));
		uxEvents.on(EVENTS.WARNING_VERBOSE, (msg: string) => this.displayWarning(msg, true));
		uxEvents.on(EVENTS.ERROR_ALWAYS, (msg: string) => this.displayError(msg));
		uxEvents.on(EVENTS.ERROR_VERBOSE, (msg: string) => this.displayError(msg));
		uxEvents.on(EVENTS.START_SPINNER, (msg: string, status: string) => this.startSpinner(msg, status));
		uxEvents.on(EVENTS.UPDATE_SPINNER, (msg: string) => this.updateSpinner(msg));
		uxEvents.on(EVENTS.WAIT_ON_SPINNER, (msg: string) => this.waitOnSpinner(msg));
		uxEvents.on(EVENTS.STOP_SPINNER, (msg: string) => this.stopSpinner(msg));
	}
}
