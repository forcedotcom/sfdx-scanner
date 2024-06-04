import {Flags, SfCommand} from '@salesforce/sf-plugins-core';
import {RulesAction} from '../../lib/actions/RulesAction';
import {BundleName, getMessage} from '../../lib/messages';

export default class RulesCommand extends SfCommand<void> {
	// We don't need the `--json` output for this command.
	public static readonly enableJsonFlag = false;
	public static readonly summary = getMessage(BundleName.RulesCommand, 'command.summary');
	public static readonly description = getMessage(BundleName.RulesCommand, 'command.description');
	public static readonly examples = [
		getMessage(BundleName.RulesCommand, 'command.examples')
	];

	public static readonly flags = {
		'rule-selector': Flags.string({
			summary: getMessage(BundleName.RulesCommand, 'flags.rule-selector.summary'),
			char: 'r',
			multiple: true,
			delimiter: ',',
			default: ["Recommended"]
		}),
		'config-file': Flags.file({
			summary: getMessage(BundleName.RulesCommand, 'flags.config-file.summary'),
			char: 'c',
			exists: true
		}),
		view: Flags.string({
			summary: getMessage(BundleName.RulesCommand, 'flags.view.summary'),
			char: 'v',
			options: ['table', 'detail'] // TODO: Should probably be enum?
		})
	};

	public async run(): Promise<void> {
		const parsedFlags = (await this.parse(RulesCommand)).flags;
		const action: RulesAction = new RulesAction();
		action.execute(parsedFlags);
	}
}

