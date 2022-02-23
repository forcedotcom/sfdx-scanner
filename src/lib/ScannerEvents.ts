import {EventEmitter} from 'events';

export const uxEvents = new EventEmitter();

export enum EVENTS {
	INFO_ALWAYS = 'info-always',
	INFO_VERBOSE = 'info-verbose',
	WARNING_ALWAYS = 'warning-always',
	WARNING_VERBOSE = 'warning-verbose',
	ERROR_ALWAYS = 'error-always',
	ERROR_VERBOSE = 'error-verbose'
}
