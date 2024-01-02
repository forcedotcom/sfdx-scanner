import 'reflect-metadata';
import {expect} from 'chai';
import {SfgeConfig} from '../../../src/types';
import {CUSTOM_CONFIG} from '../../../src/Constants';
import {SfgePathlessEngine} from '../../../src/lib/sfge/SfgePathlessEngine';
import * as TestOverrides from '../../test-related-lib/TestOverrides';
import {BundleName, getMessage} from "../../../src/MessageCatalog";

TestOverrides.initializeTestSetup();

describe('SfgePathlessEngine', () => {
	describe('#isEngineRequested()', () => {
		it('Pathless SFGE counts as requested when explicitly requested', async () => {
			// ==== SETUP ====
			const engine = new SfgePathlessEngine();
			await engine.init();
			const filteredNames = ['sfge', 'retire-js'];
			// ==== TESTED METHOD ====
			const isEngineRequested = engine.isEngineRequested(filteredNames, new Map());
			// ==== ASSERTIONS ====
			expect(isEngineRequested).to.be.true;
		});

		// NOTE: This behavior is based on the in-progress nature of many of this engine's rules.
		//       If/when we are confident enough to make this a requested-by-default engine, this test must change.
		it('Pathless SFGE does not count as requested-by-default', async () => {
			// ==== SETUP ====
			const engine = new SfgePathlessEngine();
			await engine.init();
			// An empty array means that no engines were explicitly requested.
			const filteredNames = [];
			// ==== TESTED METHOD ====
			const isEngineRequested = engine.isEngineRequested(filteredNames, new Map());
			// ==== ASSERTIONS ====
			// Since there were no explicitly requested engines, this engine
			// should be excluded, since it's not requested-by-default.
			expect(isEngineRequested).to.be.false;
		});

		it('Pathless SFGE does not count as requested when explicitly not requested', async () => {
			// ==== SETUP ====
			const engine = new SfgePathlessEngine();
			await engine.init();
			const filteredNames = ['pmd', 'retire-js'];
			// ==== TESTED METHOD ====
			const isEngineRequested = engine.isEngineRequested(filteredNames, new Map());
			// ==== ASSERTIONS ====
			// Since we requested specific engines, and sfge isn't one of them,
			// the engine should be excluded.
			expect(isEngineRequested).to.be.false;
		});
	});

	describe('#shouldEngineRun()', () => {
		it('Returns true when SfgeConfig has non-empty projectdirs array', async () => {
			// ==== SETUP ====
			const engine = new SfgePathlessEngine();
			await engine.init();
			const sfgeConfig: SfgeConfig = {
				projectDirs: ['specific/value/is/irrelevant']
			};
			const engineOptions: Map<string,string> = new Map();
			engineOptions.set(CUSTOM_CONFIG.SfgeConfig, JSON.stringify(sfgeConfig));
			// ==== TESTED METHOD ====
			// The only parameter that matters should be the engine options.
			const shouldEngineRun = engine.shouldEngineRun([], [], [], engineOptions);
			// ==== ASSERTIONS ====
			expect(shouldEngineRun).to.be.true;
		});

		it('Throws error when SfgeConfig has empty projectdirs array', async () => {
			// ==== SETUP ====
			const engine = new SfgePathlessEngine();
			await engine.init();
			const sfgeConfig: SfgeConfig = {
				projectDirs: []
			};
			const engineOptions: Map<string,string> = new Map();
			engineOptions.set(CUSTOM_CONFIG.SfgeConfig, JSON.stringify(sfgeConfig));
			// ==== TESTED METHOD ====
			const invocationOfShouldEngineRun = () => {
				// The only parameter that matters should be the engine options.
				return engine.shouldEngineRun([], [], [], engineOptions);
			};
			// ==== ASSERTIONS ====
			expect(invocationOfShouldEngineRun).to.throw(getMessage(BundleName.SfgeEngine, 'errors.failedWithoutProjectDir', []));
		});

		it('Throws error when SfgeConfig lacks projectdirs array', async () => {
			// ==== SETUP ====
			const engine = new SfgePathlessEngine();
			await engine.init();
			const engineOptions: Map<string,string> = new Map();
			engineOptions.set(CUSTOM_CONFIG.SfgeConfig, "{}");
			// ==== TESTED METHOD ====
			const invocationOfShouldEngineRun = () => {
				// The only parameter that matters should be the engine options.
				return engine.shouldEngineRun([], [], [], engineOptions);
			};
			// ==== ASSERTIONS ====
			expect(invocationOfShouldEngineRun).to.throw(getMessage(BundleName.SfgeEngine, 'errors.failedWithoutProjectDir', []));
		});

		it('Throws error when SfgeConfig is outright absent', async () => {
			// ==== SETUP ====
			const engine = new SfgePathlessEngine();
			await engine.init();
			// ==== TESTED METHOD ====
			const invocationOfShouldEngineRun = () => {
				// The only parameter that matters should be the engine options.
				return engine.shouldEngineRun([], [], [], new Map());
			};
			// ==== ASSERTIONS ====
			expect(invocationOfShouldEngineRun).to.throw(getMessage(BundleName.SfgeEngine, 'errors.failedWithoutProjectDir', []));
		});
	});
});
