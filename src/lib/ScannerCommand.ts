import {SfCommand} from '@salesforce/sf-plugins-core';
import {CategoryFilter, LanguageFilter, RuleFilter, RulesetFilter, RulenameFilter, EngineFilter} from './RuleFilter';
import {uxEvents, EVENTS} from './ScannerEvents';
import {initContainer} from '../ioc.config';
import {AnyJson} from '@salesforce/ts-types';
import {stringArrayTypeGuard} from './util/Utils';
import {LooseObject} from '../types';

import {Logger, Messages} from '@salesforce/core';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);
const commonMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'common');


export abstract class ScannerCommand extends SfCommand<AnyJson> {

	/**
	 * {@code parsedFlags} is declared as a {@link LooseObject}, which is equivalent to {@code @oclif/core}'s
	 * internal type {@code FlagOutput}, meaning we can use it for {@code this.parse()}.
	 * @protected
	 */
	protected parsedFlags: LooseObject;
	protected logger: Logger;

	public async run(): Promise<AnyJson> {
		this.runCommonSteps();
		this.logger = await Logger.child(this.ctor.name);
		this.parsedFlags = (await this.parse(this.ctor)).flags;
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
		this.warn(commonMessages.getMessage('surveyRequestMessage'));
		// Bootstrap the IOC container.
		initContainer();
	}

	protected buildRuleFilters(): RuleFilter[] {
		const filters: RuleFilter[] = [];
		// Create a filter for any provided categories.
		if (this.parsedFlags.category && stringArrayTypeGuard(this.parsedFlags.category) && this.parsedFlags.category.length) {
			filters.push(new CategoryFilter(this.parsedFlags.category));
		}

		// Create a filter for any provided rulesets.
		if (this.parsedFlags.ruleset && stringArrayTypeGuard(this.parsedFlags.ruleset) && this.parsedFlags.ruleset.length) {
			filters.push(new RulesetFilter(this.parsedFlags.ruleset));
		}

		// Create a filter for any provided languages.
		if (this.parsedFlags.language && stringArrayTypeGuard(this.parsedFlags.language) && this.parsedFlags.language.length) {
			filters.push(new LanguageFilter(this.parsedFlags.language));
		}

		// Create a filter for provided rule names.
		// NOTE: Only a single rule name can be provided. It will be treated as a singleton list.
		if (this.parsedFlags.rulename && typeof this.parsedFlags.rulename === 'string') {
			filters.push(new RulenameFilter([this.parsedFlags.rulename]));
		}

		// Create a filter for any provided engines.
		if (this.parsedFlags.engine && stringArrayTypeGuard(this.parsedFlags.engine) && this.parsedFlags.engine.length) {
			filters.push(new EngineFilter(this.parsedFlags.engine));
		}

		return filters;
	}

	protected displayInfo(msg: string, verboseOnly: boolean): void {
		if (!verboseOnly || this.parsedFlags.verbose) {
			this.log(msg);
		}
	}

	protected displayWarning(msg: string, verboseOnly: boolean): void {
		if (!verboseOnly || this.parsedFlags.verbose) {
			this.warn(msg);
		}
	}

	protected displayError(msg: string): void {
		this.error(msg);
	}

	protected startSpinner(msg: string, status="Please Wait"): void {
		this.spinner.start(msg, status);
	}

	protected updateSpinner(msg: string): void {
		this.spinner.status = msg;
	}

	/* eslint-disable-next-line @typescript-eslint/no-unused-vars */
	protected waitOnSpinner(msg: string): void {
		// msg variable is thrown away - please don't send anything here.
		this.spinner.status += ' .';
	}

	protected stopSpinner(msg: string): void {
		this.spinner.stop(msg);
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
