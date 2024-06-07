import {RulesAction, RulesDependencies} from '../../../src/lib/actions/RulesAction';
import {StubDefaultConfigLoader} from '../../stubs/StubConfigLoader';
import * as StubEngineLoaders from '../../stubs/StubEngineLoaders';
import {StubRuleViewer} from '../../stubs/StubRuleViewer';

describe('RulesAction tests', () => {

	it('When rules match selectors, they are returned', () => {
		const action = new RulesAction();
		const viewer = new StubRuleViewer();
		const dependencies: RulesDependencies = {
			configLoader: new StubDefaultConfigLoader(),
			engineLoader: new StubEngineLoaders.StubEngineLoader_withFunctionalStubEngine(),
			viewer
		};
		const input = {
			'rule-selector': ['all']
		};

		action.execute(dependencies, input);

		viewer.expectViewedRules([
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

	/**
	 * This behavior was not formally defined, and isn't actually possible at the moment due to
	 * hard-coded engines. But in the future we may want to have dynamic engine loading, and this
	 * test will help us do that.
	 */
	it('When no engines are registered, empty results are displayed', () => {
		const action = new RulesAction();
		const viewer = new StubRuleViewer();
		const dependencies: RulesDependencies = {
			configLoader: new StubDefaultConfigLoader(),
			engineLoader: new StubEngineLoaders.StubEngineLoader_withNoPlugins(),
			viewer
		};
		const input = {
			'rule-selector': ['all']
		}

		action.execute(dependencies, input);

		viewer.expectViewedRules([]);
	});

	it('Throws an error when an engine throws an error', () => {
		const action = new RulesAction();
		const viewer = new StubRuleViewer();
		const dependencies: RulesDependencies = {
			configLoader: new StubDefaultConfigLoader(),
			engineLoader: new StubEngineLoaders.StubEngineLoader_withThrowingStubPlugin(),
			viewer
		};
		const input = {
			'rule-selector': ['all']
		};

		expect(() => action.execute(dependencies, input)).toThrow('SomeErrorFromGetAvailableEngineNames');
	});

	it('Throws an error when selectors are invalid', () => {

	});
});
