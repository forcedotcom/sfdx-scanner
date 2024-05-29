import {SfCommand} from '@salesforce/sf-plugins-core';

export default class Run extends SfCommand<void> {
	// We don't need the `--json` output for this command.
	public static readonly enableJsonFlag = false;
	public static readonly summary = 'summary boop'; // TODO
	public static readonly description = 'description boop'; // TODO
	public static readonly examples = [ // TODO
		'example1 boop',
		'example2 boop'
	];

	public static readonly flags = {};

	public run(): Promise<void> {
		return Promise.resolve();
	}
}

