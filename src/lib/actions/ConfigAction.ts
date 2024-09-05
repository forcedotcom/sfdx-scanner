import {CodeAnalyzerConfig, CodeAnalyzer} from "@salesforce/code-analyzer-core";
import {CodeAnalyzerConfigFactory} from '../factories/CodeAnalyzerConfigFactory';
import {EnginePluginsFactory} from '../factories/EnginePluginsFactory';
import {ConfigWriter} from '../writers/ConfigWriter';
import {LogFileWriter} from '../writers/LogWriter';
import {ConfigViewer} from '../viewers/ConfigViewer';
import {createWorkspace} from '../utils/WorkspaceUtil';
import {LogEventListener, LogEventLogger} from '../listeners/LogEventListener';
import {ProgressEventListener} from '../listeners/ProgressEventListener';
import {ConfigModel, ConfigModelGeneratorFunction, ConfigState} from '../models/ConfigModel';

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
		// We need the user's Config and the default Config.
		const userConfig: CodeAnalyzerConfig = this.dependencies.configFactory.create(input['config-file']);
		const defaultConfig: CodeAnalyzerConfig = CodeAnalyzerConfig.withDefaults();

		const logWriter: LogFileWriter = await LogFileWriter.fromConfig(userConfig);
		// We always add a Logger Listener to the appropriate listeners list, because we should always be logging.
		this.dependencies.logEventListeners.push(new LogEventLogger(logWriter));

		// Use each config to instantiate a Core.
		const userCore: CodeAnalyzer = new CodeAnalyzer(userConfig);
		const defaultCore: CodeAnalyzer = new CodeAnalyzer(defaultConfig);

		// LogEventListeners should start listening as soon as the Cores are instantiated, since Cores can start emitting
		// relevant events basically immediately.
		this.dependencies.logEventListeners.forEach(listener => listener.listen(userCore));
		this.dependencies.logEventListeners.forEach(listener => listener.listen(defaultCore));

		// Add the standard plugins to both Cores, but only the user Core gets the custom modules.
		// This is because we can't assume that the default Config is sufficient for the custom plugins.
		const userEnginePlugins = this.dependencies.pluginsFactory.create();
		const userEnginePluginModules = userConfig.getCustomEnginePluginModules();
		const defaultEnginePlugins = this.dependencies.pluginsFactory.create();

		const addEnginePromises: Promise<void>[] = [
			...userEnginePlugins.map(enginePlugin => userCore.addEnginePlugin(enginePlugin)),
			...defaultEnginePlugins.map(enginePlugin => defaultCore.addEnginePlugin(enginePlugin)),
			...userEnginePluginModules.map(pluginModule => userCore.dynamicallyAddEnginePlugin(pluginModule))
		];
		await Promise.all(addEnginePromises);

		const userSelectOptions = input.workspace
			? {workspace: await createWorkspace(userCore, input.workspace)}
			: undefined;
		const defaultSelectOptions = input.workspace
			? {workspace: await createWorkspace(defaultCore, input.workspace)}
			: undefined;

		// EngineProgressListeners should start listening right before we call the Cores' `.selectRules()` methods, since
		// that's when progress events can start being emitted.
		this.dependencies.progressEventListeners.forEach(listener => listener.listen(userCore, defaultCore));

		const [userRules, defaultRules] = await Promise.all([
			// The user Core should respect the user's selection criteria.
			userCore.selectRules(input['rule-selector'], userSelectOptions),
			// The default Core should always use "all", to minimize the likelihood of a rule's default state being unavailable
			// for comparison.
			defaultCore.selectRules(['all'], defaultSelectOptions)
		]);

		// After the Cores are done running, the listeners need to be told to stop, since some of them have persistent UI
		// elements or file handlers that must be gracefully ended.
		this.dependencies.progressEventListeners.forEach(listener => listener.stopListening());
		this.dependencies.logEventListeners.forEach(listener => listener.stopListening());

		const userConfigState: ConfigState = {
			config: userConfig,
			core: userCore,
			rules: userRules
		};
		const defaultConfigState: ConfigState = {
			config: defaultConfig,
			core: defaultCore,
			rules: defaultRules
		};
		const configModel: ConfigModel = this.dependencies.modelGenerator(userConfigState, defaultConfigState);

		this.dependencies.viewer.view(configModel);
		this.dependencies.writer?.write(configModel);
		return Promise.resolve();
	}

	public static createAction(dependencies: ConfigDependencies): ConfigAction {
		return new ConfigAction(dependencies)
	}
}
