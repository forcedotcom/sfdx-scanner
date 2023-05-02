import { expect } from 'chai';
import Sinon = require('sinon');
import * as path from 'path';
import { fail } from 'assert';
import {Messages} from '@salesforce/core';
import {HARDCODED_RULES} from '../../../src/Constants';
import {ProcessRuleViolationType} from '../../../src/lib/eslint/EslintCommons';
import {TypescriptEslintStrategy, TYPESCRIPT_ENGINE_OPTIONS} from '../../../src/lib/eslint/TypescriptEslintStrategy';
import { FileHandler } from '../../../src/lib/util/FileHandler';
import {Controller} from '../../../src/Controller';
import {OUTPUT_FORMAT, RuleManager} from '../../../src/lib/RuleManager';
import {RuleResult, RuleViolation} from '../../../src/types';
import * as DataGenerator from './EslintTestDataGenerator';
import * as TestOverrides from '../../test-related-lib/TestOverrides';
import * as TestUtils from '../../TestUtils';

Messages.importMessagesDirectory(__dirname);
const strategyMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'TypescriptEslintStrategy');
const EMPTY_ENGINE_OPTIONS = new Map<string, string>();

let ruleManager: RuleManager = null;

TestOverrides.initializeTestSetup();

class TestTypescriptEslintStrategy extends TypescriptEslintStrategy {
	public async checkEngineOptionsForTsconfig(engineOptions: Map<string, string>): Promise<string> {
		return super.checkEngineOptionsForTsconfig(engineOptions);
	}

	public async checkWorkingDirectoryForTsconfig(): Promise<string> {
		return super.checkWorkingDirectoryForTsconfig();
	}
}

describe('TypescriptEslint Strategy', () => {
	afterEach(() => {
		Sinon.restore();
	});

	describe('processRuleViolation()', () => {
		let violationProcessor: ProcessRuleViolationType = null;
		before(async () => {
			const tsStrategy = new TestTypescriptEslintStrategy();
			await tsStrategy.init();
			violationProcessor = tsStrategy.processRuleViolation();
		});

		it('Parser errors are properly modified', () => {
			const fileName = path.join('ts-sample-project', 'src', 'fileWithBadSyntax.ts');
			const syntaxError: RuleViolation = {
				"line": 1,
				"column": 5,
				"severity": 2,
				"message": "Parsing error: ';' expected.",
				"ruleName": null,
				"category": "",
				"url": ""
			};

			violationProcessor(fileName, syntaxError);

			expect(syntaxError.ruleName).to.equal(HARDCODED_RULES.FILES_MUST_COMPILE.name, 'Wrong rulename assigned');
			expect(syntaxError.category).to.equal(HARDCODED_RULES.FILES_MUST_COMPILE.category, 'Wrong category applied');
		});

		it('Errors about un-included TS files are properly modified', () => {
			const fileName = path.join('test', 'code-fixtures', 'projects', 'ts', 'simpleYetWrong.ts');
			const unincludedFileError: RuleViolation = {
				"line": null,
				"column": null,
				"severity": 2,
				"message": `Parsing error: ESLint was configured to run on \`this path should not matter\` using \`parserOptions.project\`: this part should not matter
However, that TSConfig does not include this file. Either:
- Change ESLint's list of included files to not include this file
- Change that TSConfig to include this file
- Create a new TSConfig that includes this file and include it in your parserOptions.project
See the typescript-eslint docs for more info: https://typescript-eslint.io/linting/troubleshooting##i-get-errors-telling-me-eslint-was-configured-to-run--however-that-tsconfig-does-not--none-of-those-tsconfigs-include-this-file`,
				"ruleName": null,
				"category": "",
				"url": ""
			};

			violationProcessor(fileName, unincludedFileError);

			// We expect that the processor method modified the error message.
			expect(unincludedFileError.message).to.equal(strategyMessages.getMessage('FileNotIncludedByTsConfig', [fileName, 'tsconfig.json']), 'Incorrect msg');
		});
	});

	describe('filterDisallowedRules()', () => {
		it('Removes deprecated rules', async () => {
			const tsStrategy = new TestTypescriptEslintStrategy();
			await tsStrategy.init();

			// THIS IS THE PART BEING TESTED. (Note, `filterDisallowedRules()` is private, hence the `any` cast.)
			const filteredRules = (tsStrategy as any).filterDisallowedRules(DataGenerator.getDummyTypescriptRuleMap());

			expect(filteredRules.has('fake-deprecated')).to.equal(false, 'Deprecated rule should be removed');
		});

		it('Removes rules that are extended by other rules', async () => {
			const tsStrategy = new TestTypescriptEslintStrategy();
			await tsStrategy.init();

			// THIS IS THE PART BEING TESTED. (Note, `filterDisallowedRules()` is private, hence the `any` cast.)
			const filteredRules = (tsStrategy as any).filterDisallowedRules(DataGenerator.getDummyTypescriptRuleMap());

			expect(filteredRules.has('fake-extended-a')).to.equal(false, 'Extended rule should be removed');
			expect(filteredRules.has('fake-extended-b')).to.equal(false, 'Extended rule should be removed');
		});

		it('Keeps non-deprecated, non-extended rules', async () => {
			const tsStrategy = new TestTypescriptEslintStrategy();
			await tsStrategy.init();

			// THIS IS THE PART BEING TESTED. (Note, `filterDisallowedRules()` is private, hence the `any` cast.)
			const filteredRules = (tsStrategy as any).filterDisallowedRules(DataGenerator.getDummyTypescriptRuleMap());

			expect(filteredRules.has('fake-active')).to.equal(true, 'Active rule should be kept');
		});
	});

	describe('Test cases with tsconfig.json', () => {
		const cwdVal = 'another-dir/tsconfig.json';
		const engineOptionsVal = 'some-dir/tsconfig.json';
		describe('findTsconfig', () => {
			it('use tsconfig from cwd', async () => {
				Sinon.stub(TestTypescriptEslintStrategy.prototype, 'checkWorkingDirectoryForTsconfig').resolves(cwdVal);
				Sinon.stub(TestTypescriptEslintStrategy.prototype, 'checkEngineOptionsForTsconfig').resolves(null);

				const tsStrategy = new TestTypescriptEslintStrategy();
				await tsStrategy.init();

				const fileFound =  await tsStrategy.findTsconfig(null);

				expect(fileFound).equals(cwdVal);
			});

			it('use tsconfig from engineOptions', async () => {
				Sinon.stub(TestTypescriptEslintStrategy.prototype, 'checkWorkingDirectoryForTsconfig').resolves(null);
				Sinon.stub(TestTypescriptEslintStrategy.prototype, 'checkEngineOptionsForTsconfig').resolves(engineOptionsVal);

				const tsStrategy = new TestTypescriptEslintStrategy();
				await tsStrategy.init();

				const fileFound =  await tsStrategy.findTsconfig(null);

				expect(fileFound).equals(engineOptionsVal);
			});


			it('engineOptions.tsconfig should take precedence over tsconfig.json file found in the current working directory', async () => {
				Sinon.stub(TestTypescriptEslintStrategy.prototype, 'checkWorkingDirectoryForTsconfig').resolves(cwdVal);
				Sinon.stub(TestTypescriptEslintStrategy.prototype, 'checkEngineOptionsForTsconfig').resolves(engineOptionsVal);

				const tsStrategy = new TestTypescriptEslintStrategy();
				await tsStrategy.init();
				const foundTsConfig = await tsStrategy.findTsconfig(null);
				expect(foundTsConfig).to.equal(engineOptionsVal);
			});

			it('should throw an error if tsconfig is not in current working directory and not specified by engineOptions', async () => {
				Sinon.stub(TestTypescriptEslintStrategy.prototype, 'checkWorkingDirectoryForTsconfig').resolves(null);
				Sinon.stub(TestTypescriptEslintStrategy.prototype, 'checkEngineOptionsForTsconfig').resolves(null);

				const tsStrategy = new TestTypescriptEslintStrategy();
				await tsStrategy.init();

				try {
					await tsStrategy.findTsconfig(null);
					fail('findTsconfig should have thrown');
				} catch(e) {
					expect(e.message).to.contain(`We couldn't find 'tsconfig.json' in the current directory '${path.resolve()}'`)
				}
			});
		});

		describe('checkWorkingDirectoryForTsconfig', () => {
			const tsconfigInCwd = path.resolve('', 'tsconfig.json');

			it('finds tsconfig in current working directory', async () => {
				Sinon.stub(FileHandler.prototype, 'exists').resolves(true);

				const tsStrategy = new TestTypescriptEslintStrategy();
				await tsStrategy.init();

				const fileFound = await tsStrategy.checkWorkingDirectoryForTsconfig();

				expect(fileFound).equals(tsconfigInCwd);
			});

			it('does not find tsconfig in current working directory', async () => {
				Sinon.stub(FileHandler.prototype, 'exists').resolves(false);

				const tsStrategy = new TestTypescriptEslintStrategy();
				await tsStrategy.init();

				const fileFound = await tsStrategy.checkWorkingDirectoryForTsconfig();

				expect(fileFound).to.be.null;
			});
		});

		describe('checkEngineOptionsForTsconfig', () => {
			it('engineOption specifies a valid tsconfig.json', async () => {
				Sinon.stub(FileHandler.prototype, 'exists').resolves(true);

				const engineOptions = new Map();
				const engineOptionPath = path.join('test', 'existing-path', 'tsconfig.json');
				engineOptions.set(TYPESCRIPT_ENGINE_OPTIONS.TSCONFIG, engineOptionPath);

				const tsStrategy = new TestTypescriptEslintStrategy();
				await tsStrategy.init();
				const fileFound =  await tsStrategy.checkEngineOptionsForTsconfig(engineOptions);

				expect(fileFound).equals(engineOptionPath);
			});

			it('engineOption does not specify tsconfig.json', async () => {
				const engineOptions = new Map();
				const engineOptionPath = path.join('test', 'existing-path', 'tsconfig.json');
				engineOptions.set(TYPESCRIPT_ENGINE_OPTIONS.TSCONFIG, engineOptionPath);

				const tsStrategy = new TestTypescriptEslintStrategy();
				await tsStrategy.init();
				const fileFound =  await tsStrategy.checkEngineOptionsForTsconfig(EMPTY_ENGINE_OPTIONS);

				expect(fileFound).to.be.null;
			});

			it('should throw an error if tsconfig.json file specified engineOption does not exist', async () => {
				Sinon.stub(FileHandler.prototype, 'exists').resolves(false);

				const engineOptionPath = path.join('test', 'non-existent-path', 'tsconfig.json');
				const engineOptions = new Map();
				engineOptions.set(TYPESCRIPT_ENGINE_OPTIONS.TSCONFIG, engineOptionPath);

				const tsStrategy = new TestTypescriptEslintStrategy();
				await tsStrategy.init();
				try {
					await tsStrategy.checkEngineOptionsForTsconfig(engineOptions);
					fail('findTsconfig should have thrown');
				} catch(e) {
					expect(e.message).to.contain(`We couldn't find the 'tsconfig.json' at location '${engineOptionPath}'`)
				}
			});

			it('should throw an error if tsconfig engineOption is found, but is not a file named tsconfig.json', async () => {
				const engineOptionPath = path.join('test', 'non-existent-path', 'tsconfig.foo');
				Sinon.stub(FileHandler.prototype, 'exists').resolves(true);

				const engineOptions = new Map();
				engineOptions.set(TYPESCRIPT_ENGINE_OPTIONS.TSCONFIG, engineOptionPath);

				const tsStrategy = new TestTypescriptEslintStrategy();
				await tsStrategy.init();
				try {
					await tsStrategy.checkEngineOptionsForTsconfig(engineOptions);
					fail('findTsconfig should have thrown');
				} catch(e) {
					expect(e.message).to.contain(`The file '${engineOptionPath}' specified by the --tsconfig flag must be named 'tsconfig.json'`)
				}
			});
		});

		describe('typescript file not in tsconfig.json causes error', () => {
			before(async () => {
				TestUtils.stubCatalogFixture();

				// Declare our rule manager.
				ruleManager = await Controller.createRuleManager();

				process.chdir(path.join('test', 'code-fixtures', 'projects'));
			});
			after(() => {
				Sinon.restore();
				process.chdir("../../..");
			});

			it('The typescript engine should convert the eslint error to something more user friendly', async () => {
				const {results} = await ruleManager.runRulesMatchingCriteria([], ['invalid-ts'], {format: OUTPUT_FORMAT.JSON, normalizeSeverity: false, withPilot: false, runDfa: false, sfdxVersion: 'test'}, EMPTY_ENGINE_OPTIONS);
				// Parse the json in order to make the string match easier.
				// There should be a single violation with a single message
				const ruleResults: RuleResult[] = JSON.parse(results.toString());
				expect(ruleResults).to.have.lengthOf(1);
				const ruleResult: RuleResult = ruleResults[0];
				expect(ruleResult.violations).to.have.lengthOf(1);
				const violation: RuleViolation = ruleResult.violations[0];
				const message = violation.message;
				const thePath = path.join('test', 'code-fixtures', 'projects', 'invalid-ts', 'src', 'notSpecifiedInTsConfig.ts');
				expect(message).to.contain(`${thePath}' doesn't reside in a location that is included by your tsconfig.json 'include' attribute.`);
				expect(message).to.not.contain('Parsing error: \\"parserOptions.project\\" has been set');
			});
		});
	});
});
