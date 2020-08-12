import {expect} from 'chai';
import {Stats} from 'fs';
import * as TestOverrides from '../test-related-lib/TestOverrides';
import {CustomRulePathManager} from '../../src/lib/CustomRulePathManager';
import {PmdEngine} from '../../src/lib/pmd/PmdEngine';
import {FileHandler} from '../../src/lib/util/FileHandler';
import path = require('path');
import Sinon = require('sinon');
import { Controller } from '../../src/ioc.config';

/**
 * Unit tests to verify CustomRulePathManager
 * TODO: Add tests to cover exception scenarios when CustomPath.json does not exist
 */

describe('CustomRulePathManager tests', () => {

	// File exists, but content has been somehow cleared
	const emptyFile = '';

	// One entry for apex, two for java
	const populatedFile = '{"pmd": {"apex": ["/some/user/path/customRule.jar"],"java": ["/abc/def/ghi","/home/lib/jars"]}}';

	// Most tests rely on the singletons being clean
	beforeEach(() => TestOverrides.initializeTestSetup());

	describe('Rule path entries creation', () => {

		describe('with pre-populated file', () => {
			let readStub;
			beforeEach(() => {
				readStub = Sinon.stub(CustomRulePathManager.prototype, 'readRulePathFile').resolves(populatedFile);
			});

			afterEach(() => {
				readStub.restore();
			});

			it('should read CustomPaths.json to get Rule Path Entries', async () => {
				const manager = await Controller.createRulePathManager();

				// Execute test
				const rulePathMap = await manager.getRulePathEntries(PmdEngine.NAME);

				// Validate run
				expect(readStub.calledOnce).to.be.true;
				expect(rulePathMap).to.be.lengthOf(2);

				//Validate each entry
				expect(rulePathMap).has.keys('apex', 'java');
				const apexPaths = rulePathMap.get('apex');
				expect(apexPaths).to.be.lengthOf(1);
				expect(apexPaths).deep.contains('/some/user/path/customRule.jar');

				const javaPaths = rulePathMap.get('java');
				expect(javaPaths).to.be.lengthOf(2);
				expect(javaPaths).deep.contains('/abc/def/ghi');
				expect(javaPaths).deep.contains('/home/lib/jars');

			});

			it('should initialize only once', async () => {
				const manager = await Controller.createRulePathManager();

				// Execute test
				await manager.getRulePathEntries(PmdEngine.NAME);
				// Rerun same end point again. This time, it shouldn't have read file
				await manager.getRulePathEntries(PmdEngine.NAME);

				// Validate
				expect(readStub.calledOnce).to.be.true;
			});
		});


		it('should handle empty Rule Path file gracefully', async () => {
			const readStub = Sinon.stub(CustomRulePathManager.prototype, 'readRulePathFile').resolves(emptyFile);
			try {
				const manager = await Controller.createRulePathManager();
				const rulePathMap = await manager.getRulePathEntries(PmdEngine.NAME);

				expect(readStub.calledOnce).to.be.true;
				expect(rulePathMap).is.not.null;
				expect(rulePathMap).to.be.lengthOf(0);
			} finally {
				readStub.restore();
			}
		});
	});

	describe('Adding new Rule path entries', () => {
		// For all tests, we'll want to stub out the writeFile and mkdir methods with no-ops.
		before(() => {
			Sinon.stub(FileHandler.prototype, 'writeFile').resolves();
			Sinon.stub(FileHandler.prototype, 'mkdirIfNotExists').resolves();
		});

		after(() => {
			Sinon.restore();
		});

		describe('Test Case: Adding individual files', () => {
			// For these tests, we'll want to stub out stats so we can simulate a file.
			let statsStub;
			before(() => {
				const mockFileStats = new Stats();
				// Force our fake stat to look like a file.
				mockFileStats.isDirectory = (): boolean => false;
				mockFileStats.isFile = (): boolean => true;
				statsStub = Sinon.stub(FileHandler.prototype, 'stats').resolves(mockFileStats);
			});

			after(() => {
				statsStub.restore();
			});

			it('Should reflect any new entries', async () => {
				const readStub = Sinon.stub(CustomRulePathManager.prototype, 'readRulePathFile').resolves(emptyFile);

				try {
					const manager = await Controller.createRulePathManager();
					const language = 'javascript';
					const paths = ['/absolute/path/to/SomeJar.jar', '/another/absolute/path/to/AnotherJar.jar'];

					// Execute test - fetch original state of Map, add entries, check state of updated Map
					const originalRulePathMap = await manager.getRulePathEntries(PmdEngine.NAME);
					await manager.addPathsForLanguage(language, paths);
					const updatedRulePathMap = await manager.getRulePathEntries(PmdEngine.NAME);

					// Validate
					expect(originalRulePathMap).to.be.empty;
					expect(updatedRulePathMap).to.have.keys(language);
					const pathFromRun = updatedRulePathMap.get(language);
					expect(pathFromRun.size).to.equal(2, 'Should be four paths added');
					expect(pathFromRun).to.contain(paths[0], `Updated path set was missing expected path`);
					expect(pathFromRun).to.contain(paths[1], `Updated path set was missing expected path`);
				} finally {
					readStub.restore();
				}
			});

			it('Should append new entries to any existing entries for the language', async () => {
				// Setup stub
				const fileContent = '{"pmd": {"apex": ["/some/user/path/customRule.jar"]}}';
				const readStub = Sinon.stub(CustomRulePathManager.prototype, 'readRulePathFile').resolves(fileContent);

				try {
					const manager = await Controller.createRulePathManager();
					const language = 'apex';
					const newPaths = ['/absolute/path/to/SomeJar.jar', '/different/absolute/path/to/OtherJar.jar'];

					// Execute test
					const originalRulePathMap = await manager.getRulePathEntries(PmdEngine.NAME);
					// Pre-validate to make sure test setup is alright, before proceeding
					expect(originalRulePathMap).has.keys([language]);
					const originalPathEntries = originalRulePathMap.get(language);
					expect(originalPathEntries).to.be.lengthOf(1);
					// Now add more entries to same language
					await manager.addPathsForLanguage(language, newPaths);
					// Fetch updated Map to validate
					const updatedRulePathMap = await manager.getRulePathEntries(PmdEngine.NAME);

					// Validate
					const updatedPathEntries = updatedRulePathMap.get(language);
					expect(updatedPathEntries).to.be.lengthOf(3, 'Two new paths should be added, for a total of 3');
					expect(updatedPathEntries).to.contain(newPaths[0], `Updated path set was missing expected path`);
					expect(updatedPathEntries).to.contain(newPaths[1], `Updated path set was missing expected path`);
				} finally {
					readStub.restore();
				}
			});
		});

		describe('Test Case: Adding the contents of a folder', () => {
			// For these tests, we'll want to stub out stats and readDir so we can simulate a directory containing some JARs.
			const files = ['test1.jar', 'test2.jar'];
			let statsStub;
			before(() => {
				const mockDirStats = new Stats();
				// Force our fake stat to look like a directory.
				mockDirStats.isDirectory = (): boolean => true;
				mockDirStats.isFile = (): boolean => false;
				statsStub = Sinon.stub(FileHandler.prototype, 'stats').resolves(mockDirStats);
				// Our fake directory is pretending to have two JARs in it.
				Sinon.stub(FileHandler.prototype, 'readDir').resolves(files);
			});

			after(() => {
				statsStub.restore();
			});

			it('Should reflect newly added entries', async () => {
				const readStub = Sinon.stub(CustomRulePathManager.prototype, 'readRulePathFile').resolves(emptyFile);

				try {
					const manager = await Controller.createRulePathManager();
					const language = 'javascript';
					const paths = ['path1', 'path2'];

					// Execute test - fetch original state of Map, add entries, check state of updated Map
					const originalRulePathMap = await manager.getRulePathEntries(PmdEngine.NAME);
					await manager.addPathsForLanguage(language, paths);
					const updatedRulePathMap = await manager.getRulePathEntries(PmdEngine.NAME);

					// Validate
					expect(originalRulePathMap).to.be.empty;
					expect(updatedRulePathMap).to.have.keys(language);
					const pathFromRun = updatedRulePathMap.get(language);
					expect(pathFromRun.size).to.equal(4, 'Should be four paths added');
					expect(pathFromRun).to.contain(path.resolve(path.join(paths[0], files[0])), `Updated path set was missing expected path`);
					expect(pathFromRun).to.contain(path.resolve(path.join(paths[0], files[1])), `Updated path set was missing expected path`);
					expect(pathFromRun).to.contain(path.resolve(path.join(paths[1], files[0])), `Updated path set was missing expected path`);
					expect(pathFromRun).to.contain(path.resolve(path.join(paths[1], files[1])), `Updated path set was missing expected path`);
				} finally {
					readStub.restore();
				}
			});

			it('Should append new entries to any existing entries for language', async () => {
				// Setup stub
				const fileContent = '{"pmd": {"apex": ["/some/user/path/customRule.jar"]}}';
				const readStub = Sinon.stub(CustomRulePathManager.prototype, 'readRulePathFile').resolves(fileContent);

				try {
					const manager = await Controller.createRulePathManager();
					const language = 'apex';
					const newPath = '/my/new/path';

					// Execute test
					const originalRulePathMap = await manager.getRulePathEntries(PmdEngine.NAME);
					// Pre-validate to make sure test setup is alright, before proceeding
					expect(originalRulePathMap).has.keys([language]);
					const originalPathEntries = originalRulePathMap.get(language);
					expect(originalPathEntries).to.be.lengthOf(1);
					// Now add more entries to same language
					await manager.addPathsForLanguage(language, [newPath]);
					// Fetch updated Map to validate
					const updatedRulePathMap = await manager.getRulePathEntries(PmdEngine.NAME);

					// Validate
					const updatedPathEntries = updatedRulePathMap.get(language);
					expect(updatedPathEntries).to.be.lengthOf(3, 'Two new paths should be added, for a total of 3');
					expect(updatedPathEntries).to.contain(path.join(newPath, files[0]), `Updated path set was missing expected path`);
					expect(updatedPathEntries).to.contain(path.join(newPath, files[1]), `Updated path set was missing expected path`);
				} finally {
					readStub.restore();
				}
			});
		});
	});
});
