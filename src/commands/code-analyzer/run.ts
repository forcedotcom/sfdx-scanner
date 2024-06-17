import {Flags, SfCommand} from '@salesforce/sf-plugins-core';
import {SeverityLevel} from '@salesforce/code-analyzer-core';
import {RunAction, RunDependencies, RunInput} from '../../lib/actions/RunAction';
import {View} from '../../Constants';
import {CodeAnalyzerConfigFactoryImpl} from '../../lib/factories/CodeAnalyzerConfigFactory';
import {EnginePluginFactoryImpl} from '../../lib/factories/EnginePluginFactory';
import {OutputFileWriterImpl} from '../../lib/writers/OutputFileWriter';
import {ResultsDetailViewer, ResultsTableViewer} from '../../lib/viewers/ResultsViewer';
import {BundleName, getMessage} from '../../lib/messages';
import {Displayable, UxDisplay} from '../../lib/Display';

export default class RunCommand extends SfCommand<void> implements Displayable {
	// We don't need the `--json` output for this command.
	public static readonly enableJsonFlag = false;
	public static readonly summary = getMessage(BundleName.RunCommand, 'command.summary');
	public static readonly description = getMessage(BundleName.RunCommand, 'command.description');
	public static readonly examples = [
		getMessage(BundleName.RunCommand, 'command.examples')
	];

	public static readonly flags = {
		// === Flags pertaining to targeting ===
		workspace: Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.workspace.summary'),
			char: 'w',
			multiple: true,
			delimiter: ',',
			default: ['.']
		}),
		'path-start': Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.path-start.summary'),
			char: 's',
			multiple: true,
			delimiter: ','
		}),
		// === Flags pertaining to rule selection ===
		'rule-selector': Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.rule-selector.summary'),
			char: 'r',
			multiple: true,
			delimiter: ',',
			default: ["Recommended"]
		}),
		// === Flags pertaining to output ===
		'severity-threshold': Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.severity-threshold.summary'),
			char: 't',
			options: [
				'1', 'critical',
				'2', 'high',
				'3', 'moderate',
				'4', 'low',
				'5', 'info'
			]
		}),
		view: Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.view.summary'),
			char: 'v',
			default: View.TABLE,
			options: Object.values(View)
		}),
		'output-file': Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.output-file.summary'),
			char: 'f',
			multiple: true,
			delimiter: ','
		}),
		// === Flags pertaining to configuration ===
		'config-file': Flags.file({
			summary: getMessage(BundleName.RunCommand, 'flags.config-file.summary'),
			char: 'c',
			exists: true
		})
	};

	public async run(): Promise<void> {
		const parsedFlags = (await this.parse(RunCommand)).flags;
		const dependencies: RunDependencies = this.createDependencies(parsedFlags.view as View, parsedFlags['output-file']);
		const action: RunAction = RunAction.createAction(dependencies);
		const runInput: RunInput = {
			'config-file': parsedFlags['config-file'],
			'path-start': parsedFlags['path-start'],
			'rule-selector': parsedFlags['rule-selector'],
			'workspace': parsedFlags['workspace']
		};
		const possibleThreshold = this.convertThresholdToEnum(parsedFlags['severity-threshold']);
		if (possibleThreshold) {
			runInput['severity-threshold'] = possibleThreshold;
		}
		await action.execute(runInput);
	}

	protected createDependencies(view: View, outputFiles: string[] = []): RunDependencies {
		const uxDisplay: UxDisplay = new UxDisplay(this);
		return {
			configFactory: new CodeAnalyzerConfigFactoryImpl(),
			engineFactory: new EnginePluginFactoryImpl(),
			outputFileWriter: new OutputFileWriterImpl(outputFiles),
			viewer: view === View.TABLE
				? new ResultsTableViewer(uxDisplay)
				: new ResultsDetailViewer(uxDisplay)
		};
	}

	private convertThresholdToEnum(threshold: string|undefined): SeverityLevel|undefined {
		// We could do all sorts of complicated conversion logic, but honestly it's just easier
		// to do a switch-statement.
		switch (threshold) {
			case undefined:
				return undefined;
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
				// This should never happen, so the error doesn't need to be polished.
				throw new Error(`Developer error: Unexpected severity ${threshold}`);
		}
	}
}

