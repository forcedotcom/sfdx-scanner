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
		const names: string[] = engines.map(e => e.getName());

		expect(engines.length, names + '').to.equal(7);
		expect(names).to.contain(ENGINE.ESLINT);
		expect(names).to.contain(ENGINE.ESLINT_LWC);
		expect(names).to.contain(ENGINE.ESLINT_TYPESCRIPT);
		expect(names).to.contain(ENGINE.ESLINT_CUSTOM);
		expect(names).to.contain(ENGINE.PMD);
		expect(names).to.contain(ENGINE.PMD_CUSTOM);
		expect(names).to.contain(ENGINE.RETIRE_JS);
	});

	it('getEnabledEngines returns only non-custom enabled engines when engineOptions is empty', async() => {
		const engines: RuleEngine[] = await Controller.getEnabledEngines();
		const names: string[] = engines.map(e => e.getName());

		expect(engines.length).to.equal(3);
		expect(names).to.contain(ENGINE.ESLINT);
		expect(names).to.contain(ENGINE.ESLINT_TYPESCRIPT);
		expect(names).to.contain(ENGINE.PMD);
	});

	it('getEnabledEngines returns PMD_CUSTOM when engineOptions contains pmdconfig', async () => {
		const engineOptions = new Map<string, string>([
			[CUSTOM_CONFIG.PmdConfig, "/some/path"]
		]);

		const engines: RuleEngine[] = await Controller.getEnabledEngines(engineOptions);
		const names: string[] = engines.map(e => e.getName());

		expect(engines.length).to.equal(3);
		expect(names).to.contain(ENGINE.ESLINT);
		expect(names).to.contain(ENGINE.ESLINT_TYPESCRIPT);
		expect(names).to.contain(ENGINE.PMD_CUSTOM);
	});

	it('getEnabledEngines returns ESLINT_CUSTOM when engineOptions contains eslintconfig', async () => {
		const engineOptions = new Map<string, string>([
			[CUSTOM_CONFIG.EslintConfig, "/some/path"]
		]);

		const engines: RuleEngine[] = await Controller.getEnabledEngines(engineOptions);
		const names: string[] = engines.map(e => e.getName());

		expect(engines.length).to.equal(2);
		expect(names).to.contain(ENGINE.ESLINT_CUSTOM);
		expect(names).to.contain(ENGINE.PMD);
	});

	it('getEnabledEngines returns PMD_CUSTOM, ESLINT_CUSTOM when engineOptions contains pmdconfig and eslintconfig', async () => {
		const engineOptions = new Map<string, string>([
			[CUSTOM_CONFIG.EslintConfig, "/some/path"],
			[CUSTOM_CONFIG.PmdConfig, "/some/other/path"]
		]);

		const engines: RuleEngine[] = await Controller.getEnabledEngines(engineOptions);
		const names: string[] = engines.map(e => e.getName());

		expect(engines.length).to.equal(2);
		expect(names).to.contain(ENGINE.ESLINT_CUSTOM);
		expect(names).to.contain(ENGINE.PMD_CUSTOM);
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
			expect(e.message).to.equal('No engines are currently enabled. Currently disabled engines: fake-engine');
		}
	});

	it('getFilteredEngines throws exception when no engines are found', async() => {
		try {
			await Controller.getFilteredEngines(['invalid-engine']);
			fail('getFilteredEngines should have thrown');
		} catch (e) {
			expect(e.message).to.equal(`No engines meet the given filter. Filter: 'invalid-engine', Engines not matching filter: eslint, eslint-lwc, eslint-typescript, pmd, retire-js`);
		}
	});
});
