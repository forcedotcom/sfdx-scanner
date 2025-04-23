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
import {createWorkspace} from '../utils/WorkspaceUtil';
import {ResultsViewer} from '../viewers/ResultsViewer';
import {RunActionSummaryViewer} from '../viewers/ActionSummaryViewer';
import {ResultsWriter} from '../writers/ResultsWriter';
import {LogFileWriter} from '../writers/LogWriter';
import {LogEventListener, LogEventLogger} from '../listeners/LogEventListener';
import {ProgressEventListener} from '../listeners/ProgressEventListener';
import {BundleName, getMessage} from '../messages';
import {NoOpTelemetryEmitter, TelemetryEmitter} from "../Telemetry";
import {TelemetryEventListener} from "../listeners/TelemetryEventListener";

export type RunDependencies = {
	configFactory: CodeAnalyzerConfigFactory;
	pluginsFactory: EnginePluginsFactory;
	logEventListeners: LogEventListener[];
	progressListeners: ProgressEventListener[];
	telemetryEmitter?: TelemetryEmitter;
	writer: ResultsWriter;
	resultsViewer: ResultsViewer;
	actionSummaryViewer: RunActionSummaryViewer;
}

export type RunInput = {
	'config-file'?: string;
	'output-file': string[];
	'rule-selector': string[];
	'severity-threshold'?: SeverityLevel;
	target?: string[];
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
		this.dependencies.actionSummaryViewer.viewPreExecutionSummary(logWriter.getLogDestination());
		// We always add a Logger Listener to the appropriate listeners list, because we should Always Be Logging.
		this.dependencies.logEventListeners.push(new LogEventLogger(logWriter));
		const core: CodeAnalyzer = new CodeAnalyzer(config);
		// LogEventListeners should start listening as soon as the Core is instantiated, since Core can start emitting
		// events they listen for basically immediately.
		this.dependencies.logEventListeners.forEach(listener => listener.listen(core));
		const telemetryListener: TelemetryEventListener = new TelemetryEventListener(this.dependencies.telemetryEmitter ?? new NoOpTelemetryEmitter());
		telemetryListener.listen(core);
		const enginePlugins = this.dependencies.pluginsFactory.create();
		const enginePluginModules = config.getCustomEnginePluginModules();
		const addEnginePromises: Promise<void>[] = [
			...enginePlugins.map(enginePlugin => core.addEnginePlugin(enginePlugin)),
			...enginePluginModules.map(pluginModule => core.dynamicallyAddEnginePlugin(pluginModule))
		];
		await Promise.all(addEnginePromises);
		const workspace: Workspace = await createWorkspace(core, input.workspace, input.target);

		// EngineProgressListeners should start listening right before we call Core's `.selectRules()` method, since
		// that's when progress events can start being emitted.
		this.dependencies.progressListeners.forEach(listener => listener.listen(core));
		const ruleSelection: RuleSelection = await core.selectRules(input['rule-selector'], {workspace});
		const runOptions: RunOptions = {workspace};
		const results: RunResults = await core.run(ruleSelection, runOptions);
		await this.emitEngineExecutionTelemetry(ruleSelection, results, enginePlugins.flatMap(p => p.getAvailableEngineNames()));
		// After Core is done running, the listeners need to be told to stop, since some of them have persistent UI elements
		// or file handlers that must be gracefully ended.
		this.dependencies.progressListeners.forEach(listener => listener.stopListening());
		this.dependencies.logEventListeners.forEach(listener => listener.stopListening());
		telemetryListener.stopListening();
		this.dependencies.writer.write(results);
		this.dependencies.resultsViewer.view(results);
		this.dependencies.actionSummaryViewer.viewPostExecutionSummary(results, logWriter.getLogDestination(), input['output-file']);

		const thresholdValue = input['severity-threshold'];
		if (thresholdValue) {
			throwErrorIfSevThresholdExceeded(thresholdValue, results);
		}
	}

	public static createAction(dependencies: RunDependencies): RunAction {
		return new RunAction(dependencies);
	}

	private emitEngineExecutionTelemetry(ruleSelection: RuleSelection, results: RunResults, coreEngineNames: string[]): Promise<void> {
		const selectedEngineNames: Set<string> = new Set(ruleSelection.getEngineNames());
		const executedEngineNames: Set<string> = new Set(results.getEngineNames());
		const engineTelemetryObject: Record<string, boolean|number> = {};
		for (const coreEngineName of coreEngineNames) {
			const selected: boolean = selectedEngineNames.has(coreEngineName);
			const executed: boolean = executedEngineNames.has(coreEngineName);
			const resultCount: number = (selected && executed) ? results.getEngineRunResults(coreEngineName).getViolationCount() : 0;
			engineTelemetryObject[`${coreEngineName}_selected`] = selected;
			engineTelemetryObject[`${coreEngineName}_executed`] = executed;
			engineTelemetryObject[`${coreEngineName}_violation_count`] = resultCount;
		}
		return this.dependencies.telemetryEmitter
			? this.dependencies.telemetryEmitter.emitTelemetry('RunAction', 'core-engine-data', engineTelemetryObject)
			: Promise.resolve();
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
