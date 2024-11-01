import {Flags, SfCommand} from '@salesforce/sf-plugins-core';
import {View} from '../../Constants';
import {CodeAnalyzerConfigFactoryImpl} from '../../lib/factories/CodeAnalyzerConfigFactory';
import {EnginePluginsFactoryImpl} from '../../lib/factories/EnginePluginsFactory';
import {RuleDetailDisplayer, RuleTableDisplayer} from '../../lib/viewers/RuleViewer';
import {RulesActionSummaryViewer} from '../../lib/viewers/ActionSummaryViewer';
import {RulesAction, RulesDependencies} from '../../lib/actions/RulesAction';
import {BundleName, getMessage, getMessages} from '../../lib/messages';
import {Displayable, UxDisplay} from '../../lib/Display';
import {LogEventDisplayer} from '../../lib/listeners/LogEventListener';
import {RuleSelectionProgressSpinner} from '../../lib/listeners/ProgressEventListener';

export default class RulesCommand extends SfCommand<void> implements Displayable {
	// We don't need the `--json` output for this command.
	public static readonly enableJsonFlag = false;
	public static readonly summary = getMessage(BundleName.RulesCommand, 'command.summary');
	public static readonly description = getMessage(BundleName.RulesCommand, 'command.description');
	public static readonly examples = getMessages(BundleName.RulesCommand, 'command.examples');

	// TODO: Update when we go to Beta and when we go GA
	public static readonly state = getMessage(BundleName.Shared, 'label.command-state');

	public static readonly flags = {
		workspace: Flags.string({
			summary: getMessage(BundleName.RulesCommand, 'flags.workspace.summary'),
			description: getMessage(BundleName.RulesCommand, 'flags.workspace.description'),
			char: 'w',
			multiple: true,
			delimiter: ',',
		}),
		'rule-selector': Flags.string({
			summary: getMessage(BundleName.RulesCommand, 'flags.rule-selector.summary'),
			description: getMessage(BundleName.RulesCommand, 'flags.rule-selector.description'),
			char: 'r',
			multiple: true,
			delimiter: ',',
			default: ["Recommended"]
		}),
		'config-file': Flags.file({
			summary: getMessage(BundleName.RulesCommand, 'flags.config-file.summary'),
			description: getMessage(BundleName.RulesCommand, 'flags.config-file.description'),
			char: 'c',
			exists: true
		}),
		view: Flags.string({
			summary: getMessage(BundleName.RulesCommand, 'flags.view.summary'),
			description: getMessage(BundleName.RulesCommand, 'flags.view.description'),
			char: 'v',
			default: View.TABLE,
			options: Object.values(View)
		})
	};

	public async run(): Promise<void> {
		// TODO: Update when we go to Beta and when we go GA
		this.warn(getMessage(BundleName.Shared, "warning.command-state", [getMessage(BundleName.Shared, 'label.command-state')]));

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
			logEventListeners: [new LogEventDisplayer(uxDisplay)],
			progressListeners: [new RuleSelectionProgressSpinner(uxDisplay)],
			actionSummaryViewer: new RulesActionSummaryViewer(uxDisplay),
			viewer: view === View.TABLE ? new RuleTableDisplayer(uxDisplay) : new RuleDetailDisplayer(uxDisplay)
		};
	}
}

