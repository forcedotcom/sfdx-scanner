import "reflect-metadata";
import {container} from "tsyringe";
import {Controller} from '../../src/Controller';
import {RuleEngine} from '../../src/lib/services/RuleEngine';
import * as TestOverrides from '../test-related-lib/TestOverrides';
import {CUSTOM_CONFIG, ENGINE, Services} from '../../src/Constants';
import {expect}  from 'chai';
import {fail} from 'assert';
import {instance, mock, when} from 'ts-mockito';

describe('Controller.ts tests', () => {
	beforeEach(() => {
		TestOverrides.initializeTestSetup();
	});

	it('getAllEngines returns enabled/disabled engines', async() => {
		const engines: RuleEngine[] = await Controller.getAllEngines();
		const names: string[] = engines.map(e => e.constructor.name);

		expect(engines.length, names + '').to.equal(10);
		expect(names).to.contain('JavascriptEslintEngine');
		expect(names).to.contain('LWCEslintEngine');
		expect(names).to.contain('TypescriptEslintEngine');
		expect(names).to.contain('CustomEslintEngine');
		expect(names).to.contain('PmdEngine');
		expect(names).to.contain('CustomPmdEngine');
		expect(names).to.contain('RetireJsEngine');
		expect(names).to.contain('CpdEngine');
		expect(names).to.contain('SfgeDfaEngine');
		expect(names).to.contain('SfgePathlessEngine');
	});

	it('getEnabledEngines returns only non-custom enabled engines when engineOptions is empty', async() => {
		const engines: RuleEngine[] = await Controller.getEnabledEngines();
		const names: string[] = engines.map(e => e.constructor.name);

		expect(engines.length).to.equal(6);
		expect(names).to.contain('JavascriptEslintEngine');
		expect(names).to.contain('TypescriptEslintEngine');
		expect(names).to.contain('PmdEngine');
		expect(names).to.contain('RetireJsEngine');
		expect(names).to.contain('SfgeDfaEngine');
		expect(names).to.contain('SfgePathlessEngine');
	});

	it('getEnabledEngines returns PMD_CUSTOM when engineOptions contains pmdconfig', async () => {
		const engineOptions = new Map<string, string>([
			[CUSTOM_CONFIG.PmdConfig, "/some/path"]
		]);

		const engines: RuleEngine[] = await Controller.getEnabledEngines(engineOptions);
		const names: string[] = engines.map(e => e.constructor.name);

		expect(engines.length).to.equal(6);
		expect(names).to.contain('JavascriptEslintEngine');
		expect(names).to.contain('TypescriptEslintEngine');
		expect(names).to.contain('CustomPmdEngine');
		expect(names).to.contain('RetireJsEngine');
		expect(names).to.contain('SfgeDfaEngine');
		expect(names).to.contain('SfgePathlessEngine');
	});

	it('getEnabledEngines returns ESLINT_CUSTOM when engineOptions contains eslintconfig', async () => {
		const engineOptions = new Map<string, string>([
			[CUSTOM_CONFIG.EslintConfig, "/some/path"]
		]);

		const engines: RuleEngine[] = await Controller.getEnabledEngines(engineOptions);
		const names: string[] = engines.map(e => e.constructor.name);

		expect(engines.length).to.equal(5);
		expect(names).to.contain('CustomEslintEngine');
		expect(names).to.contain('PmdEngine');
		expect(names).to.contain('RetireJsEngine');
		expect(names).to.contain('SfgeDfaEngine');
		expect(names).to.contain('SfgePathlessEngine');
	});

	it('getEnabledEngines returns PMD_CUSTOM, ESLINT_CUSTOM when engineOptions contains pmdconfig and eslintconfig', async () => {
		const engineOptions = new Map<string, string>([
			[CUSTOM_CONFIG.EslintConfig, "/some/path"],
			[CUSTOM_CONFIG.PmdConfig, "/some/other/path"]
		]);

		const engines: RuleEngine[] = await Controller.getEnabledEngines(engineOptions);
		const names: string[] = engines.map(e => e.constructor.name);

		expect(engines.length).to.equal(5);
		expect(names).to.contain('CustomEslintEngine');
		expect(names).to.contain('CustomPmdEngine');
		expect(names).to.contain('RetireJsEngine');
		expect(names).to.contain('SfgeDfaEngine');
		expect(names).to.contain('SfgePathlessEngine');
	});

	it('getFilteredEngines filters and includes disabled', async() => {
		const engines: RuleEngine[] = await Controller.getFilteredEngines([ENGINE.ESLINT, ENGINE.ESLINT_LWC, ENGINE.PMD]);
		const names: string[] = engines.map(e => e.getName());

		expect(engines.length).to.equal(3);
		expect(names).to.contain(ENGINE.ESLINT);
		expect(names).to.contain(ENGINE.ESLINT_LWC);
		expect(names).to.contain(ENGINE.PMD);
	});

	it('getFilteredEngines uses custom config information to choose the correct instance', async() => {
		const engineOptionsWithPmdCustom = new Map<string, string>([
			[CUSTOM_CONFIG.PmdConfig, '/some/path/to/config']
		]);
		const engines: RuleEngine[] = await Controller.getFilteredEngines([ENGINE.PMD], engineOptionsWithPmdCustom);
		const names: string[] = engines.map(e => e.getName());

		expect(engines.length).to.equal(1);
		expect(names).to.contain(ENGINE.PMD_CUSTOM);
	});

	it('getEnabledEngines throws exception when no engines are found', async() => {
		// Create a single mocked engine that is disabled
		const mockedRuleEngine: RuleEngine = mock<RuleEngine>();
		when(mockedRuleEngine.getName).thenReturn(() => 'fake-engine');
		when(mockedRuleEngine.isEnabled).thenReturn(() => Promise.resolve(false));
		const ruleEngine: RuleEngine = instance(mockedRuleEngine);

		// Remove everything else from the container and register the mock engine
		container.reset();
		container.registerInstance(Services.RuleEngine, ruleEngine);

		try {
			await Controller.getEnabledEngines();
			fail('getEnabledEngines should have thrown');
		} catch (e) {
			expect(e.message).to.equal('You must enable at least one engine. Your currently disabled engines are: fake-engine.');
		}
	});

	it('getFilteredEngines throws exception when no engines are found', async() => {
		try {
			await Controller.getFilteredEngines(['invalid-engine']);
			fail('getFilteredEngines should have thrown');
		} catch (e) {
			expect(e.message).to.equal(`The filter doesn't match any engines. Filter 'invalid-engine'. Engines: cpd, eslint, eslint-lwc, eslint-typescript, pmd, retire-js, sfge.`);
		}
	});
});
