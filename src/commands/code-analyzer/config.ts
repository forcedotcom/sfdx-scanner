import {Flags, SfCommand} from '@salesforce/sf-plugins-core';
import {ConfigAction, ConfigDependencies} from '../../lib/actions/ConfigAction';
import {CodeAnalyzerConfigFactoryImpl} from '../../lib/factories/CodeAnalyzerConfigFactory';
import {EnginePluginsFactoryImpl} from '../../lib/factories/EnginePluginsFactory';
import {CompositeConfigWriter} from '../../lib/writers/ConfigWriter';
import {ConfigDisplayViewer} from '../../lib/viewers/ConfigViewer';
import {BundleName, getMessage, getMessages} from '../../lib/messages';
import {LogEventDisplayer} from '../../lib/listeners/LogEventListener';
import {Displayable, UxDisplay} from '../../lib/Display';

export default class ConfigCommand extends SfCommand<void> implements Displayable {
	// We don't need the `--json` output for this command.
	public static readonly enableJsonFlag = false;
	public static readonly summary = getMessage(BundleName.ConfigCommand, 'command.summary');
	public static readonly description = getMessage(BundleName.ConfigCommand, 'command.description');
	public static readonly examples = getMessages(BundleName.ConfigCommand, 'command.examples');
	// TODO: UN-HIDE WHEN COMMAND IS READY
	public static readonly hidden = true;

	public static readonly flags = {
		workspace: Flags.string({
			summary: getMessage(BundleName.ConfigCommand, 'flags.workspace.summary'),
			description: getMessage(BundleName.ConfigCommand, 'flags.workspace.description'),
			char: 'w',
			multiple: true,
			delimiter: ',',
			// TODO: UN-HIDE WHEN ASSOCIATED FEATURES ARE IMPLEMENTED
			hidden: true
		}),
		'rule-selector': Flags.string({
			summary: getMessage(BundleName.ConfigCommand, 'flags.rule-selector.summary'),
			description: getMessage(BundleName.ConfigCommand, 'flags.rule-selector.description'),
			char: 'r',
			multiple: true,
			delimiter: ',',
			default: ["Recommended"],
			// TODO: UN-HIDE WHEN ASSOCIATED FEATURES ARE IMPLEMENTED
			hidden: true
		}),
		'config-file': Flags.file({
			summary: getMessage(BundleName.ConfigCommand, 'flags.config-file.summary'),
			description: getMessage(BundleName.ConfigCommand, 'flags.config-file.description'),
			char: 'c',
			exists: true,
			// TODO: UN-HIDE WHEN ASSOCIATED FEATURES ARE IMPLEMENTED
			hidden: true
		}),
		'output-file': Flags.string({
			summary: getMessage(BundleName.ConfigCommand, 'flags.output-file.summary'),
			description: getMessage(BundleName.ConfigCommand, 'flags.output-file.description'),
			char: 'f',
			multiple: true,
			delimiter: ',',
			// TODO: UN-HIDE WHEN ASSOCIATED FEATURES ARE IMPLEMENTED
			hidden: true
		})
	};

	public async run(): Promise<void> {
		const parsedFlags = (await this.parse(ConfigCommand)).flags;
		const dependencies: ConfigDependencies = this.createDependencies(parsedFlags['output-file']);
		const action: ConfigAction = ConfigAction.createAction(dependencies);
		await action.execute(parsedFlags);
	}

	protected createDependencies(outputFiles: string[] = []): ConfigDependencies {
		const uxDisplay: UxDisplay = new UxDisplay(this, this.spinner);
		return {
			configFactory: new CodeAnalyzerConfigFactoryImpl(),
			pluginsFactory: new EnginePluginsFactoryImpl(),
			logEventListeners: [new LogEventDisplayer(uxDisplay)],
			writer: CompositeConfigWriter.fromFiles(outputFiles),
			viewer: new ConfigDisplayViewer(uxDisplay)
		};
	}
}
