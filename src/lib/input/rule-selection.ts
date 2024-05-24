import {Flags} from '@salesforce/sf-plugins-core';
export const RULE_SELECTOR = 'rule-selector';

export const flags  = {
	[RULE_SELECTOR]: Flags.string({
		summary: 'summary boop', // TODO
		char: 'r',
		delimiter: ',',
		multiple: true
	})
};

export type RuleSelectorInput = {
	[RULE_SELECTOR]: string[] | undefined;
};
