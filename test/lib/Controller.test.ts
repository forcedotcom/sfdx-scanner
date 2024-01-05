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

	describe('#getAllEngines()', () => {
		it('Returns literally all engines', async () => {
			const engines: RuleEngine[] = await Controller.getAllEngines();
			const names: string[] = engines.map(e => e.constructor.name);

			expect(engines.length, names + '').to.equal(11);
			expect(names).to.contain('JavascriptEslintEngine');
			expect(names).to.contain('LWCEslintEngine');
			expect(names).to.contain('TypescriptEslintEngine');
			expect(names).to.contain('CustomEslintEngine');
			expect(names).to.contain('PmdEngine');
			expect(names).to.contain('AppExchangePmdEngine');
			expect(names).to.contain('CustomPmdEngine');
			expect(names).to.contain('RetireJsEngine');
			expect(names).to.contain('CpdEngine');
			expect(names).to.contain('SfgeDfaEngine');
			expect(names).to.contain('SfgePathlessEngine');
		});
	});

	describe('#getEnabledEngines()', () => {
		it('When engineOptions is empty, returns only engines that are non-custom, enabled, and requested-by-default', async () => {
			const engines: RuleEngine[] = await Controller.getEnabledEngines();
			const names: string[] = engines.map(e => e.constructor.name);

			expect(engines.length).to.equal(5);
			expect(names).to.contain('JavascriptEslintEngine');
			expect(names).to.contain('TypescriptEslintEngine');
			expect(names).to.contain('PmdEngine');
			expect(names).to.contain('RetireJsEngine');
			expect(names).to.contain('SfgeDfaEngine');
		});

		it('When engineOptions includes custom pmd config, PmdCustomEngine is included', async () => {
			const engineOptions = new Map<string, string>([
				[CUSTOM_CONFIG.PmdConfig, "/some/path"]
			]);

			const engines: RuleEngine[] = await Controller.getEnabledEngines(engineOptions);
			const names: string[] = engines.map(e => e.constructor.name);

			expect(engines.length).to.equal(5);
			expect(names).to.contain('JavascriptEslintEngine');
			expect(names).to.contain('TypescriptEslintEngine');
			expect(names).to.contain('CustomPmdEngine');
			expect(names).to.contain('RetireJsEngine');
			expect(names).to.contain('SfgeDfaEngine');
		});

		it('When engineOptions includes custom eslint config, CustomEslintEngine is included', async () => {
			const engineOptions = new Map<string, string>([
				[CUSTOM_CONFIG.EslintConfig, "/some/path"]
			]);

			const engines: RuleEngine[] = await Controller.getEnabledEngines(engineOptions);
			const names: string[] = engines.map(e => e.constructor.name);

			expect(engines.length).to.equal(4);
			expect(names).to.contain('CustomEslintEngine');
			expect(names).to.contain('PmdEngine');
			expect(names).to.contain('RetireJsEngine');
			expect(names).to.contain('SfgeDfaEngine');
		});

		it('When engineOptions includes both custom pmd and custom eslint configs, both custom engines are included', async () => {
			const engineOptions = new Map<string, string>([
				[CUSTOM_CONFIG.EslintConfig, "/some/path"],
				[CUSTOM_CONFIG.PmdConfig, "/some/other/path"]
			]);

			const engines: RuleEngine[] = await Controller.getEnabledEngines(engineOptions);
			const names: string[] = engines.map(e => e.constructor.name);

			expect(engines.length).to.equal(4);
			expect(names).to.contain('CustomEslintEngine');
			expect(names).to.contain('CustomPmdEngine');
			expect(names).to.contain('RetireJsEngine');
			expect(names).to.contain('SfgeDfaEngine');
		});

		it('When no engines are found, error is thrown', async () => {
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
	});

	describe('#getFilteredEngines()', () => {
		it('If no filtering is provided, only requested-by-default engines are returned', async () => {
			const engines: RuleEngine[] = await Controller.getFilteredEngines([]);
			const names: string[] = engines.map(e => e.constructor.name);

			expect(engines.length).to.equal(7);
			expect(names).to.contain('JavascriptEslintEngine');
			expect(names).to.contain('LWCEslintEngine');
			expect(names).to.contain('TypescriptEslintEngine');
			expect(names).to.contain('PmdEngine');
			expect(names).to.contain('AppExchangePmdEngine');
			expect(names).to.contain('RetireJsEngine');
			expect(names).to.contain('SfgeDfaEngine');
		})

		it('Even a disabled engine is included when explicitly requested', async () => {
			const engines: RuleEngine[] = await Controller.getFilteredEngines([ENGINE.ESLINT, ENGINE.ESLINT_LWC, ENGINE.PMD]);
			const names: string[] = engines.map(e => e.getName());

			expect(engines.length).to.equal(3);
			expect(names).to.contain(ENGINE.ESLINT);
			expect(names).to.contain(ENGINE.ESLINT_LWC);
			expect(names).to.contain(ENGINE.PMD);
		});

		it('When custom config information is provided, the correct instance is returned', async () => {
			const engineOptionsWithPmdCustom = new Map<string, string>([
				[CUSTOM_CONFIG.PmdConfig, '/some/path/to/config']
			]);
			const engines: RuleEngine[] = await Controller.getFilteredEngines([ENGINE.PMD], engineOptionsWithPmdCustom);
			const names: string[] = engines.map(e => e.getName());

			expect(engines.length).to.equal(1);
			expect(names).to.contain(ENGINE.PMD_CUSTOM);
		});

		it('When no engines are found, exception is thrown', async () => {
			try {
				await Controller.getFilteredEngines(['invalid-engine']);
				fail('getFilteredEngines should have thrown');
			} catch (e) {
				expect(e.message).to.equal(`The filter doesn't match any engines. Filter 'invalid-engine'. Engines: cpd, eslint, eslint-lwc, eslint-typescript, pmd, pmd-appexchange, retire-js, sfge.`);
			}
		});
	});
});
