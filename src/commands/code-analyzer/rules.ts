import {Flags, SfCommand} from '@salesforce/sf-plugins-core';
import {View} from '../../Constants';
import {CodeAnalyzerConfigFactoryImpl} from '../../lib/factories/CodeAnalyzerConfigFactory';
import {EnginePluginsFactoryImpl} from '../../lib/factories/EnginePluginsFactory';
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
		workspace: Flags.string({
			summary: getMessage(BundleName.RulesCommand, 'flags.workspace.summary'),
			char: 'w',
			multiple: true,
			delimiter: ',',
			default: ['.']
		}),
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
		const dependencies: RulesDependencies = this.createDependencies(parsedFlags.view as View);
		const action: RulesAction = RulesAction.createAction(dependencies);
		await action.execute(parsedFlags);
	}

	protected createDependencies(view: View): RulesDependencies {
		const uxDisplay: UxDisplay = new UxDisplay(this, this.spinner);
		return {
			configFactory: new CodeAnalyzerConfigFactoryImpl(),
			pluginsFactory: new EnginePluginsFactoryImpl(),
			viewer: view === View.TABLE ? new RuleTableViewer(uxDisplay) : new RuleDetailViewer(uxDisplay)
		};
	}
}

