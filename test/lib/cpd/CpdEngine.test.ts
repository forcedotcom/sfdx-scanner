import 'reflect-metadata';
import path = require('path');
import {expect} from 'chai';
import {CpdEngine} from '../../../src/lib/cpd/CpdEngine'
import * as TestOverrides from '../../test-related-lib/TestOverrides';
import {FileHandler} from '../../../src/lib/util/FileHandler';
import Sinon = require('sinon');
import { CUSTOM_CONFIG, LANGUAGE } from '../../../src/Constants';
import { RuleTarget } from '../../../src/types';

TestOverrides.initializeTestSetup();

describe('CpdEngine', () => {
	let testEngine: CpdEngine = new CpdEngine();
    
	before(async () => {
		Sinon.createSandbox();

		await testEngine.init();
	});
	after(() => {
		Sinon.restore();
	});
    
	describe('processStdOut()', () => {
		it('If a file has multiple duplications, they should appear as part of the same result', async () => {
			// This file contains a `file`-type node and a mix of other node types that are direct children of the `pmd`
			// node. The file nodes and the error nodes representing parser errors should be converted to violations.
			// Anything else should be filtered out of the results without problems.
			const xmlPath = path.join('test', 'code-fixtures', 'cpd-results', 'result-with-duplications.txt');
			const fileHandler: FileHandler = new FileHandler();
			const xml: string = await fileHandler.readFile(xmlPath);
			expect(xml).to.not.be.null;

			const results = (testEngine as any).processStdOut(xml);
			expect(results).to.be.length(3, 'Should be three result entries');
			expect(results[0].violations).to.be.length(2, 'Unexpected violation count in 1st result');
			expect(results[1].violations).to.be.length(1, 'Unexpected violation count in 2nd result');
			expect(results[2].violations).to.be.length(1, 'Unexpected violation count in 3rd result');
		});
	});
    
    
	describe('shouldEngineRun()', () => {
		it('should always return true if the engine was not filtered out', () => {
			expect((testEngine as any).shouldEngineRun([],[],[],new Map<string,string>())).to.be.true;
		});
	});

	describe('sortPaths()', () => {
		it('should return the correct map based on valid cpd files and targets given', () => {
			const targets: RuleTarget[] = [{
				target: '',
				paths: ['file.cls','file.trigger','file.page','file.py','file.component']
			}]
		
			let langPathMap: Map<LANGUAGE, string[]> = new Map();
			langPathMap.set(LANGUAGE.APEX, ['file.cls','file.trigger']);
			langPathMap.set(LANGUAGE.VISUALFORCE, ['file.page','file.component']);
			expect((testEngine as any).sortPaths(targets)).to.eql(langPathMap);
		});
	});

	describe('jsonToRuleResult()', () => {
		it('should return the correct rule result array from the json of cpd output', () => {
			const duplications = [{"type":"element","name":"duplication","attributes":{"lines":"2","tokens":"72"},"elements":[{"type":"element","name":"file","attributes":{"column":"44","endcolumn":"75","endline":"2","line":"1","path":"/sfdx-scanner/test/code-fixtures/projects/app/force-app/main/default/pages/Template1.page"}},{"type":"element","name":"file","attributes":{"column":"9","endcolumn":"78","endline":"6","line":"5","path":"/sfdx-scanner/test/code-fixtures/projects/app/force-app/main/default/pages/Template1.page"}},{"type":"element","name":"codefragment","elements":[{"type":"cdata","cdata":"<apex:page showHeader=\"true\" sidebar=\"true\">\n\t<!-- this is a template that embeds other pages and components, and is also a visualforce page in its own right -->"}]}]},{"type":"element","name":"duplication","attributes":{"lines":"1","tokens":"71"},"elements":[{"type":"element","name":"file","attributes":{"column":"44","endcolumn":"116","endline":"2","line":"2","path":"/sfdx-scanner/test/code-fixtures/projects/app/force-app/main/default/pages/Template1.page"}},{"type":"element","name":"file","attributes":{"column":"9","endcolumn":"81","endline":"6","line":"6","path":"/sfdx-scanner/test/code-fixtures/projects/app/force-app/main/default/pages/Template1.page"}},{"type":"element","name":"codefragment","elements":[{"type":"cdata","cdata":"\t<!-- this is a template that embeds other pages and components, and is also a visualforce page in its own right -->"}]}]}];
			const ruleResults = [{"engine":"cpd","fileName":"/sfdx-scanner/test/code-fixtures/projects/app/force-app/main/default/pages/Template1.page","violations":[{"line":"1","column":"44","endLine":"2","endColumn":"75","ruleName":"copy-paste-detected","severity":3,"message":"1a77270: 1 of 2 duplication segments detected. 2 line(s), 72 tokens.","category":"Copy/Paste Detected","url":"https://pmd.github.io/latest/pmd_userdocs_cpd.html#refactoring-duplicates"},{"line":"5","column":"9","endLine":"6","endColumn":"78","ruleName":"copy-paste-detected","severity":3,"message":"1a77270: 2 of 2 duplication segments detected. 2 line(s), 72 tokens.","category":"Copy/Paste Detected","url":"https://pmd.github.io/latest/pmd_userdocs_cpd.html#refactoring-duplicates"},{"line":"2","column":"44","endLine":"2","endColumn":"116","ruleName":"copy-paste-detected","severity":3,"message":"3d13437: 1 of 2 duplication segments detected. 1 line(s), 71 tokens.","category":"Copy/Paste Detected","url":"https://pmd.github.io/latest/pmd_userdocs_cpd.html#refactoring-duplicates"},{"line":"6","column":"9","endLine":"6","endColumn":"81","ruleName":"copy-paste-detected","severity":3,"message":"3d13437: 2 of 2 duplication segments detected. 1 line(s), 71 tokens.","category":"Copy/Paste Detected","url":"https://pmd.github.io/latest/pmd_userdocs_cpd.html#refactoring-duplicates"}]}]
			expect((testEngine as any).jsonToRuleResults(duplications)).to.eql(ruleResults);
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

			const isEngineRequested = (testEngine as any).isEngineRequested(filterValues, emptyEngineOptions);

			expect(isEngineRequested).to.be.true;
		});

		it('should return true if filter contains "cpd" and engineOptions map contains eslint config', () => {
			const filterValues = ['cpd', 'eslint'];

			const isEngineRequested = (testEngine as any).isEngineRequested(filterValues, engineOptionsWithEslintCustom);

			expect(isEngineRequested).to.be.true;
		});

		it('should return true if filter contains "cpd" and engineOptions map contains pmd config', () => {
			const filterValues = ['cpd', 'pmd'];

			const isEngineRequested = (testEngine as any).isEngineRequested(filterValues, engineOptionsWithPmdCustom);

			expect(isEngineRequested).to.be.true;
		});

		it('should return false if filter does not contain "cpd" irrespective of engineOptions', () => {
			const filterValues = ['retire-js', 'pmd'];

			const isEngineRequested = (testEngine as any).isEngineRequested(filterValues, emptyEngineOptions);

			expect(isEngineRequested).to.be.false;
		});
	});

    
});
