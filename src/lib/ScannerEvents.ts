import {EventEmitter} from 'events';

// TODO: Refactor away from events and instead inject the "Display" as a dependency into all of the classes that emit events

export const uxEvents = new EventEmitter();

export enum EVENTS {
	INFO_ALWAYS = 'info-always',
	INFO_VERBOSE = 'info-verbose',
	WARNING_ALWAYS = 'warning-always',
	WARNING_VERBOSE = 'warning-verbose',
	WARNING_ALWAYS_UNIQUE = 'warning-always-unique',
	ERROR_ALWAYS = 'error-always',
	// NOTE: If, for some reason, we eventually need verbose-only spinners, we'll need to split START_SPINNER into
	// always/verbose variants, but the other two can be left alone. This is because they correspond to `ux.setSpinnerStatus()`
	// and `ux.stopSpinner()`, which are both no-ops if there's not an active spinner.
	START_SPINNER = 'start-spinner',
	UPDATE_SPINNER = 'update-spinner',
	WAIT_ON_SPINNER = 'wait-on-spinner',
	STOP_SPINNER = 'stop-spinner'
}
