import {Flags} from '@salesforce/sf-plugins-core';

export const CONFIG_FILE = 'config-file';

export const flags = {
	[CONFIG_FILE]: Flags.file({
		summary: 'summary boop', // TODO
		char: 'c',
		exists: true
	})
}

export type ConfigInput = {
	[CONFIG_FILE]: string | undefined;
};

