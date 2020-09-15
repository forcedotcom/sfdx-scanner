import { expect } from 'chai';
import Sinon = require('sinon');
import {TypescriptEslintStrategy, TYPESCRIPT_ENGINE_OPTIONS} from '../../../src/lib/eslint/TypescriptEslintStrategy';
import { FileHandler } from '../../../src/lib/util/FileHandler';
import * as path from 'path';
import {Controller} from '../../../src/ioc.config';
import {OUTPUT_FORMAT} from '../../../src/Constants';
import {RuleManager} from '../../../src/lib/RuleManager';
import LocalCatalog from '../../../src/lib/services/LocalCatalog';
import fs = require('fs');
import { fail } from 'assert';
import {RuleResult, RuleViolation} from '../../../src/types';

const CATALOG_FIXTURE_PATH = path.join('test', 'catalog-fixtures', 'DefaultCatalogFixture.json');
const CATALOG_FIXTURE_RULE_COUNT = 15;
const EMPTY_ENGINE_OPTIONS = new Map<string, string>();

let ruleManager: RuleManager = null;

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
					expect(e.message).to.contain(`Unable to find 'tsconfig.json' in current directory '${path.resolve()}'`)
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
					expect(e.message).to.contain(`Unable to find 'tsconfig.json' at location '${engineOptionPath}'`)
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
					expect(e.message).to.contain(`File '${engineOptionPath}' specified by the --tsconfig flag must be named 'tsconfig.json'`)
				}
			});
		});

		describe('typescript file not in tsconfig.json causes error', () => {
			before(async () => {
				// Make sure all catalogs exist where they're supposed to.
				if (!fs.existsSync(CATALOG_FIXTURE_PATH)) {
					throw new Error('Fake catalog does not exist');
				}

				// Make sure all catalogs have the expected number of rules.
				const catalogJson = JSON.parse(fs.readFileSync(CATALOG_FIXTURE_PATH).toString());
				if (catalogJson.rules.length !== CATALOG_FIXTURE_RULE_COUNT) {
					throw new Error('Fake catalog has ' + catalogJson.rules.length + ' rules instead of ' + CATALOG_FIXTURE_RULE_COUNT);
				}

				// Stub out the LocalCatalog's getCatalog method so it always returns the fake catalog, whose contents are known,
				// and never overwrites the real catalog. (Or we could use the IOC container to do this without sinon.)
				Sinon.stub(LocalCatalog.prototype, 'getCatalog').callsFake(async () => {
					return JSON.parse(fs.readFileSync(CATALOG_FIXTURE_PATH).toString());
				});

				// Declare our rule manager.
				ruleManager = await Controller.createRuleManager();

				process.chdir(path.join('test', 'code-fixtures', 'projects'));
			});
			after(() => {
				process.chdir("../../..");
			});

			it('The typescript engine should convert the eslint error to something more user friendly', async () => {
				const {results} = await ruleManager.runRulesMatchingCriteria([], ['invalid-ts'], OUTPUT_FORMAT.JSON, EMPTY_ENGINE_OPTIONS);
				// Parse the json in order to make the string match easier.
				// There should be a single violation with a single message
				const ruleResults: RuleResult[] = JSON.parse(results.toString());
				expect(ruleResults).to.have.lengthOf(1);
				const ruleResult: RuleResult = ruleResults[0];
				expect(ruleResult.violations).to.have.lengthOf(1);
				const violation: RuleViolation = ruleResult.violations[0];
				const message = violation.message;
				const thePath = path.join('test', 'code-fixtures', 'projects', 'invalid-ts', 'src', 'notSpecifiedInTsConfig.ts');
				expect(message).to.contain(`${thePath}' does not reside in a location that is included by your tsconfig.json 'include' attribute.`);
				expect(message).to.not.contain('Parsing error: \\"parserOptions.project\\" has been set');
			});
		});
	});
});
