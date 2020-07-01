import { expect } from 'chai';
import Sinon = require('sinon');
import {TypescriptEslintStrategy} from '../../../src/lib/eslint/TypescriptEslintStrategy';
import { FileHandler } from '../../../src/lib/util/FileHandler';
import * as path from 'path';
import {Controller} from '../../../src/ioc.config';
import {OUTPUT_FORMAT, RuleManager} from '../../../src/lib/RuleManager';
import LocalCatalog from '../../../src/lib/services/LocalCatalog';
import fs = require('fs');
import { fail } from 'assert';

const CATALOG_FIXTURE_PATH = path.join('test', 'catalog-fixtures', 'DefaultCatalogFixture.json');
const CATALOG_FIXTURE_RULE_COUNT = 15;

let ruleManager: RuleManager = null;

describe('TypescriptEslint Strategy', () => {
	describe('Test cases with #findTsConfig', () => {
		const tsconfigInCwd = path.resolve('', 'tsconfig.json');

		describe('find tsconfig from current working directory', () => {
			afterEach(() => {
				Sinon.restore();
			});
			it('should look for tsconfig in current working directory', async () => {
				Sinon.stub(FileHandler.prototype, 'exists').resolves(true);

				const tsStrategy = new TypescriptEslintStrategy();
				await tsStrategy.init();

				const fileFound = await tsStrategy.findTsconfig();

				expect(fileFound).equals(tsconfigInCwd);
			});
			it('should throw an error if tsconfig is not in current working directory', async () => {
				Sinon.stub(FileHandler.prototype, 'exists').resolves(false);

				const tsStrategy = new TypescriptEslintStrategy();
				await tsStrategy.init();

				try {
					await tsStrategy.findTsconfig();
					fail('findTsconfig should have thrown');
				} catch(e) {
					expect(e.message).to.equal(`Unable to find 'tsconfig.json' in current directory '${path.resolve()}'`)
				}
			});

			// TODO: test cases that set exists to true/false depending on call order
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
				const output = await ruleManager.runRulesMatchingCriteria([], ['invalid-ts'], OUTPUT_FORMAT.JSON);
				expect(output).to.contain("test/code-fixtures/projects/invalid-ts/src/simpleYetWrong.ts' does not reside in a location that is included by your tsconfig.json 'include' attribute.");
				expect(output).to.not.contain('Parsing error: \\"parserOptions.project\\" has been set');
			});

			// TODO: test cases that set exists to true/false depending on call order
		});
	});
});
