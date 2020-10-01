import "reflect-metadata";
import {container} from "tsyringe";
import {Controller} from '../../src/Controller';
import {RuleEngine} from '../../src/lib/services/RuleEngine';
import * as TestOverrides from '../test-related-lib/TestOverrides';
import {ENGINE, Services} from '../../src/Constants';
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

		expect(engines.length, names + '').to.equal(5);
		expect(names).to.contain(ENGINE.ESLINT);
		expect(names).to.contain(ENGINE.ESLINT_LWC);
		expect(names).to.contain(ENGINE.ESLINT_TYPESCRIPT);
		expect(names).to.contain(ENGINE.PMD);
		expect(names).to.contain(ENGINE.RETIRE_JS);
	});

	it('getEnabledEngines returns only enabled engines', async() => {
		const engines: RuleEngine[] = await Controller.getEnabledEngines();
		const names: string[] = engines.map(e => e.getName());

		expect(engines.length).to.equal(3);
		expect(names).to.contain(ENGINE.ESLINT);
		expect(names).to.contain(ENGINE.ESLINT_TYPESCRIPT);
		expect(names).to.contain(ENGINE.PMD);
	});

	it('getFilteredEngines filters and includes disabled', async() => {
		const engines: RuleEngine[] = await Controller.getFilteredEngines([ENGINE.ESLINT, ENGINE.ESLINT_LWC, ENGINE.PMD]);
		const names: string[] = engines.map(e => e.getName());

		expect(engines.length).to.equal(3);
		expect(names).to.contain(ENGINE.ESLINT);
		expect(names).to.contain(ENGINE.ESLINT_LWC);
		expect(names).to.contain(ENGINE.PMD);
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
