import {CodeAnalyzer, CodeAnalyzerConfig, Rule, RuleSelection} from '@salesforce/code-analyzer-core';

import {CodeAnalyzerConfigFactory} from '../factories/CodeAnalyzerConfigFactory';
import {EnginePluginsFactory} from '../factories/EnginePluginsFactory';
import {ProgressEventListener} from '../listeners/ProgressEventListener';
import {LogFileWriter} from '../writers/LogWriter';
import {LogEventListener, LogEventLogger} from '../listeners/LogEventListener';
import {RuleViewer} from '../viewers/RuleViewer';

export type RulesDependencies = {
	configFactory: CodeAnalyzerConfigFactory;
	pluginsFactory: EnginePluginsFactory;
	logEventListeners: LogEventListener[];
	progressListeners: ProgressEventListener[];
	viewer: RuleViewer;
}

export type RulesInput = {
	'config-file'?: string;
	'rule-selector': string[];
	workspace?: string[];
}

export class RulesAction {
	private readonly dependencies: RulesDependencies;

	private constructor(dependencies: RulesDependencies) {
		this.dependencies = dependencies;
	}

	public async execute(input: RulesInput): Promise<void> {
		const config: CodeAnalyzerConfig = this.dependencies.configFactory.create(input['config-file']);
		const logWriter: LogFileWriter = await LogFileWriter.fromConfig(config);
		// We always add a Logger Listener to the appropriate listeners list, because we should Always Be Logging.
		this.dependencies.logEventListeners.push(new LogEventLogger(logWriter));
		const core: CodeAnalyzer = new CodeAnalyzer(config);
		// LogEventListeners should start listening as soon as the Core is instantiated, since Core can start emitting
		// events they listen for basically immediately.
		this.dependencies.logEventListeners.forEach(listener => listener.listen(core));
		const enginePlugins = this.dependencies.pluginsFactory.create();
		const enginePluginModules = config.getCustomEnginePluginModules()
			.map(pluginModule => require.resolve(pluginModule, {paths: [process.cwd()]})); // TODO: Remove this line as soon as it is moved to the core module.
		const addEnginePromises: Promise<void>[] = [
			...enginePlugins.map(enginePlugin => core.addEnginePlugin(enginePlugin)),
			...enginePluginModules.map(pluginModule => core.dynamicallyAddEnginePlugin(pluginModule))
		];
		await Promise.all(addEnginePromises);
		const selectOptions = input.workspace ? {workspace: await core.createWorkspace(input.workspace)} : undefined;
		// EngineProgressListeners should start listening right before we call Core's `.selectRules()` method, since
		// that's when progress events can start being emitted.
		this.dependencies.progressListeners.forEach(listener => listener.listen(core));
		const ruleSelection: RuleSelection = await core.selectRules(input["rule-selector"], selectOptions);
		// After Core is done running, the listeners need to be told to stop, since some of them have persistent UI elements
		// or file handlers that must be gracefully ended.
		this.dependencies.progressListeners.forEach(listener => listener.stopListening());
		this.dependencies.logEventListeners.forEach(listener => listener.stopListening());
		const rules: Rule[] = core.getEngineNames().flatMap(name => ruleSelection.getRulesFor(name));

		this.dependencies.viewer.view(rules);
	}

	public static createAction(dependencies: RulesDependencies): RulesAction {
		return new RulesAction(dependencies);
	}
}
