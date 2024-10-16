import {CodeAnalyzerConfig, CodeAnalyzer} from "@salesforce/code-analyzer-core";
import {CodeAnalyzerConfigFactory} from '../factories/CodeAnalyzerConfigFactory';
import {EnginePluginsFactory} from '../factories/EnginePluginsFactory';
import {ConfigWriter} from '../writers/ConfigWriter';
import {LogFileWriter} from '../writers/LogWriter';
import {ConfigViewer} from '../viewers/ConfigViewer';
import {createWorkspace} from '../utils/WorkspaceUtil';
import {LogEventListener, LogEventLogger} from '../listeners/LogEventListener';
import {ProgressEventListener} from '../listeners/ProgressEventListener';
import {ConfigActionSummaryViewer} from '../viewers/ActionSummaryViewer';
import {ConfigModel, ConfigModelGeneratorFunction, ConfigContext} from '../models/ConfigModel';

export type ConfigDependencies = {
	configFactory: CodeAnalyzerConfigFactory;
	pluginsFactory: EnginePluginsFactory;
	modelGenerator: ConfigModelGeneratorFunction;
	logEventListeners: LogEventListener[];
	progressEventListeners: ProgressEventListener[];
	writer?: ConfigWriter;
	actionSummaryViewer: ConfigActionSummaryViewer;
	viewer: ConfigViewer;
};

export type ConfigInput = {
	'config-file'?: string;
	'output-file'?: string;
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

		// We always add a Logger Listener to the appropriate listeners list, because we should always be logging.
		const logFileWriter: LogFileWriter = await LogFileWriter.fromConfig(userConfig);
		const logEventLogger: LogEventLogger = new LogEventLogger(logFileWriter);
		this.dependencies.logEventListeners.push(logEventLogger);

		// The User's config produces one Core.
		const userCore: CodeAnalyzer = new CodeAnalyzer(userConfig);
		// The Default config produces two Cores, since we have to run two selections.
		const defaultCoreForAllRules: CodeAnalyzer = new CodeAnalyzer(defaultConfig);
		const defaultCoreForSelectRules: CodeAnalyzer = new CodeAnalyzer(defaultConfig);

		// LogEventListeners should start listening as soon as the User Core is instantiated, since it can start emitting
		// relevant events basically immediately.
		this.dependencies.logEventListeners.forEach(listener => listener.listen(userCore));
		// Only the File Logger should listen to the Default Cores, since we don't want to bother the user with redundant
		// logs printed to the console.
		logEventLogger.listen(defaultCoreForAllRules);
		logEventLogger.listen(defaultCoreForSelectRules);

		const enginePlugins = this.dependencies.pluginsFactory.create();
		const enginePluginModules = userConfig.getCustomEnginePluginModules();

		const addEnginePromises: Promise<void>[] = [
			// Assumption: It's safe for the different cores to share the same plugins, because all currently existent
			// plugins return unique engines every time. This code may fail or behave unexpectedly if a plugin reuses engines.
			...enginePlugins.map(enginePlugin => userCore.addEnginePlugin(enginePlugin)),
			...enginePlugins.map(enginePlugin => defaultCoreForAllRules.addEnginePlugin(enginePlugin)),
			...enginePlugins.map(enginePlugin => defaultCoreForSelectRules.addEnginePlugin(enginePlugin)),
			...enginePluginModules.map(pluginModule => userCore.dynamicallyAddEnginePlugin(pluginModule)),
			// Assumption: Every engine's default configuration is sufficient to allow that engine to be instantiated,
			// or throw a clear error indicating the problem.
			...enginePluginModules.map(pluginModule => defaultCoreForAllRules.dynamicallyAddEnginePlugin(pluginModule)),
			...enginePluginModules.map(pluginModule => defaultCoreForSelectRules.dynamicallyAddEnginePlugin(pluginModule)),
		];
		await Promise.all(addEnginePromises);

		const userSelectOptions = input.workspace
			? {workspace: await createWorkspace(userCore, input.workspace)}
			: undefined;
		const defaultSelectOptionsForAllRules = input.workspace
			? {workspace: await createWorkspace(defaultCoreForAllRules, input.workspace)}
			: undefined;
		const defaultSelectOptionsForSelectRules = input.workspace
			? {workspace: await createWorkspace(defaultCoreForSelectRules, input.workspace)}
			: undefined;

		// EngineProgressListeners should start listening right before we call the Cores' `.selectRules()` methods, since
		// that's when progress events can start being emitted.
		this.dependencies.progressEventListeners.forEach(listener => listener.listen(userCore, defaultCoreForAllRules, defaultCoreForSelectRules));

		const [userRules, allDefaultRules, selectedDefaultRules] = await Promise.all([
			// The user Core should respect the user's selection criteria.
			userCore.selectRules(input['rule-selector'], userSelectOptions),
			// One of the Default Cores should always use "all", to minimize the likelihood of a rule's default state
			// being unavailable for comparison.
			defaultCoreForAllRules.selectRules(['all'], defaultSelectOptionsForAllRules),
			// One of the Default Cores should use the same selectors as the User Core, so we know what rules are available
			// by default.
			defaultCoreForSelectRules.selectRules(input['rule-selector'], defaultSelectOptionsForSelectRules)
		]);

		// After the Cores are done running, the listeners need to be told to stop, since some of them have persistent UI
		// elements or file handlers that must be gracefully ended.
		this.dependencies.progressEventListeners.forEach(listener => listener.stopListening());
		this.dependencies.logEventListeners.forEach(listener => listener.stopListening());

		// We need the Set of all Engines that returned rules for the user's selection on both the Default and User Cores.
		const relevantEngines: Set<string> = new Set([...userRules.getEngineNames(), ...selectedDefaultRules.getEngineNames()]);

		const userConfigContext: ConfigContext = {
			config: userConfig,
			core: userCore,
			rules: userRules
		};
		const defaultConfigContext: ConfigContext = {
			config: defaultConfig,
			core: defaultCoreForAllRules,
			rules: allDefaultRules
		};
		const configModel: ConfigModel = this.dependencies.modelGenerator(relevantEngines, userConfigContext, defaultConfigContext);

		this.dependencies.viewer.view(configModel);
		const fileWritten: boolean = this.dependencies.writer
			? await this.dependencies.writer.write(configModel)
			: false;
		this.dependencies.actionSummaryViewer.view(logFileWriter.getLogDestination(), fileWritten ? input['output-file'] : undefined);
		return Promise.resolve();
	}

	public static createAction(dependencies: ConfigDependencies): ConfigAction {
		return new ConfigAction(dependencies)
	}
}
