import {SfError} from '@salesforce/core';
import {
	CodeAnalyzer,
	CodeAnalyzerConfig,
	RuleSelection,
	RunOptions,
	RunResults,
	SeverityLevel
} from '@salesforce/code-analyzer-core';
import {CodeAnalyzerConfigFactory} from '../factories/CodeAnalyzerConfigFactory';
import {EnginePluginsFactory} from '../factories/EnginePluginsFactory';
import {ResultsViewer} from '../viewers/ResultsViewer';
import {ResultsWriter} from '../writers/ResultsWriter';
import {LogEventListener} from '../listeners/LogEventListener';
import {EngineProgressListener} from '../listeners/EngineProgressListener';
import {BundleName, getMessage} from '../messages';

export type RunDependencies = {
	configFactory: CodeAnalyzerConfigFactory;
	pluginsFactory: EnginePluginsFactory;
	logEventListeners: LogEventListener[];
	progressListeners: EngineProgressListener[];
	writer: ResultsWriter;
	viewer: ResultsViewer;
}

export type RunInput = {
	'config-file'?: string;
	'path-start'?: string[];
	'rule-selector': string[];
	'severity-threshold'?: SeverityLevel;
	workspace: string[];
}

export class RunAction {
	private readonly dependencies: RunDependencies;

	private constructor(dependencies: RunDependencies) {
		this.dependencies = dependencies;
	}

	public async execute(input: RunInput): Promise<void> {
		const config: CodeAnalyzerConfig = this.dependencies.configFactory.create(input['config-file']);
		const core: CodeAnalyzer = new CodeAnalyzer(config);

		const enginePlugins = this.dependencies.pluginsFactory.create();
		const enginePluginModules = config.getCustomEnginePluginModules()
			.map(pluginModule => require.resolve(pluginModule, {paths: [process.cwd()]})); // TODO: Remove this line as soon as it is moved to the core module.
		const addEnginePromises: Promise<void>[] = [
			...enginePlugins.map(enginePlugin => core.addEnginePlugin(enginePlugin)),
			...enginePluginModules.map(pluginModule => core.dynamicallyAddEnginePlugin(pluginModule))
		];
		await Promise.all(addEnginePromises);

		const ruleSelection: RuleSelection = core.selectRules(...input['rule-selector']);
		const runOptions: RunOptions = {
			workspaceFiles: input.workspace,
			pathStartPoints: input['path-start']
		};
		this.dependencies.progressListeners.forEach(listener => listener.listen(core, ruleSelection));
		this.dependencies.logEventListeners.forEach(listener => listener.listen(core));
		const results: RunResults = await core.run(ruleSelection, runOptions);
		this.dependencies.progressListeners.forEach(listener => listener.stopListening());
		this.dependencies.writer.write(results);
		this.dependencies.viewer.view(results);

		if (input['severity-threshold']) {
			const thresholdValue = input['severity-threshold'];
			throwErrorIfSevThresholdExceeded(thresholdValue, results);
		}
	}

	public static createAction(dependencies: RunDependencies): RunAction {
		return new RunAction(dependencies);
	}
}

function throwErrorIfSevThresholdExceeded(threshold: SeverityLevel, results: RunResults): void {
	let exceedingCount = 0;
	let mostIntenseSeverity = Number.MAX_SAFE_INTEGER;
	for (let i: number = threshold; i > 0; i--) {
		const sevCount = results.getViolationCountOfSeverity(i);
		if (sevCount > 0) {
			exceedingCount += sevCount;
			mostIntenseSeverity = i;
		}
	}
	if (exceedingCount > 0) {
		const message = getMessage(BundleName.RunAction, 'error.severity-threshold-exceeded', [exceedingCount, SeverityLevel[threshold]]);
		// Use an SfError because we can easily set an exit code, and it will know what to do with that.
		throw new SfError(message, 'ThresholdExceeded', [], mostIntenseSeverity);
	}
}
