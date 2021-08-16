import {expect} from 'chai';
import {CpdEngine} from '../../../src/lib/cpd/CpdEngine'
import * as TestOverrides from '../../test-related-lib/TestOverrides';
import { CUSTOM_CONFIG, LANGUAGE } from '../../../src/Constants';
import { RuleTarget } from '../../../src/types';

TestOverrides.initializeTestSetup();

describe('CpdEngine', () => {

	const testEngine: CpdEngine = new CpdEngine();
    
	before(async () => {
		await testEngine.init();
	});
    
	describe('shouldEngineRun()', () => {
		it('should always return true if the engine was not filtered out', () => {
			expect(testEngine.shouldEngineRun([],[],[],new Map<string,string>())).to.be.true;
		});
	});

	describe('sortPaths()', () => {
		it('should return the correct map based on valid cpd files and targets given', () => {
			const targets: RuleTarget[] = [{
				target: '',
				paths: ['file.cls','file.trigger','file.page','file.py','file.component']
			}];
		
			let langPathMap: Map<LANGUAGE, string[]> = new Map();
			langPathMap.set(LANGUAGE.APEX, ['file.cls','file.trigger']);
			langPathMap.set(LANGUAGE.VISUALFORCE, ['file.page','file.component']);
			expect((testEngine as any).sortPaths(targets)).to.eql(langPathMap);
		});

		it('should compare extensions case-insensitively', () => {
			const targets: RuleTarget[] = [{
				target: '',
				paths: ['file.Cls','file.tRIGGER']
			}];
		
			let langPathMap: Map<LANGUAGE, string[]> = new Map();
			langPathMap.set(LANGUAGE.APEX, ['file.Cls','file.tRIGGER']);
			expect((testEngine as any).sortPaths(targets)).to.eql(langPathMap);
		});
	});

	
	describe('isEngineRequested()', () => {
		const emptyEngineOptions = new Map<string, string>();

		const configFilePath = '/some/file/path/config.json';
		const engineOptionsWithEslintCustom = new Map<string, string>([
			[CUSTOM_CONFIG.EslintConfig, configFilePath]
		]);
		const engineOptionsWithPmdCustom = new Map<string, string>([
			[CUSTOM_CONFIG.PmdConfig, configFilePath]
		]);

		it('should return true if filter contains "cpd" and engineOptions map is empty', () => {
			const filterValues = ['cpd', 'pmd'];

			const isEngineRequested = testEngine.isEngineRequested(filterValues, emptyEngineOptions);

			expect(isEngineRequested).to.be.true;
		});

		it('should return true if filter contains "cpd" and engineOptions map contains eslint config', () => {
			const filterValues = ['cpd', 'eslint'];

			const isEngineRequested = testEngine.isEngineRequested(filterValues, engineOptionsWithEslintCustom);

			expect(isEngineRequested).to.be.true;
		});

		it('should return true if filter contains "cpd" and engineOptions map contains pmd config', () => {
			const filterValues = ['cpd', 'pmd'];

			const isEngineRequested = testEngine.isEngineRequested(filterValues, engineOptionsWithPmdCustom);

			expect(isEngineRequested).to.be.true;
		});

		it('should return false if filter does not contain "cpd" irrespective of engineOptions', () => {
			const filterValues = ['retire-js', 'pmd'];

			const isEngineRequested = testEngine.isEngineRequested(filterValues, emptyEngineOptions);

			expect(isEngineRequested).to.be.false;
		});
	});

    
});
