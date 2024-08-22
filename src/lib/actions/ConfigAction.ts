import {CodeAnalyzerConfig, CodeAnalyzer, RuleSelection} from "@salesforce/code-analyzer-core";
import {CodeAnalyzerConfigFactory} from '../factories/CodeAnalyzerConfigFactory';
import {EnginePluginsFactory} from '../factories/EnginePluginsFactory';
import {ConfigWriter} from '../writers/ConfigWriter';
import {LogFileWriter} from '../writers/LogWriter';
import {ConfigViewer} from '../viewers/ConfigViewer';
import {createWorkspace} from '../utils/WorkspaceUtil';
import {LogEventListener, LogEventLogger} from '../listeners/LogEventListener';
import {ProgressEventListener} from '../listeners/ProgressEventListener';
import {ConfigModel, ConfigModelGeneratorFunction} from '../models/ConfigModel';

export type ConfigDependencies = {
	configFactory: CodeAnalyzerConfigFactory;
	pluginsFactory: EnginePluginsFactory;
	modelGenerator: ConfigModelGeneratorFunction;
	logEventListeners: LogEventListener[];
	progressEventListeners: ProgressEventListener[];
	writer?: ConfigWriter;
	viewer: ConfigViewer;
};

export type ConfigInput = {
	'config-file'?: string;
	'rule-selector': string[];
	workspace?: string[];
};

export class ConfigAction {
	private readonly dependencies: ConfigDependencies;

	private constructor(dependencies: ConfigDependencies) {
		this.dependencies = dependencies;
	}

	public async execute(input: ConfigInput): Promise<void> {
		const config: CodeAnalyzerConfig = this.dependencies.configFactory.create(input['config-file']);
		const logWriter: LogFileWriter = await LogFileWriter.fromConfig(config);
		// We always add a Logger Listener to the appropriate listeners list, because we should Always Be Logging.
		this.dependencies.logEventListeners.push(new LogEventLogger(logWriter));

		const core: CodeAnalyzer = new CodeAnalyzer(config);

		// LogEventListeners should start listening as soon as the Core is instantiated, since Core can start emitting
		// events they listen for basically immediately.
		this.dependencies.logEventListeners.forEach(listener => listener.listen(core));
		const enginePlugins = this.dependencies.pluginsFactory.create();
		const enginePluginModules = config.getCustomEnginePluginModules();
		const addEnginePromises: Promise<void>[] = [
			...enginePlugins.map(enginePlugin => core.addEnginePlugin(enginePlugin)),
			...enginePluginModules.map(pluginModule => core.dynamicallyAddEnginePlugin(pluginModule))
		];
		await Promise.all(addEnginePromises);

		const selectOptions = input.workspace
			? {workspace: await createWorkspace(core, input.workspace)}
			: undefined;
		// EngineProgressListeners should start listening right before we call Core's `.selectRules()` method, since
		// that's when progress events can start being emitted.
		this.dependencies.progressEventListeners.forEach(listener => listener.listen(core));
		const ruleSelection: RuleSelection = await core.selectRules(input['rule-selector'], selectOptions);

		// After Core is done running, the listeners need to be told to stop, since some of them have persistent UI elements
		// or file handlers that must be gracefully ended.
		this.dependencies.progressEventListeners.forEach(listener => listener.stopListening());
		this.dependencies.logEventListeners.forEach(listener => listener.stopListening());

		const configModel: ConfigModel = this.dependencies.modelGenerator(config, ruleSelection);

		this.dependencies.viewer.view(configModel);
		this.dependencies.writer?.write(configModel);
		return Promise.resolve();
	}

	public static createAction(dependencies: ConfigDependencies): ConfigAction {
		return new ConfigAction(dependencies)
	}
}
