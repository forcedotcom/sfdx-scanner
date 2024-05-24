import {SfCommand} from '@salesforce/sf-plugins-core';
import {flags as configFlags, ConfigInput} from '../../lib/input/config';
import {flags as ruleSelectorFlags, RuleSelectorInput} from '../../lib/input/rule-selection';

type Input = ConfigInput & RuleSelectorInput;
export default class Rules extends SfCommand<void> {
	// We don't need the `--json` output for this command.
	public static readonly enableJsonFlag = false;

	public static summary = 'summary boop'; // TODO
	public static description = 'description boop'; // TODO
	public static examples = [ // TODO
		'example1 boop',
		'example2 boop'
	];

	public static readonly flags = {
		...ruleSelectorFlags,
		...configFlags
	};

	public async run(): Promise<void> {
		// Step 1: Parse the inputs into a usable object.
		const parsedFlags: Input = (await this.parse(Rules)).flags;

		// Step 2: Hand the inputs off to the more easily testable delegate class.
		new RulesDelegate().run(parsedFlags);
	}
}

export class RulesDelegate {

	public run(input: Input): void {
		// TODO: IMPLEMENT
		console.log(`Input is ${JSON.stringify(input)}`);
	}
}
