import * as path from 'node:path';
import * as fsp from 'node:fs/promises';
import ansis from 'ansis';
import {RulesAction, RulesDependencies, RulesInput} from '../../../src/lib/actions/RulesAction';
import {RulesActionSummaryViewer} from '../../../src/lib/viewers/ActionSummaryViewer';
import {StubDefaultConfigFactory} from '../../stubs/StubCodeAnalyzerConfigFactories';
import * as StubEnginePluginFactories from '../../stubs/StubEnginePluginsFactories';
import {SpyRuleViewer} from '../../stubs/SpyRuleViewer';
import {DisplayEventType, SpyDisplay} from '../../stubs/SpyDisplay';

const PATH_TO_GOLDFILES = path.join(__dirname, '..', '..', 'fixtures', 'comparison-files', 'lib', 'actions', 'RulesAction.test.ts');

describe('RulesAction tests', () => {
	let viewer: SpyRuleViewer;

	beforeEach(() => {
		viewer = new SpyRuleViewer();
	})

	it('Submitting the all-selector returns all rules', async () => {
		const spyDisplay: SpyDisplay = new SpyDisplay();
		const actionSummaryViewer: RulesActionSummaryViewer = new RulesActionSummaryViewer(spyDisplay);
		const dependencies: RulesDependencies = {
			configFactory: new StubDefaultConfigFactory(),
			pluginsFactory: new StubEnginePluginFactories.StubEnginePluginsFactory_withFunctionalStubEngine(),
			logEventListeners: [],
			progressListeners: [],
			actionSummaryViewer,
			viewer
		};
		const action = RulesAction.createAction(dependencies);
		const input: RulesInput = {
			'rule-selector': ['all']
		};

		await action.execute(input);

		const viewerCallHistory = viewer.getCallHistory();
		expect(viewerCallHistory).toHaveLength(1);
		expect(viewerCallHistory[0].map(rule => rule.getName())).toEqual([
			'stub1RuleA',
			'stub1RuleB',
			'stub1RuleC',
			'stub1RuleD',
			'stub1RuleE',
			'stub2RuleA',
			'stub2RuleB',
			'stub2RuleC'
		]);
	});

	it('Submitting a filtering selector returns only matching rules', async () => {
		const spyDisplay: SpyDisplay = new SpyDisplay();
		const actionSummaryViewer: RulesActionSummaryViewer = new RulesActionSummaryViewer(spyDisplay);
		const dependencies: RulesDependencies = {
			configFactory: new StubDefaultConfigFactory(),
			pluginsFactory: new StubEnginePluginFactories.StubEnginePluginsFactory_withFunctionalStubEngine(),
			logEventListeners: [],
			progressListeners: [],
			actionSummaryViewer,
			viewer
		};
		const action = RulesAction.createAction(dependencies);
		const input: RulesInput = {
			'rule-selector': ['CodeStyle']
		};

		await action.execute(input);

		const viewerCallHistory = viewer.getCallHistory();
		expect(viewerCallHistory).toHaveLength(1);
		expect(viewerCallHistory[0].map(rule => rule.getName())).toEqual([
			'stub1RuleA',
			'stub1RuleD'
		]);
	});

	it('Engines with target-dependent rules return the right rules', async () => {
		const spyDisplay: SpyDisplay = new SpyDisplay();
		const actionSummaryViewer: RulesActionSummaryViewer = new RulesActionSummaryViewer(spyDisplay);
		const dependencies: RulesDependencies = {
			configFactory: new StubDefaultConfigFactory(),
			// The engine we're using here will synthesize one rule per target.
			pluginsFactory: new StubEnginePluginFactories.StubEnginePluginsFactory_withTargetDependentStubEngine(),
			logEventListeners: [],
			progressListeners: [],
			actionSummaryViewer,
			viewer
		};
		const targetedFilesAndFolders = ['package.json', 'src', 'README.md'];
		const action = RulesAction.createAction(dependencies);
		const input: RulesInput = {
			'rule-selector': ['Recommended'],
			workspace: targetedFilesAndFolders
		};

		await action.execute(input);

		const viewerCallHistory = viewer.getCallHistory();
		expect(viewerCallHistory).toHaveLength(1);
		const expectedRuleNames = targetedFilesAndFolders.map(t => `ruleFor${path.resolve(t)}`);
		const actualRuleNames = viewerCallHistory[0].map(rule => rule.getName());
		expect(actualRuleNames).toHaveLength(expectedRuleNames.length);
		// The rules' order might not exactly match the provided targets', but as long as they're all present, it's fine.
		for (const expectedRuleName of expectedRuleNames) {
			expect(actualRuleNames).toContain(expectedRuleName);
		}
	});

	/**
	 * This behavior was not formally defined, and isn't actually possible at the moment due to
	 * hard-coded engines. But in the future we may want to have dynamic engine loading, and this
	 * test will help us do that.
	 */
	it('When no engines are registered, empty results are displayed', async () => {
		const spyDisplay: SpyDisplay = new SpyDisplay();
		const actionSummaryViewer: RulesActionSummaryViewer = new RulesActionSummaryViewer(spyDisplay);
		const dependencies: RulesDependencies = {
			configFactory: new StubDefaultConfigFactory(),
			pluginsFactory: new StubEnginePluginFactories.StubEnginePluginsFactory_withNoPlugins(),
			logEventListeners: [],
			progressListeners: [],
			actionSummaryViewer,
			viewer
		};
		const action = RulesAction.createAction(dependencies);
		const input: RulesInput = {
			'rule-selector': ['all']
		};

		await action.execute(input);

		const viewerCallHistory = viewer.getCallHistory();
		expect(viewerCallHistory).toHaveLength(1);
		expect(viewerCallHistory[0]).toEqual([]);
	});

	it('Throws an error when an engine throws an error', async () => {
		const spyDisplay: SpyDisplay = new SpyDisplay();
		const actionSummaryViewer: RulesActionSummaryViewer = new RulesActionSummaryViewer(spyDisplay);
		const dependencies: RulesDependencies = {
			configFactory: new StubDefaultConfigFactory(),
			pluginsFactory: new StubEnginePluginFactories.StubEnginePluginsFactory_withThrowingStubPlugin(),
			logEventListeners: [],
			progressListeners: [],
			actionSummaryViewer,
			viewer
		};
		const action = RulesAction.createAction(dependencies);
		const input: RulesInput = {
			'rule-selector': ['all']
		};
		const executionPromise = action.execute(input);

		await expect(executionPromise).rejects.toThrow('SomeErrorFromGetAvailableEngineNames');
	});

	describe('Summary generation', () => {
		it.each([
			{quantifier: 'no', expectation: 'Summary indicates absence of rules', selector: 'NonsensicalTag', goldfile: 'no-rules.txt.goldfile'},
			{quantifier: 'some', expectation: 'Summary provides breakdown by engine', selector: 'Recommended', goldfile: 'some-rules.txt.goldfile'}
		])('When $quantifier rules are returned, $expectation', async ({selector, goldfile}) => {
			const preExecutionGoldfilePath: string = path.join(PATH_TO_GOLDFILES, 'action-summaries', 'pre-execution-summary.txt.goldfile');
			const goldfilePath: string = path.join(PATH_TO_GOLDFILES, 'action-summaries', goldfile);
			const spyDisplay: SpyDisplay = new SpyDisplay();
			const actionSummaryViewer: RulesActionSummaryViewer = new RulesActionSummaryViewer(spyDisplay);
			const dependencies: RulesDependencies = {
				configFactory: new StubDefaultConfigFactory(),
				pluginsFactory: new StubEnginePluginFactories.StubEnginePluginsFactory_withFunctionalStubEngine(),
				logEventListeners: [],
				progressListeners: [],
				actionSummaryViewer,
				viewer
			};
			const action = RulesAction.createAction(dependencies);
			const input: RulesInput = {
				'rule-selector': [selector]
			};

			await action.execute(input);

			const displayEvents = spyDisplay.getDisplayEvents();
			const displayedLogEvents = ansis.strip(displayEvents
				.filter(e => e.type === DisplayEventType.LOG)
				.map(e => e.data)
				.join('\n'));
			const preExecutionGoldfileContents: string = await fsp.readFile(preExecutionGoldfilePath, 'utf-8');
			expect(displayedLogEvents).toContain(preExecutionGoldfileContents);

			const goldfileContents: string = await fsp.readFile(goldfilePath, 'utf-8');
			expect(displayedLogEvents).toContain(goldfileContents);
		});
	});
});

// TODO: Whenever we decide to document the custom_engine_plugin_modules flag in our configuration file, then we'll want
// to add in tests to lock in that behavior. But for now, it is a hidden utility for us to use internally, so no tests
// have been added.
