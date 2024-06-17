import path from 'node:path';
import {SeverityLevel} from '@salesforce/code-analyzer-core';
import {SpyResultsViewer} from '../../stubs/SpyResultsViewer';
import {SpyOutputFileWriter} from '../../stubs/SpyOutputFileWriter';
import {StubDefaultConfigFactory} from '../../stubs/StubCodeAnalyzerConfigFactories';
import {ConfigurableStubEnginePlugin1, StubEngine1} from '../../stubs/StubEnginePlugins';
import {RunAction, RunInput, RunDependencies} from '../../../src/lib/actions/RunAction';
import {
	StubEnginePluginFactory_withPreconfiguredStubEngines,
	StubEnginePluginFactory_withThrowingStubPlugin
} from '../../stubs/StubEnginePluginFactories';

const PATH_TO_FILE_A = path.resolve('test', 'sample-code', 'fileA.cls');

describe('RunAction tests', () => {
	let engine1: StubEngine1;
	let stubEnginePlugin: ConfigurableStubEnginePlugin1;
	let engineFactory: StubEnginePluginFactory_withPreconfiguredStubEngines;
	let outputFileWriter: SpyOutputFileWriter;
	let viewer: SpyResultsViewer;
	let dependencies: RunDependencies;
	let action: RunAction;

	beforeEach(() => {
		// Set up the engine, plugin, and factory.
		engine1 = new StubEngine1({});
		stubEnginePlugin = new ConfigurableStubEnginePlugin1();
		stubEnginePlugin.addEngine(engine1);
		engineFactory = new StubEnginePluginFactory_withPreconfiguredStubEngines();
		engineFactory.addPreconfiguredEnginePlugin(stubEnginePlugin);

		// Set up the writer and viewer.
		outputFileWriter = new SpyOutputFileWriter();
		viewer = new SpyResultsViewer();

		// Initialize our dependency object.
		dependencies = {
			configFactory: new StubDefaultConfigFactory(),
			engineFactory,
			outputFileWriter,
			viewer
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
			'workspace': ['.']
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
		const actualTargetFiles = engine1.runRulesCallHistory[0].runOptions.workspaceFiles;
		expect(actualTargetFiles).toEqual([path.resolve('.')]);
		// Verify that the expected results were passed into the Viewer and Writer.
		expect(outputFileWriter.getCallHistory()[0].getViolationCount()).toEqual(1);
		expect(outputFileWriter.getCallHistory()[0].getViolations()[0].getMessage()).toEqual('Fake message');
		expect(viewer.getCallHistory()[0].getViolationCount()).toEqual(1);
		expect(viewer.getCallHistory()[0].getViolations()[0].getMessage()).toEqual('Fake message');
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
			'workspace': ['.']
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
		const executionPromise = action.execute(input);

		// ==== ASSERTIONS ====
		await expect(executionPromise).rejects.toThrow(SeverityLevel[sev]);
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
			'workspace': ['.']
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
			engineFactory: new StubEnginePluginFactory_withThrowingStubPlugin(),
			outputFileWriter,
			viewer
		};
		// Instantiate our action, intentionally using a different instance than the one set up in
		// the before-each.
		const action = RunAction.createAction(dependencies);
		const input: RunInput = {
			'rule-selector': ['all'],
			'workspace': ['.']
		};

		// ==== TESTED BEHAVIOR ====
		const executionPromise =  action.execute(input);

		// ==== ASSERTIONS ====
		await expect(executionPromise).rejects.toThrow('SomeErrorFromGetAvailableEngineNames');
	});
});
