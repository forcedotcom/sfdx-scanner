import {Flags, SfCommand} from '@salesforce/sf-plugins-core';
import {SeverityLevel} from '@salesforce/code-analyzer-core';
import {RunAction, RunDependencies, RunInput} from '../../lib/actions/RunAction';
import {View} from '../../Constants';
import {CodeAnalyzerConfigFactoryImpl} from '../../lib/factories/CodeAnalyzerConfigFactory';
import {EnginePluginsFactoryImpl} from '../../lib/factories/EnginePluginsFactory';
import {CompositeResultsWriter} from '../../lib/writers/ResultsWriter';
import {ResultsDetailDisplayer, ResultsNoOpDisplayer, ResultsTableDisplayer, ResultsViewer} from '../../lib/viewers/ResultsViewer';
import {RunActionSummaryViewer} from '../../lib/viewers/ActionSummaryViewer';
import {BundleName, getMessage, getMessages} from '../../lib/messages';
import {LogEventDisplayer} from '../../lib/listeners/LogEventListener';
import {EngineRunProgressSpinner, RuleSelectionProgressSpinner} from '../../lib/listeners/ProgressEventListener';
import {Displayable, UxDisplay} from '../../lib/Display';
import {SfCliTelemetryEmitter} from "../../lib/Telemetry";

export default class RunCommand extends SfCommand<void> implements Displayable {
	// We don't need the `--json` output for this command.
	public static readonly enableJsonFlag = false;
	public static readonly summary = getMessage(BundleName.RunCommand, 'command.summary');
	public static readonly description = getMessage(BundleName.RunCommand, 'command.description');
	public static readonly examples = getMessages(BundleName.RunCommand, 'command.examples');

	public static readonly flags = {
		// === Flags pertaining to targeting ===
		workspace: Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.workspace.summary'),
			description: getMessage(BundleName.RunCommand, 'flags.workspace.description'),
			char: 'w',
			multiple: true,
			delimiter: ',',
			default: ['.']
		}),
		'target': Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.target.summary'),
			description: getMessage(BundleName.RunCommand, 'flags.target.description'),
			char: 't',
			multiple: true,
			delimiter: ','
		}),
		// === Flags pertaining to rule selection ===
		'rule-selector': Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.rule-selector.summary'),
			description: getMessage(BundleName.RunCommand, 'flags.rule-selector.description'),
			char: 'r',
			multiple: true,
			delimiter: ',',
			default: ["Recommended"]
		}),
		// === Flags pertaining to output ===
		'severity-threshold': Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.severity-threshold.summary'),
			description: getMessage(BundleName.RunCommand, 'flags.severity-threshold.description'),
			char: 's'
		}),
		view: Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.view.summary'),
			description: getMessage(BundleName.RunCommand, 'flags.view.description'),
			char: 'v',
			options: Object.values(View)
		}),
		'output-file': Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.output-file.summary'),
			description: getMessage(BundleName.RunCommand, 'flags.output-file.description'),
			char: 'f',
			multiple: true,
			delimiter: ','
		}),
		// === Flags pertaining to configuration ===
		'config-file': Flags.file({
			summary: getMessage(BundleName.RunCommand, 'flags.config-file.summary'),
			description: getMessage(BundleName.RunCommand, 'flags.config-file.description'),
			char: 'c',
			exists: true
		})
	};

	public async run(): Promise<void> {
		const parsedFlags = (await this.parse(RunCommand)).flags;
		const dependencies: RunDependencies = this.createDependencies(parsedFlags.view as View|undefined, parsedFlags['output-file']);
		const action: RunAction = RunAction.createAction(dependencies);
		const runInput: RunInput = {
			'config-file': parsedFlags['config-file'],
			'output-file': parsedFlags['output-file'] ?? [],
			'rule-selector': parsedFlags['rule-selector'],
			'workspace': parsedFlags['workspace'],
			'severity-threshold': parsedFlags['severity-threshold'] === undefined ? undefined :
				convertThresholdToEnum(parsedFlags['severity-threshold'].toLowerCase()),
			'target': parsedFlags['target']
		};
		await action.execute(runInput);
	}

	protected createDependencies(view: View|undefined, outputFiles: string[] = []): RunDependencies {
		const uxDisplay: UxDisplay = new UxDisplay(this, this.spinner);
		const resultsViewer: ResultsViewer = createResultsViewer(view, outputFiles, uxDisplay);
		return {
			configFactory: new CodeAnalyzerConfigFactoryImpl(),
			pluginsFactory: new EnginePluginsFactoryImpl(),
			writer: CompositeResultsWriter.fromFiles(outputFiles),
			telemetryEmitter: new SfCliTelemetryEmitter(),
			logEventListeners: [new LogEventDisplayer(uxDisplay)],
			progressListeners: [new EngineRunProgressSpinner(uxDisplay), new RuleSelectionProgressSpinner(uxDisplay)],
			resultsViewer,
			actionSummaryViewer: new RunActionSummaryViewer(uxDisplay)
		};
	}
}

function convertThresholdToEnum(threshold: string): SeverityLevel {
	// We could do all sorts of complicated conversion logic, but honestly it's just easier
	// to do a switch-statement.
	switch (threshold) {
		case '1':
		case 'critical':
			return SeverityLevel.Critical;
		case '2':
		case 'high':
			return SeverityLevel.High;
		case '3':
		case 'moderate':
			return SeverityLevel.Moderate;
		case '4':
		case 'low':
			return SeverityLevel.Low;
		case '5':
		case 'info':
			return SeverityLevel.Info;
		default:
			throw new Error(getMessage(BundleName.RunCommand, 'error.invalid-severity-threshold',
				[threshold, JSON.stringify(Object.values(SeverityLevel))]));
	}
}

function createResultsViewer(view: View|undefined, outputFiles: string[], uxDisplay: UxDisplay): ResultsViewer {
	switch (view) {
		case View.DETAIL:
			return new ResultsDetailDisplayer(uxDisplay);
		case View.TABLE:
			return new ResultsTableDisplayer(uxDisplay);
		default:
			return outputFiles.length === 0
				? new ResultsTableDisplayer(uxDisplay)
				: new ResultsNoOpDisplayer();
	}
}
