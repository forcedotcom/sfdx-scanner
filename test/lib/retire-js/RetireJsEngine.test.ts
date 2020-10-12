import 'reflect-metadata';
import {FileHandler} from '../../../src/lib/util/FileHandler';
import {RuleResult, RuleTarget} from '../../../src/types';
import path = require('path');
import {expect} from 'chai';
import {RetireJsEngine} from '../../../src/lib/retire-js/RetireJsEngine'
import * as TestOverrides from '../../test-related-lib/TestOverrides';
import globby = require('globby');


TestOverrides.initializeTestSetup();

class TestableRetireJsEngine extends RetireJsEngine {
	public processOutput(cmdOutput: string, ruleName: string): RuleResult[] {
		return super.processOutput(cmdOutput, ruleName);
	}

	public createTmpDirWithDuplicatedTargets(targets: RuleTarget[]): Promise<string> {
		return super.createTmpDirWithDuplicatedTargets(targets);
	}

	public addFakeAliasData(original: string, alias: string): void {
		this.originalPathsByAlias.set(alias, original);
		this.aliasesByOriginalPath.set(original, alias);
	}
}

describe('RetireJsEngine', () => {
	let testEngine;

	before(async () => {
		testEngine = new TestableRetireJsEngine();

		await testEngine.init();
	});

	describe('createTmpDirWithDuplicatedTargets()', () => {
		it('Duplicates only specifically targeted files', async () => {
			// We'll want some paths that simulate matching a glob.
			const globPaths = [
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-a', 'jquery-3.1.0.js'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-b', 'jquery-3.5.1.js')
			];
			// We'll want some paths that simulate matching a whole directory.
			const dirPaths = [
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-c', 'Burrito.js'),
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
			const dupedFiles: string[] = await globby(path.join(tmpDir, '**', '*'));
			expect(dupedFiles.length).to.equal(5, 'Wrong number of files copied.');
			// Make sure the files themselves have the names we expect.
			const dupedFileBaseNames = dupedFiles.map(f => path.basename(f));
			const expectedFileNames = [...globPaths, ...dirPaths, ...filePaths].map(f => path.basename(f));
			for (const e of expectedFileNames) {
				expect(dupedFileBaseNames).to.include(e, 'Expected duplicate file missing from array');
			}
		});
	});

	describe('processOutput()', () => {
		it('Properly dealiases and processes results from non-zipped file', async () => {
			// First, we need to seed the test engine with some fake aliases.
			const firstPath = path.join('first', 'unimportant', 'path');
			const firstAlias = path.join('first', 'unimportant', 'alias');
			const secondPath = path.join('second', 'unimportant', 'path');
			const secondAlias = path.join('second', 'unimportant', 'alias');

			testEngine.addFakeAliasData(firstPath, firstAlias);
			testEngine.addFakeAliasData(secondPath, secondAlias);

			// Next, we want to spoof some output that looks like it came from RetireJS.
			const fakeRetireOutput = {
				"version": "2.2.2",
				"data": [{
					"file": path.join(firstAlias, 'jquery-3.1.0.js'),
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
					"file": path.join(secondAlias, 'angular-scenario.js'),
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
			expect(results[0].fileName).to.equal(path.join(firstPath, 'jquery-3.1.0.js'), 'First path should have been de-aliased properly');
			expect(results[0].violations.length).to.equal(1, 'Should be a single violation in the first result');
			expect(results[0].violations[0].severity).to.equal(2, 'Severity should be translated to 2');
			expect(results[1].fileName).to.equal(path.join(secondPath, 'angular-scenario.js'), 'Second path should have been de-aliased properly');
			expect(results[1].violations.length).to.equal(2, 'Should be two violations in the second file');
			expect(results[1].violations[0].severity).to.equal(1, 'Sev should be translated to 1');
			expect(results[1].violations[1].severity).to.equal(3, 'Sev should be translated to 3');
		});

		describe('Error handling', () => {
			it('Throws user-friendly error for un-parsable JSON', async () => {
				const invalidJson = '{"beep": [';

				try {
					// eslint-disable-next-line no-unused-vars @typescript-eslint/no-unused-vars
					const results: RuleResult[] = testEngine.processOutput(invalidJson, 'insecure-bundled-dependencies');
					expect(true).to.be(false, 'Exception should be thrown');
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
					// eslint-disable-next-line no-unused-vars @typescript-eslint/no-unused-vars
					const results: RuleResult[] = testEngine.processOutput(JSON.stringify(malformedJson), 'insecure-bundled-dependencies');
					expect(true).to.be(false, 'Exception should be thrown');
				} catch (e) {
					expect(e.message.toLowerCase()).to.include('retire-js output did not match expected structure');
				}
			});
		});
	});
});
