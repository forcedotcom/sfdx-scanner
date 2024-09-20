import {Flags, SfCommand} from '@salesforce/sf-plugins-core';
import {ConfigAction, ConfigDependencies} from '../../lib/actions/ConfigAction';
import {ConfigFileWriter} from '../../lib/writers/ConfigWriter';
import {ConfigStyledYamlViewer} from '../../lib/viewers/ConfigViewer';
import {CodeAnalyzerConfigFactoryImpl} from '../../lib/factories/CodeAnalyzerConfigFactory';
import {EnginePluginsFactoryImpl} from '../../lib/factories/EnginePluginsFactory';
import {BundleName, getMessage, getMessages} from '../../lib/messages';
import {LogEventDisplayer} from '../../lib/listeners/LogEventListener';
import {RuleSelectionProgressSpinner} from '../../lib/listeners/ProgressEventListener';
import {AnnotatedConfigModel, ConfigContext} from '../../lib/models/ConfigModel';
import {Displayable, UxDisplay} from '../../lib/Display';

export default class ConfigCommand extends SfCommand<void> implements Displayable {
	// We don't need the `--json` output for this command.
	public static readonly enableJsonFlag = false;
	public static readonly summary = getMessage(BundleName.ConfigCommand, 'command.summary');
	public static readonly description = getMessage(BundleName.ConfigCommand, 'command.description');
	public static readonly examples = getMessages(BundleName.ConfigCommand, 'command.examples');

	// TODO: Update when we go to Beta and when we go GA
	public static readonly state = getMessage(BundleName.Shared, 'label.command-state');

	public static readonly flags = {
		workspace: Flags.string({
			summary: getMessage(BundleName.ConfigCommand, 'flags.workspace.summary'),
			description: getMessage(BundleName.ConfigCommand, 'flags.workspace.description'),
			char: 'w',
			multiple: true,
			delimiter: ','
		}),
		'rule-selector': Flags.string({
			summary: getMessage(BundleName.ConfigCommand, 'flags.rule-selector.summary'),
			description: getMessage(BundleName.ConfigCommand, 'flags.rule-selector.description'),
			char: 'r',
			multiple: true,
			delimiter: ',',
			default: ["Recommended"]
		}),
		'config-file': Flags.file({
			summary: getMessage(BundleName.ConfigCommand, 'flags.config-file.summary'),
			description: getMessage(BundleName.ConfigCommand, 'flags.config-file.description'),
			char: 'c',
			exists: true
		}),
		'output-file': Flags.string({
			summary: getMessage(BundleName.ConfigCommand, 'flags.output-file.summary'),
			description: getMessage(BundleName.ConfigCommand, 'flags.output-file.description'),
			char: 'f'
		})
	};

	public async run(): Promise<void> {
		// TODO: Update when we go to Beta and when we go GA
		this.warn(getMessage(BundleName.Shared, "warning.command-state", [getMessage(BundleName.Shared, 'label.command-state')]));

		const parsedFlags = (await this.parse(ConfigCommand)).flags;
		const dependencies: ConfigDependencies = this.createDependencies(parsedFlags['output-file']);
		const action: ConfigAction = ConfigAction.createAction(dependencies);
		await action.execute(parsedFlags);
	}

	protected createDependencies(outputFile?: string): ConfigDependencies {
		const uxDisplay: UxDisplay = new UxDisplay(this, this.spinner);
		const modelGeneratorFunction = (relevantEngines: Set<string>, userContext: ConfigContext, defaultContext: ConfigContext) => {
			return AnnotatedConfigModel.fromSelection(relevantEngines, userContext, defaultContext);
		};
		const dependencies: ConfigDependencies = {
			configFactory: new CodeAnalyzerConfigFactoryImpl(),
			pluginsFactory: new EnginePluginsFactoryImpl(),
			logEventListeners: [new LogEventDisplayer(uxDisplay)],
			progressEventListeners: [new RuleSelectionProgressSpinner(uxDisplay)],
			modelGenerator: modelGeneratorFunction,
			viewer: new ConfigStyledYamlViewer(uxDisplay)
		};
		if (outputFile) {
			dependencies.writer = ConfigFileWriter.fromFile(outputFile);
		}
		return dependencies;
	}
}