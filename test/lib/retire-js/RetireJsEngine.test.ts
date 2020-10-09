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
			testEngine.addFakeAliasData(path.join('first', 'unimportant', 'path'), path.join('first', 'unimportant', 'alias'));
			testEngine.addFakeAliasData(path.join('second', 'unimportant', 'path'), path.join('second', 'unimportant', 'alias'));

			// Next, we want to spoof some output that looks like it came from RetireJS.
			const fakeRetireOutput = {
				"data": [{
					"file": path.join('first', 'unimportant', 'alias', 'jquery-3.1.0.js'),
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
					"file": path.join('second', 'unimportant', 'alias', 'angular-scenario.js'),
					"results": [{
						"version": "1.10.2",
						"component": "jquery",
						"vulnerabilities": [{
							"severity": "medium"
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
							"severity": "medium"
						}, {
							"severity": "medium"
						}, {
							"severity": "medium"
						}, {
							"severity": "medium"
						}, {
							"severity": "medium"
						}, {
							"severity": "low"
						}]
					}]
				}]
			};

			// THIS IS THE ACTUAL METHOD BEING TESTED: Now we feed that fake result into the engine and see what we get back.
			const results: RuleResult[] = testEngine.procesOutput(JSON.stringify(fakeRetireOutput), 'insecure-bundled-dependencies');
		});

	});
});
