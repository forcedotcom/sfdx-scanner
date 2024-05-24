import {Flags} from '@salesforce/sf-plugins-core';

export const PATH_START = 'path-start';

export const flags = {
	workspace: Flags.string({
		summary: 'summary boop', // TODO
		char: 'w'
	}),
	[PATH_START]: Flags.string({
		summary: 'summary boop', // TODO
		multiple: true
	})
}

export type ScopeInput = {
	workspace: string | undefined;
	[PATH_START]: string[] | undefined;
}
