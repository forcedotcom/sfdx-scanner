import {Controller} from '../../src/Controller';
import {RuleEngine} from '../../src/lib/services/RuleEngine';
import * as TestOverrides from '../test-related-lib/TestOverrides';
import {expect}  from 'chai';
import {ENGINE} from '../../src/Constants';

describe('Controller.ts tests', () => {
	beforeEach(() => {
		TestOverrides.initializeTestSetup();
	});

	it('getAllEngines returns enabled/disabled engines', async() => {
		const engines: RuleEngine[] = await Controller.getAllEngines();
		const names: string[] = engines.map(e => e.getName());

		expect(engines.length, names + '').to.equal(4);
		expect(names).to.contain(ENGINE.ESLINT);
		expect(names).to.contain(ENGINE.ESLINT_LWC);
		expect(names).to.contain(ENGINE.ESLINT_TYPESCRIPT);
		expect(names).to.contain(ENGINE.PMD);
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
});
