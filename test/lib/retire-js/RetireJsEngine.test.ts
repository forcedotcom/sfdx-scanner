import 'reflect-metadata';
import {RuleResult, RuleTarget} from '../../../src/types';
import path = require('path');
import {expect} from 'chai';
import {RetireJsEngine} from '../../../src/lib/retire-js/RetireJsEngine'
import * as TestOverrides from '../../test-related-lib/TestOverrides';
import { CUSTOM_CONFIG } from '../../../src/Constants';


TestOverrides.initializeTestSetup();

class TestableRetireJsEngine extends RetireJsEngine {
	private invertedAliasMap = null;

	public processOutput(cmdOutput: string, ruleName: string): RuleResult[] {
		return super.processOutput(cmdOutput, ruleName);
	}

	public createTmpDirWithDuplicatedTargets(targets: RuleTarget[]): Promise<string> {
		return super.createTmpDirWithDuplicatedTargets(targets);
	}

	public addFakeAliasData(original: string, alias: string): void {
		this.originalFilesByAlias.set(alias, original);
	}

	public getAliasMap(): Map<string,string> {
		return this.originalFilesByAlias;
	}

	public getInvertedAliasMap(): Map<string,string> {
		if (this.invertedAliasMap === null) {
			const invertedMap: Map<string,string> = new Map();

			for (const [key, value] of this.originalFilesByAlias.entries()) {
				invertedMap.set(value, key);
			}
			this.invertedAliasMap = invertedMap;
		}
		return this.invertedAliasMap;
	}

	public getZipMap(): Map<string,string> {
		return this.zipDstByZipSrc;
	}
}

describe('RetireJsEngine', () => {
	let testEngine: TestableRetireJsEngine;

	beforeEach(async () => {
		testEngine = new TestableRetireJsEngine();

		await testEngine.init();
	});

	describe('createTmpDirWithDuplicatedTargets()', () => {

		describe('Text files', () => {
			// ============= TEST SETUP ==============
			// Create a target that simulates a glob matching two JS files.
			const globPaths = [
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-a', 'jquery-3.1.0.js'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-b', 'jquery-3.5.1.js')
			];
			const globTarget: RuleTarget = {
				target: path.join('.', 'test', 'code-fixtures', 'projects', 'dep-test-app', '**', 'jquery*.js'),
				paths: globPaths
			};

			// Create a target that simulates matching an entire directory containing some JS files.
			const dirPaths = [
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-c', 'Burrito.js'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-c', 'Taco.js')
			];
			const dirTarget: RuleTarget = {
				target: path.join('.', 'test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-c'),
				isDirectory: true,
				paths: dirPaths
			};

			// Create a target that simulates matching a single JS file directly.
			const filePaths = [
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-d', 'OrangeChicken.js')
			];
			const fileTarget: RuleTarget = {
				target: path.join('.', 'test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-d', 'OrangeChicken.js'),
				paths: filePaths
			};

			// Create a target that simulates matching a directory full of .resource files.
			const resourcePaths = [
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-e', 'JsStaticResource1.resource'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-e', 'JsStaticResource2.resource'),
				// Even though this resource is an HTML file instead of a JS file, we still expect it to be duplicated
				// because it's still a text file.
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-e', 'HtmlStaticResource1.resource'),
			];
			const resourceTarget: RuleTarget = {
				target: path.join('.', 'test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-e'),
				isDirectory: true,
				paths: resourcePaths
			};

			// Create a target that simulates matching a directory containing a bunch of files with weird/absent extensions,
			// but corresponding .resource-meta.xml files denoting them as static resources.
			const implicitResourcePaths = [
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-g', 'JsResWithOddExt.foo'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-g', 'JsResWithoutExt')
			];
			const implicitResourceTarget: RuleTarget = {
				target: path.join('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-g'),
				isDirectory: true,
				paths: implicitResourcePaths
			};

			// Put all of our targets into an array.
			const targets: RuleTarget[] = [globTarget, dirTarget, fileTarget, resourceTarget, implicitResourceTarget];


			it('Files with a .js extension are duplicated', async () => {
				// ================= INVOCATION OF TEST METHOD ============
				await testEngine.createTmpDirWithDuplicatedTargets(targets);

				// ================ ASSERTIONS ================
				// Create an array of the files we're looking for, and a set of all the files that were aliased.
				const expectedDupedFiles: string[] = [...globPaths, ...dirPaths, ...filePaths];
				const actualDupedFiles: Set<string> = new Set([...testEngine.getAliasMap().values()]);

				expectedDupedFiles.forEach((expectedFile) => {
					expect(actualDupedFiles.has(expectedFile)).to.equal(true, `JS file ${expectedFile} was not duplicated`);
					expect(testEngine.getInvertedAliasMap().get(expectedFile).endsWith('.js')).to.equal(true, 'Alias should end in .js');
				});
			});

			it('Files with a .resource extension are duplicated and given a .js alias', async () => {
				// ================= INVOCATION OF TEST METHOD ============
				await testEngine.createTmpDirWithDuplicatedTargets(targets);

				// ================ ASSERTIONS ================
				// Create an array of the files we're looking for, and a set of all the files that were aliased.
				const expectedDupedFiles: string[] = resourcePaths;
				const actualDupedFiles: Set<string> = new Set([...testEngine.getAliasMap().values()]);

				expectedDupedFiles.forEach((expectedFile) => {
					expect(actualDupedFiles.has(expectedFile)).to.equal(true, `Explicit resource file ${expectedFile} was not duplicated`);
					expect(testEngine.getInvertedAliasMap().get(expectedFile).endsWith('.js')).to.equal(true, 'Alias should end in .js');
				});
			});

			it('Files accompanied by a .resource-meta.xml file are duplicated and given a .js alias', async () => {
				// ================= INVOCATION OF TEST METHOD ============
				await testEngine.createTmpDirWithDuplicatedTargets(targets);

				// ================ ASSERTIONS ================
				// Create an array of the files we're looking for, and a set of all the files that were aliased.
				const expectedDupedFiles: string[] = implicitResourcePaths;
				const actualDupedFiles: Set<string> = new Set([...testEngine.getAliasMap().values()]);

				expectedDupedFiles.forEach((expectedFile) => {
					expect(actualDupedFiles.has(expectedFile)).to.equal(true, `Implicit resource file ${expectedFile} was not duplicated`);
					expect(testEngine.getInvertedAliasMap().get(expectedFile).endsWith('.js')).to.equal(true, 'Alias should end in .js');
				});
			});
		});

		describe('Binary files', () => {
			// ===================== TEST SETUP =========
			// Create a target that simulates a glob matching a bunch of different ZIPs, all of which were generated
			// from the same contents. Crucially, this ZIP has no directories within it; its structure is totally flat.
			const flatZipPaths = [
				path.join('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-f', 'ZipFile.zip'),
				path.join('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-f', 'ZipFileAsResource.resource'),
				path.join('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-f', 'ZipFileWithNoExt'),
				path.join('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-f', 'ZipFileWithOddExt.foo')
			];
			const flatZipTarget: RuleTarget = {
				target: path.join('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-f', 'ZipFile*'),
				paths: flatZipPaths
			};

			// Create a target that simulates directly matching a ZIP. Crucially, this ZIP has directories, some of which
			// are empty, and others are not.
			const verticalZipPaths = [
				path.join('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-h', 'ZipWithDirectories.zip')
			];
			const verticalZipTarget: RuleTarget = {
				target: path.join('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-h', '*.zip'),
				paths: verticalZipPaths
			};

			// Create a target that simulates a glob matching a bunch of image files.
			const imgPaths = [
				path.join('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-f', 'ImageFileAsResource.resource'),
				path.join('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-f', 'ImageFileWithNoExt'),
				path.join('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-f', 'ImageFileWithOddExt.foo')
			];
			const imgTarget: RuleTarget = {
				target: path.join('test', 'code-fixtures', 'project', 'dep-test-app', 'folder-f', 'ImageFile*'),
				paths: imgPaths
			};

			const targets: RuleTarget[] = [flatZipTarget, verticalZipTarget, imgTarget];


			it('ZIPs are extracted, and text files within are aliased', async () => {
				// ================= INVOCATION OF TEST METHOD ============
				await testEngine.createTmpDirWithDuplicatedTargets(targets);

				// ================ ASSERTIONS ================
				const flatZipContents = [
					'HtmlFile.html',
					'HtmlFileWithOddExt.foo',
					'HtmlFileWithoutExt',
					'JsFile.js',
					'JsFileWithOddExt.foo',
					'JsFileWithoutExt'
				];

				// Paths within a ZIP are normalized to UNIX.
				const verticalZipContents = [
					'FilledParentFolder/ChildFolderWithText/JsFile.js',
					'FilledParentFolder/ChildFolderWithText/JsFileWithOddExt.foo',
					'FilledParentFolder/ChildFolderWithText/JsFileWithoutExt'
				];

				const actualDupedFiles = new Set([...testEngine.getAliasMap().values()]);

				// For each of the flat ZIPs...
				for (const zipPath of flatZipPaths) {
					// Verify that the ZIP was extracted.
					expect(testEngine.getZipMap().has(zipPath)).to.equal(true, `Zip file ${zipPath} should have been extracted`);

					// Verify that all of the expected files in the zip were aliased.
					for (const expectedFile of flatZipContents) {
						const fullPath = `${zipPath}:${expectedFile}`;
						expect(actualDupedFiles.has(fullPath)).to.equal(true, `Zip contents ${fullPath} should be aliased`);
					}
				}

				// For the vertical ZIPs...
				for (const zipPath of verticalZipPaths) {
					// Verify that the ZIP was extracted.
					expect(testEngine.getZipMap().has(zipPath)).to.equal(true, `Zip file ${zipPath} should have been extracted`);

					// Verify that all of the expected files in the zip were aliased.
					for (const expectedFile of verticalZipContents) {
						const fullPath = `${zipPath}:${expectedFile}`;
						expect(actualDupedFiles.has(fullPath)).to.equal(true, `Zip contents ${fullPath} should be aliased`);
					}
				}

			});

			it('Non-ZIP binary files are ignored', async () => {
				// ================= INVOCATION OF TEST METHOD ============
				await testEngine.createTmpDirWithDuplicatedTargets(targets);

				// ================ ASSERTIONS ================
				// Verify that none of the images were treated as zips.
				for (const imgPath of imgPaths) {
					expect(testEngine.getZipMap().has(imgPath)).to.equal(false, `Extraction should not be attempted on image file ${imgPath}`);
				}
			});
		});
	});

	describe('processOutput()', () => {
		it('Properly dealiases and processes results from files', async () => {
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

		// Changes to the codebase make it unclear how this corner case would occur, but it's worth having the automation
		// so we avoid introducing any weird bugs in the future.
		it('Corner Case: When file has multiple aliases, results are consolidated', async () => {
			// First, we need to seed the engine with some fake data.
			const originalFile = path.join('unimportant', 'path', 'to', 'SomeFile.js');
			const firstAlias = path.join('unimportant', 'alias', 'for', 'Alias1.js');
			const secondAlias = path.join('unimportant', 'alias', 'for', 'Alias2.js');
			testEngine.addFakeAliasData(originalFile, firstAlias);
			testEngine.addFakeAliasData(originalFile, secondAlias);

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
			expect(results.length).to.equal(1, 'Should be one result object, since both aliases correspond to the same original file');
			expect(results[0].fileName).to.equal(originalFile, 'Path should properly de-alias back to the ZIP');
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
