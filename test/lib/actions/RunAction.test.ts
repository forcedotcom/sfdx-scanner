import path from 'node:path';
import {SfError} from '@salesforce/core';
import {SeverityLevel} from '@salesforce/code-analyzer-core';
import {SpyResultsViewer} from '../../stubs/SpyResultsViewer';
import {SpyRunSummaryViewer} from '../../stubs/SpyRunSummaryViewer';
import {SpyResultsWriter} from '../../stubs/SpyResultsWriter';
import {StubDefaultConfigFactory} from '../../stubs/StubCodeAnalyzerConfigFactories';
import {ConfigurableStubEnginePlugin1, StubEngine1, TargetDependentEngine1} from '../../stubs/StubEnginePlugins';
import {RunAction, RunInput, RunDependencies} from '../../../src/lib/actions/RunAction';
import {
	StubEnginePluginsFactory_withPreconfiguredStubEngines,
	StubEnginePluginsFactory_withThrowingStubPlugin
} from '../../stubs/StubEnginePluginsFactories';

const PATH_TO_FILE_A = path.resolve('test', 'sample-code', 'fileA.cls');

describe('RunAction tests', () => {
	let engine1: StubEngine1;
	let stubEnginePlugin: ConfigurableStubEnginePlugin1;
	let pluginsFactory: StubEnginePluginsFactory_withPreconfiguredStubEngines;
	let writer: SpyResultsWriter;
	let resultsViewer: SpyResultsViewer;
	let runSummaryViewer: SpyRunSummaryViewer;
	let dependencies: RunDependencies;
	let action: RunAction;

	beforeEach(() => {
		// Set up the engine, plugin, and factory.
		engine1 = new StubEngine1({});
		stubEnginePlugin = new ConfigurableStubEnginePlugin1();
		stubEnginePlugin.addEngine(engine1);
		pluginsFactory = new StubEnginePluginsFactory_withPreconfiguredStubEngines();
		pluginsFactory.addPreconfiguredEnginePlugin(stubEnginePlugin);

		// Set up the writer and viewers.
		writer = new SpyResultsWriter();
		resultsViewer = new SpyResultsViewer();
		runSummaryViewer = new SpyRunSummaryViewer();

		// Initialize our dependency object.
		dependencies = {
			configFactory: new StubDefaultConfigFactory(),
			pluginsFactory: pluginsFactory,
			logEventListeners: [],
			progressListeners: [],
			writer,
			resultsViewer,
			runSummaryViewer
		};
		// Create the action.
		action = RunAction.createAction(dependencies);
	});

	it.each([
		{selector: 'all', expectedRules: ['stub1RuleA', 'stub1RuleB', 'stub1RuleC', 'stub1RuleD', 'stub1RuleE']},
		{selector: 'recommended', expectedRules: ['stub1RuleA', 'stub1RuleB', 'stub1RuleC']},
		{selector: 'security', expectedRules: ['stub1RuleB']}
	])('Accepts and runs rule selector: "$selector"', async ({selector, expectedRules}) => {
		// ==== SETUP ====
		// Create the input.
		const input: RunInput = {
			// Use the selector provided by the test
			'rule-selector': [selector],
			// Use the current directory, for convenience.
			'workspace': ['.'],
			// Outfiles can just be an empty list.
			'output-file': []
		};
		// Configure the engine to return a violation for the first expected rule.
		engine1.resultsToReturn = {
			violations: [{
				ruleName: expectedRules[0],
				message: 'Fake message',
				codeLocations: [{
					file: PATH_TO_FILE_A,
					startLine: 5,
					startColumn: 1
				}],
				primaryLocationIndex: 0
			}]
		};

		// ==== TESTED BEHAVIOR ====
		await action.execute(input);

		// ==== ASSERTIONS ====
		// Verify that the expected rules were executed on the right files.
		const actualExecutedRules = engine1.runRulesCallHistory[0].ruleNames;
		expect(actualExecutedRules).toEqual(expectedRules);
		const actualTargetFiles = engine1.runRulesCallHistory[0].runOptions.workspace.getFilesAndFolders();
		expect(actualTargetFiles).toEqual([path.resolve('.')]);
		// Verify that the expected results were passed into the Viewer and Writer.
		expect(writer.getCallHistory()[0].getViolationCount()).toEqual(1);
		expect(writer.getCallHistory()[0].getViolations()[0].getMessage()).toEqual('Fake message');
		expect(resultsViewer.getCallHistory()[0].getViolationCount()).toEqual(1);
		expect(resultsViewer.getCallHistory()[0].getViolations()[0].getMessage()).toEqual('Fake message');
		expect(runSummaryViewer.getCallHistory()[0].results.getViolationCount()).toEqual(1);
		expect(runSummaryViewer.getCallHistory()[0].results.getViolations()[0].getMessage()).toEqual('Fake message');
	});

	it('Engines with target-dependent rules run the right rules', async () => {
		// ==== SETUP ====
		// Add a target-dependent engine to the engines that will be run.
		const targetDependentEngine: TargetDependentEngine1 = new TargetDependentEngine1({});
		stubEnginePlugin.addEngine(targetDependentEngine);
		// Select a few specific targets instead of vacuously selecting the whole project.
		const targetedFilesAndFolders = ['package.json', 'src', 'README.md'];
		// Create the input
		const input: RunInput = {
			// Select only rules in the target-dependent engine.
			"rule-selector": [targetDependentEngine.getName()],
			"workspace": targetedFilesAndFolders,
			'output-file': []
		};

		// ==== TESTED BEHAVIOR ====
		await action.execute(input);

		// ==== ASSERTIONS ====
		// No rules in the shared stub engine should have been run.
		expect(engine1.runRulesCallHistory).toHaveLength(0);
		const actualExecutedRules = targetDependentEngine.runRulesCallHistory[0].ruleNames;
		// One rule per target should have been run in the target-dependent engine.
		expect(actualExecutedRules).toHaveLength(targetedFilesAndFolders.length);
		const expectedRuleNames = targetedFilesAndFolders.map(t => `ruleFor${path.resolve(t)}`);
		// The rules' order might not exactly match the provided targets', but as long as they're all present, it's fine.
		for (const expectedRuleName of expectedRuleNames) {
			expect(actualExecutedRules).toContain(expectedRuleName);
		}
	});

	it.each([
		{type: 'exceeded', sev: SeverityLevel.Moderate},
		{type: 'met', sev: SeverityLevel.High}
	])('When severity threshold is $type, an error is thrown', async ({sev}) => {
		// ==== SETUP ====
		// Create the input.
		const input: RunInput = {
			// Use the test-specific severity.
			'severity-threshold': sev,
			// Use 'all' to select all rules.
			'rule-selector': ['all'],
			// Use the current directory, for convenience.
			'workspace': ['.'],
			'output-file': []
		};
		// Configure the engine to return a violation for a rule with a known severity.
		engine1.resultsToReturn = {
			violations: [{
				ruleName: 'stub1RuleB',
				message: 'Fake message',
				codeLocations: [{
					file: PATH_TO_FILE_A,
					startLine: 5,
					startColumn: 1
				}],
				primaryLocationIndex: 0
			}]
		};

		// ==== TESTED BEHAVIOR ====
		// Typically we'd use Jest's `expect().toThrow()` method, but since we need to assert specific things
		// about the error, we're doing this instead.
		let thrownError: Error|null = null;
		try {
			await action.execute(input);
		} catch (e) {
			thrownError = e;
		}
		expect(thrownError).toBeInstanceOf(SfError);
		expect((thrownError as SfError).message).toContain(SeverityLevel[sev]);
		expect((thrownError as SfError).exitCode).toEqual(SeverityLevel.High);
	});

	it('When severity threshold is NOT met, an error is not thrown', async () => {
		// ==== SETUP ====
		// Create the input.
		const input: RunInput = {
			// Specify a severity HIGHER than that of the known rule.
			'severity-threshold': SeverityLevel.Critical,
			// Use 'all' to select all rules.
			'rule-selector': ['all'],
			// Use the current directory, for convenience.
			'workspace': ['.'],
			'output-file': []
		};
		// Configure the engine to return a violation for a rule with a known severity.
		engine1.resultsToReturn = {
			violations: [{
				ruleName: 'stub1RuleC',
				message: 'Fake message',
				codeLocations: [{
					file: PATH_TO_FILE_A,
					startLine: 5,
					startColumn: 1
				}],
				primaryLocationIndex: 0
			}]
		};

		// ==== TESTED BEHAVIOR ====
		const executionPromise = action.execute(input);

		// ==== ASSERTIONS ====
		await expect(executionPromise).resolves.not.toThrow();
	});

	it('Throws an error when an engine cannot be initialized', async () => {
		// ==== SETUP ====
		// Create our dependencies, intentionally using a different object than the one set up in
		// the before-each.
		const dependencies: RunDependencies = {
			configFactory: new StubDefaultConfigFactory(),
			// Use an engine plugin factory that returns an engine guaranteed to fail during initialization
			pluginsFactory: new StubEnginePluginsFactory_withThrowingStubPlugin(),
			logEventListeners: [],
			progressListeners: [],
			writer,
			resultsViewer,
			runSummaryViewer
		};
		// Instantiate our action, intentionally using a different instance than the one set up in
		// the before-each.
		const action = RunAction.createAction(dependencies);
		const input: RunInput = {
			'rule-selector': ['all'],
			'workspace': ['.'],
			'output-file': []
		};

		// ==== TESTED BEHAVIOR ====
		const executionPromise =  action.execute(input);

		// ==== ASSERTIONS ====
		await expect(executionPromise).rejects.toThrow('SomeErrorFromGetAvailableEngineNames');
	});
});

// TODO: Whenever we decide to document the custom_engine_plugin_modules flag in our configuration file, then we'll want
// to add in tests to lock in that behavior. But for now, it is a hidden utility for us to use internally, so no tests
// have been added.
