import 'reflect-metadata';
import {FileHandler} from '../../../src/lib/util/FileHandler';
import {RuleResult, RuleTarget} from '../../../src/types';
import path = require('path');
import {expect} from 'chai';
import {RetireJsEngine} from '../../../src/lib/retire-js/RetireJsEngine'
import * as TestOverrides from '../../test-related-lib/TestOverrides';
import globby = require('globby');
import normalize = require('normalize-path');
import { CUSTOM_CONFIG } from '../../../src/Constants';


TestOverrides.initializeTestSetup();

class TestableRetireJsEngine extends RetireJsEngine {
	public processOutput(cmdOutput: string, ruleName: string): RuleResult[] {
		return super.processOutput(cmdOutput, ruleName);
	}

	public createTmpDirWithDuplicatedTargets(targets: RuleTarget[]): Promise<string> {
		return super.createTmpDirWithDuplicatedTargets(targets);
	}

	public addFakeAliasData(original: string, alias: string): void {
		this.originalFilesByAlias.set(alias, original);
	}

	public dealiasFile(alias: string): string {
		return this.originalFilesByAlias.get(alias);
	}
}

describe('RetireJsEngine', () => {
	let testEngine: TestableRetireJsEngine;

	beforeEach(async () => {
		testEngine = new TestableRetireJsEngine();

		await testEngine.init();
	});

	describe('createTmpDirWithDuplicatedTargets()', () => {
		it('Duplicates only specifically targeted .js files', async () => {
			// We'll want some paths that simulate matching a glob.
			const globPaths = [
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-a', 'jquery-3.1.0.js'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-b', 'jquery-3.5.1.js')
			];
			// We'll want some paths that simulate matching a whole directory.
			const dirPaths = [
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-c', 'Burrito.js'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-c', 'ChineseRestaurantMenu.html'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-c', 'RandomMeme.png'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-c', 'Taco.js')
			];
			// We'll want some paths that simulate matching a single file.
			const filePaths = [
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-d', 'OrangeChicken.js')
			];

			// Use these paths to simulate some RuleTargets.
			const targets: RuleTarget[] = [{
				// Simulate a target that matched a glob.
				target: path.join('.', 'test', 'code-fixtures', 'projects', 'dep-test-app', '**', 'jquery*.js'),
				paths: globPaths
			}, {
				// Simulate a target that matched a directory.
				target: path.join('.', 'test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-c'),
				isDirectory: true,
				paths: dirPaths
			}, {
				// Simulate a target that matched a single file directly.
				target: path.join('.', 'test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-d', 'OrangeChicken.js'),
				paths: filePaths
			}];

			// THIS IS THE ACTUAL METHOD BEING TESTED: Construct our temporary directory.
			const tmpDir: string = await testEngine.createTmpDirWithDuplicatedTargets(targets);

			// We expect the directory to still exist, since the process hasn't actually exited yet.
			expect(await new FileHandler().exists(tmpDir)).to.equal(true, `Temp directory ${tmpDir} should still exist.`);
			// We expect the various files to exist somewhere in the temp directory.
			const dupedFiles: string[] = await globby(normalize(path.join(tmpDir, '**', '*')));
			expect(dupedFiles.length).to.equal(5, 'Wrong number of files copied.');
			// Make sure the files themselves have the names we expect.
			const dupedFileBaseNames = dupedFiles.map(f => path.basename(f));
			const expectedFileNames = [...globPaths, ...dirPaths, ...filePaths].map(f => path.basename(f));
			for (const e of expectedFileNames) {
				if (e.endsWith('.js')) {
					expect(dupedFileBaseNames).to.include(e, 'Unexpectedly failed to duplicate file');
				} else {
					expect(dupedFileBaseNames).to.not.include(e, 'Unexpectedly duplicated non-JS file');
				}
			}
		});

		it('Targeted text-based static resources are duplicated and renamed', async () => {
			// We'll want some paths that simulate matching an entire directory. Importantly, the resources are a mixture
			// of JS and non-JS.
			const resourcePaths = [
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-e', 'JsStaticResource1.resource'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-e', 'JsStaticResource2.resource'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-e', 'HtmlStaticResource1.resource'),
			];

			const targets: RuleTarget[] = [{
				target: path.dirname(resourcePaths[0]),
				isDirectory: true,
				paths: resourcePaths
			}];

			// THIS IS THE ACTUAL METHOD BEING TESTED: Construct our temporary directory.
			const tmpDir: string = await testEngine.createTmpDirWithDuplicatedTargets(targets);

			// We expect the directory to still exist, since the process hasn't actually exited yet.
			expect(await new FileHandler().exists(tmpDir)).to.equal(true, `Temp directory ${tmpDir} should still exist.`);
			// We expect various files to exist somewhere in the temp directory.
			const dupedFiles: string[] = await globby(normalize(path.join(tmpDir, '**', '*')));
			expect(dupedFiles.length).to.equal(3, 'Wrong number of files copied');
			// Expect each of the copied files to have a `.js` extension.
			for (const d of dupedFiles) {
				expect(path.extname(d)).to.equal('.js', 'Copied static resource should have .js file extension');
			}
		});

		it('ZIPs and ZIP-type static resources are unpacked, other binaries are ignored', async () => {
			const targetDir: string = path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-f');

			const angularPath: string = path.resolve(targetDir, 'AngularJS.zip');
			const leafletPath: string = path.resolve(targetDir, 'leaflet.resource');
			const randomMemePath: string = path.resolve(targetDir, 'RandomMeme.resource');
			const zipPaths = [angularPath, leafletPath, randomMemePath];

			const targets: RuleTarget[] = [{
				target: targetDir,
				isDirectory: true,
				paths: zipPaths
			}];

			// THIS IS THE ACTUAL METHOD BEING TESTED: Construct our temporary directory.
			const tmpDir: string = await testEngine.createTmpDirWithDuplicatedTargets(targets);

			// We expect the temp directory to still exist, since the process hasn't actually exited yet.
			const fh = new FileHandler();
			expect(await fh.exists(tmpDir)).to.equal(true, `Temp directory ${tmpDir} should still exist.`);
			// Verify that files were actually extracted from AngularJS.zip, and that every JS file has the expected alias.
			const extractedAngular = await globby(normalize(path.join(tmpDir, '**', 'AngularJS-extracted', '**', '*')));
			expect(extractedAngular.length).to.be.greaterThan(0, 'Should be some copied Angular files');
			for (const subpath of extractedAngular) {
				if (path.extname(subpath).toLowerCase() === '.js') {
					// We want to make sure that each file in the ZIP was aliased back to its relative position within the ZIP.
					// We'll use everything after AngularJS-extracted to build the expected directories.
					const relativeRoot = `AngularJS-extracted`;
					const relativeRootStartPoint = subpath.lastIndexOf(relativeRoot) + relativeRoot.length + 1;
					expect(testEngine.dealiasFile(subpath)).to.equal(`${angularPath}:${subpath.slice(relativeRootStartPoint)}`);
				}
			}

			// Same verification as above.
			const extractedLeaflet = await globby(normalize(path.join(tmpDir, '**', 'leaflet-extracted', '**', '*')));
			expect(extractedLeaflet.length).to.be.greaterThan(0, 'Should be some copied Leaflet files');
			for (const subpath of extractedLeaflet) {
				if (path.extname(subpath).toLowerCase() === '.js') {
					const relativeRoot = `leaflet-extracted`;
					const relativeRootStartPoint = subpath.lastIndexOf(relativeRoot) + relativeRoot.length + 1;
					expect(testEngine.dealiasFile(subpath)).to.equal(`${leafletPath}:${subpath.slice(relativeRootStartPoint)}`);
				}
			}
			// Nothing should be extracted from RandomMeme, because it's a picture.
			const extractedRandomMeme = await globby(normalize(path.join(tmpDir, '**', 'RandomMeme-extracted', '**', '*')));
			expect(extractedRandomMeme.length).to.equal(0, 'Somehow extracted files from a PNG');
		});
	});

	describe('processOutput()', () => {
		it('Properly dealiases and processes results from non-zipped file', async () => {
			// First, we need to seed the test engine with some fake aliases.
			const firstOriginal = path.join('first', 'unimportant', 'path', 'jquery-3.1.0.js');
			const firstAlias = path.join('first', 'unimportant', 'alias', 'jquery-3.1.0.js');
			const secondOriginal = path.join('first', 'unimportant', 'path', 'angular-scenario.js');
			const secondAlias = path.join('first', 'unimportant', 'alias', 'angular-scenario.js');

			testEngine.addFakeAliasData(firstOriginal, firstAlias);
			testEngine.addFakeAliasData(secondOriginal, secondAlias);

			// Next, we want to spoof some output that looks like it came from RetireJS.
			const fakeRetireOutput = {
				"version": "2.2.2",
				"data": [{
					"file": firstAlias,
					"results": [{
						"version": "3.1.0",
						"component": "jquery",
						"vulnerabilities": [{
							"severity": "low"
						}, {
							"severity": "medium"
						}, {
							"severity": "medium"
						}]
					}]
				}, {
					"file": secondAlias,
					"results": [{
						"version": "1.10.2",
						"component": "jquery",
						"vulnerabilities": [{
							"severity": "high"
						}, {
							"severity": "medium"
						}, {
							"severity": "low"
						}, {
							"severity": "medium"
						}, {
							"severity": "medium"
						}]
					}, {
						"version": "1.2.13",
						"component": "angularjs",
						"vulnerabilities": [{
							"severity": "low"
						}, {
							"severity": "low"
						}, {
							"severity": "low"
						}, {
							"severity": "low"
						}, {
							"severity": "low"
						}, {
							"severity": "low"
						}]
					}]
				}]
			};

			// THIS IS THE ACTUAL METHOD BEING TESTED: Now we feed that fake result into the engine and see what we get back.
			const results: RuleResult[] = testEngine.processOutput(JSON.stringify(fakeRetireOutput), 'insecure-bundled-dependencies');

			// Now we run our assertions.
			expect(results.length).to.equal(2, 'Should be two result objects because of the two spoofed files.');
			expect(results[0].fileName).to.equal(firstOriginal, 'First path should have been de-aliased properly');
			expect(results[0].violations.length).to.equal(1, 'Should be a single violation in the first result');
			expect(results[0].violations[0].severity).to.equal(2, 'Severity should be translated to 2');
			expect(results[1].fileName).to.equal(secondOriginal, 'Second path should have been de-aliased properly');
			expect(results[1].violations.length).to.equal(2, 'Should be two violations in the second file');
			expect(results[1].violations[0].severity).to.equal(1, 'Sev should be translated to 1');
			expect(results[1].violations[1].severity).to.equal(3, 'Sev should be translated to 3');
		});

		it('Results from ZIP contents are properly consolidated', async () => {
			// First, we need to seed the engine with some fake data.
			const originalZip = path.join('unimportant', 'path', 'to', 'SomeBundle.zip');
			const firstAlias = path.join('unimportant', 'alias', 'for', 'SomeBundle-extracted', 'subfolder-a', 'jquery-3.1.0.js');
			const secondAlias = path.join('unimportant', 'alias', 'for', 'SomeBundle-extracted', 'subfolder-b', 'angular-scenario.js');
			testEngine.addFakeAliasData(originalZip, firstAlias);
			testEngine.addFakeAliasData(originalZip, secondAlias);

			// Next, we want to spoof some output that looks like it came from RetireJS.
			const fakeRetireOutput = {
				"version": "2.2.2",
				"data": [{
					"file": firstAlias,
					"results": [{
						"version": "3.1.0",
						"component": "jquery",
						"vulnerabilities": [{
							"severity": "low"
						}, {
							"severity": "medium"
						}, {
							"severity": "medium"
						}]
					}]
				}, {
					"file": secondAlias,
					"results": [{
						"version": "1.10.2",
						"component": "jquery",
						"vulnerabilities": [{
							"severity": "high"
						}, {
							"severity": "medium"
						}, {
							"severity": "low"
						}, {
							"severity": "medium"
						}, {
							"severity": "medium"
						}]
					}, {
						"version": "1.2.13",
						"component": "angularjs",
						"vulnerabilities": [{
							"severity": "low"
						}, {
							"severity": "low"
						}, {
							"severity": "low"
						}, {
							"severity": "low"
						}, {
							"severity": "low"
						}, {
							"severity": "low"
						}]
					}]
				}]
			};

			// THIS IS THE ACTUAL METHOD BEING TESTED: Now we feed that fake result into the engine and see what we get back.
			const results: RuleResult[] = testEngine.processOutput(JSON.stringify(fakeRetireOutput), 'insecure-bundled-dependencies');

			// Now we run our assertions.
			expect(results.length).to.equal(1, 'Should be one result object, since both "files" are in the same "zip".');
			expect(results[0].fileName).to.equal(originalZip, 'Path should properly de-alias back to the ZIP');
			expect(results[0].violations.length).to.equal(3, 'All violations should be consolidated properly');
			expect(results[0].violations[0].severity).to.equal(2, 'Severity should be translated to 2');
			expect(results[0].violations[1].severity).to.equal(1, 'Sev should be translated to 1');
			expect(results[0].violations[2].severity).to.equal(3, 'Sev should be translated to 3');
		});

		describe('Error handling', () => {
			it('Throws user-friendly error for un-parsable JSON', async () => {
				const invalidJson = '{"beep": [';

				try {
					const results: RuleResult[] = testEngine.processOutput(invalidJson, 'insecure-bundled-dependencies');
					expect(true).to.equal(false, 'Exception should be thrown');
					expect(results).to.equal(null, 'This assertion should never fire. It is needed to make the TS compiler stop complaining');
				} catch (e) {
					expect(e.message.toLowerCase()).to.include('could not parse retirejs output', 'Error message should be user-friendly');
				}
			});

			it('Throws user-friendly error for improperly formed JSON', async () => {
				const malformedJson = {
					// The top-level will not have the `data` property.
					"version": "2.2.2"
				};

				try {
					const results: RuleResult[] = testEngine.processOutput(JSON.stringify(malformedJson), 'insecure-bundled-dependencies');
					expect(true).to.equal(false, 'Exception should be thrown');
					expect(results).to.equal(null, 'This assertion should never fire. It is needed to make the TS compiler stop complaining');
				} catch (e) {
					expect(e.message.toLowerCase()).to.include('retire-js output did not match expected structure');
				}
			});
		});

		describe('Tests for shouldEngineRun()', () => {
			it('should always return true if the engine was not filtered out', () => {
				expect(testEngine.shouldEngineRun([],[],[],new Map<string,string>())).to.be.true;
			});
		});

		describe('Tests for isEngineRequested()', () => {
			const emptyEngineOptions = new Map<string, string>();

			const configFilePath = '/some/file/path/config.json';
			const engineOptionsWithEslintCustom = new Map<string, string>([
				[CUSTOM_CONFIG.EslintConfig, configFilePath]
			]);
			const engineOptionsWithPmdCustom = new Map<string, string>([
				[CUSTOM_CONFIG.PmdConfig, configFilePath]
			]);
			
			it('should return true if filter contains "retire-js" and engineOptions map is empty', () => {
				const filterValues = ['retire-js', 'pmd'];

				const isEngineRequested = testEngine.isEngineRequested(filterValues, emptyEngineOptions);

				expect(isEngineRequested).to.be.true;
			});

			it('should return true if filter contains "retire-js" and engineOptions map contains eslint config', () => {
				const filterValues = ['retire-js', 'pmd'];

				const isEngineRequested = testEngine.isEngineRequested(filterValues, engineOptionsWithEslintCustom);

				expect(isEngineRequested).to.be.true;
			});

			it('should return true if filter contains "retire-js" and engineOptions map contains pmd config', () => {
				const filterValues = ['retire-js', 'pmd'];

				const isEngineRequested = testEngine.isEngineRequested(filterValues, engineOptionsWithPmdCustom);

				expect(isEngineRequested).to.be.true;
			});

			it('should return false if filter does not contain "retire-js" irrespective of engineOptions', () => {
				const filterValues = ['eslint-lwc', 'pmd'];

				const isEngineRequested = testEngine.isEngineRequested(filterValues, emptyEngineOptions);

				expect(isEngineRequested).to.be.false;
			});

		});
	});
});
