import {CodeAnalyzer, CodeAnalyzerConfig, RuleSelection, RunOptions, RunResults, SeverityLevel} from '@salesforce/code-analyzer-core';
import {CodeAnalyzerConfigFactory} from '../factories/CodeAnalyzerConfigFactory';
import {EnginePluginFactory} from '../factories/EnginePluginFactory';
import {ResultsViewer} from '../viewers/ResultsViewer';
import {OutputFileWriter} from '../writers/OutputFileWriter';
import {BundleName, getMessage} from '../messages';

export type RunDependencies = {
	configFactory: CodeAnalyzerConfigFactory;
	engineFactory: EnginePluginFactory;
	outputFileWriter: OutputFileWriter;
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

		const enginePlugins = this.dependencies.engineFactory.create();
		const addEnginePromises: Promise<void>[] = enginePlugins.map(e => core.addEnginePlugin(e));
		await Promise.all(addEnginePromises);

		const ruleSelection: RuleSelection = core.selectRules(...input['rule-selector']);
		const runOptions: RunOptions = {
			workspaceFiles: input.workspace,
			pathStartPoints: input['path-start']
		};

		const results: RunResults = await core.run(ruleSelection, runOptions);

		this.dependencies.outputFileWriter.writeToFiles(results);
		this.dependencies.viewer.view(results);

		if (input['severity-threshold']) {
			const thresholdValue = input['severity-threshold'];
			let exceedingCount = 0;
			for (let i = 1; i <= thresholdValue; i++) {
				exceedingCount += results.getViolationCountOfSeverity(i);
			}
			if (exceedingCount > 0) {
				throw new ThresholdExceededError(
					getMessage(BundleName.RunAction, 'error.severity-threshold-exceeded', [exceedingCount, SeverityLevel[thresholdValue]]),
					thresholdValue);
			}
		}
	}

	public static createAction(dependencies: RunDependencies): RunAction {
		return new RunAction(dependencies);
	}
}

class ThresholdExceededError extends Error {
	private readonly threshold: SeverityLevel;

	public constructor(message: string, threshold: SeverityLevel) {
		super(message);
		this.threshold = threshold;
	}

	public getThreshold(): SeverityLevel {
		return this.threshold;
	}
}
