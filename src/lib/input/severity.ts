import {Flags} from '@salesforce/sf-plugins-core';

export const SEVERITY_THRESHOLD = 'severity-threshold';

export const flags = {
	[SEVERITY_THRESHOLD]: Flags.string({
		summary: 'summary boop', // TODO
		char: 's',
		options: ["1", "2", "3", "4", "5", "critical", "high", "moderate", "low", "info"] // TODO: This should probably be an Enum
	})
}

export type SeverityInput = {
	[SEVERITY_THRESHOLD]: string | undefined;
};

