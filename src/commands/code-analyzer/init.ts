import {Flags, SfCommand} from '@salesforce/sf-plugins-core';
import {InitAction} from '../../lib/actions/InitAction';
import {BundleName, getMessage} from '../../lib/messages';

export default class InitCommand extends SfCommand<void> {
	// We don't need the `--json` output for this command.
	public static readonly enableJsonFlag = false;
	public static readonly summary = getMessage(BundleName.InitCommand, 'command.summary');
	public static readonly description = getMessage(BundleName.InitCommand, 'command.description');
	public static readonly examples = [
		getMessage(BundleName.InitCommand, 'command.examples')
	];

	public static readonly flags = {
		template: Flags.string({
			summary: getMessage(BundleName.InitCommand, 'flags.template.summary'),
			char: 't',
			options: ["empty", "default"] // TODO: These should probably be an enum?
		})
	};

	public async run(): Promise<void> {
		const parsedFlags = (await this.parse(InitCommand)).flags;
		const action: InitAction = new InitAction();
		action.execute(parsedFlags);
	}
}

