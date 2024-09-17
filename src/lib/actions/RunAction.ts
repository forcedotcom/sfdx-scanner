import {SfError} from '@salesforce/core';
import {
	CodeAnalyzer,
	CodeAnalyzerConfig,
	RuleSelection,
	RunOptions,
	RunResults,
	SeverityLevel,
	Workspace
} from '@salesforce/code-analyzer-core';
import {CodeAnalyzerConfigFactory} from '../factories/CodeAnalyzerConfigFactory';
import {EnginePluginsFactory} from '../factories/EnginePluginsFactory';
import {createPathStarts} from '../utils/PathStartUtil';
import {createWorkspace} from '../utils/WorkspaceUtil';
import {ResultsViewer} from '../viewers/ResultsViewer';
import {RunSummaryViewer} from '../viewers/RunSummaryViewer';
import {ResultsWriter} from '../writers/ResultsWriter';
import {LogFileWriter} from '../writers/LogWriter';
import {LogEventListener, LogEventLogger} from '../listeners/LogEventListener';
import {ProgressEventListener} from '../listeners/ProgressEventListener';
import {BundleName, getMessage} from '../messages';

export type RunDependencies = {
	configFactory: CodeAnalyzerConfigFactory;
	pluginsFactory: EnginePluginsFactory;
	logEventListeners: LogEventListener[];
	progressListeners: ProgressEventListener[];
	writer: ResultsWriter;
	resultsViewer: ResultsViewer;
	runSummaryViewer: RunSummaryViewer;
}

export type RunInput = {
	'config-file'?: string;
	'output-file': string[];
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
		const workspace: Workspace = await createWorkspace(core, input.workspace);

		// EngineProgressListeners should start listening right before we call Core's `.selectRules()` method, since
		// that's when progress events can start being emitted.
		this.dependencies.progressListeners.forEach(listener => listener.listen(core));
		const ruleSelection: RuleSelection = await core.selectRules(input['rule-selector'], {workspace});
		const runOptions: RunOptions = {
			workspace,
			pathStartPoints: await createPathStarts(input['path-start'])
		};
		const results: RunResults = await core.run(ruleSelection, runOptions);
		// After Core is done running, the listeners need to be told to stop, since some of them have persistent UI elements
		// or file handlers that must be gracefully ended.
		this.dependencies.progressListeners.forEach(listener => listener.stopListening());
		this.dependencies.logEventListeners.forEach(listener => listener.stopListening());
		this.dependencies.writer.write(results);
		this.dependencies.resultsViewer.view(results);
		this.dependencies.runSummaryViewer.view(results, config, input['output-file']);

		const thresholdValue = input['severity-threshold'];
		if (thresholdValue) {
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
