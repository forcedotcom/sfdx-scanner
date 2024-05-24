import {SfCommand} from '@salesforce/sf-plugins-core';
import {flags as ruleSelectorFlags} from '../../lib/input/rule-selection';
import {flags as configFlags} from '../../lib/input/config';
import {flags as scopeFlags} from '../../lib/input/scope';
import {flags as severityFlags} from '../../lib/input/severity';

export default class Run extends SfCommand<void> {
	// We don't need the `--json` output for this command.
	public static readonly enableJsonFlag = false;

	public static summary = 'summary boop'; // TODO
	public static description = 'description boop'; // TODO
	public static examples = [ // TODO
		'example1 boop',
		'example2 boop'
	];

	public static readonly flags = {
		...severityFlags,
		...scopeFlags,
		...ruleSelectorFlags,
		...configFlags
	};

	public async run(): Promise<void> {
		// Step 1: Parse the flags into a usable object.
		const parsedFlags = (await this.parse(Run)).flags
		console.log(`Flags are ${JSON.stringify(parsedFlags)}`);

		// Step 2: Hand the inputs off to a more easily tested helper class.
		// TODO:
	}
}
