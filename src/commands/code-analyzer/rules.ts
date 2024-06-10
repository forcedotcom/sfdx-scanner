import {Flags, SfCommand} from '@salesforce/sf-plugins-core';
import {View} from '../../Constants';
import {ConfigLoaderImpl} from '../../lib/loaders/ConfigLoader';
import {EngineLoaderImpl} from '../../lib/loaders/EngineLoader';
import {RuleDetailViewer, RuleTableViewer} from '../../lib/viewers/RuleViewer';
import {RulesAction, RulesDependencies} from '../../lib/actions/RulesAction';
import {BundleName, getMessage} from '../../lib/messages';
import {Displayable, UxDisplay} from '../../lib/Display';

export default class RulesCommand extends SfCommand<void> implements Displayable {
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
			default: View.TABLE,
			options: Object.values(View)
		})
	};

	public async run(): Promise<void> {
		const parsedFlags = (await this.parse(RulesCommand)).flags;
		const uxDisplay: UxDisplay = new UxDisplay(this);
		const dependencies: RulesDependencies = {
			configLoader: new ConfigLoaderImpl(),
			engineLoader: new EngineLoaderImpl(),
			viewer: (parsedFlags.view as View) === View.TABLE ? new RuleTableViewer(uxDisplay) : new RuleDetailViewer(uxDisplay)
		}
		const action: RulesAction = new RulesAction(dependencies);
		action.execute(parsedFlags);
	}
}

