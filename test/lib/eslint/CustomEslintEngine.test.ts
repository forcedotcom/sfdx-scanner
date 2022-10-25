import { expect } from 'chai';
import { CustomEslintEngine } from '../../../src/lib/eslint/CustomEslintEngine';
import * as DataGenerator from './EslintTestDataGenerator';
import { CUSTOM_CONFIG } from '../../../src/Constants';
import Mockito = require('ts-mockito');
import { FileHandler } from '../../../src/lib/util/FileHandler';
import { StaticDependencies } from '../../../src/lib/eslint/EslintCommons';
import { Messages } from '@salesforce/core';
import { ESLint } from 'eslint';
import { ENGINE } from '../../../src/Constants';

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'CustomEslintEngine');

describe("Tests for CustomEslintEngine", () => {

	const configFilePath = '/some/file/path/config.json';
	const engineOptionsWithEslintCustom = new Map<string, string>([
		[CUSTOM_CONFIG.EslintConfig, configFilePath]
	]);
	const engineOptionsWithPmdCustom = new Map<string, string>([
		[CUSTOM_CONFIG.PmdConfig, configFilePath]
	]);

	const emptyEngineOptions = new Map<string, string>();

	describe("Testing shouldEngineRun()", () => {

		const customEslintEngine = new CustomEslintEngine();

		before(async () => {
			await customEslintEngine.init();
		});

		it("should decide to run if EngineOptions has custom config for eslint and target is not empty", async () => {

			const shouldEngineRun = customEslintEngine.shouldEngineRun(
				[],
				[],
				[DataGenerator.getDummyTarget()],
				engineOptionsWithEslintCustom);

			expect(shouldEngineRun).to.be.true;
		});

		it("should decide to not run if EngineOptions is empty", async () => {

			const shouldEngineRun = customEslintEngine.shouldEngineRun(
				[],
				[],
				[DataGenerator.getDummyTarget()],
				emptyEngineOptions);

			expect(shouldEngineRun).to.be.false;
		});

		it("should decide to not run if target is empty", async () => {

			const shouldEngineRun = customEslintEngine.shouldEngineRun(
				[],
				[],
				[],
				engineOptionsWithEslintCustom);

			expect(shouldEngineRun).to.be.false;
		});
	});

	describe('Testing isEngineRequested()', () => {
		const engine = new CustomEslintEngine();

		before(async () => {
			await engine.init();
		});

		it('should return true when custom config is present and filter contains "eslint"', () => {
			const filteredValues = ['pmd','eslint'];

			const isEngineRequested = engine.isEngineRequested(filteredValues, engineOptionsWithEslintCustom);

			expect(isEngineRequested).to.be.true;
		});

		it('should return true when custom config is present and filter starts with "eslint"', () => {
			const filteredValues = ['eslint-lwc'];

			const isEngineRequested = engine.isEngineRequested(filteredValues, engineOptionsWithEslintCustom);

			expect(isEngineRequested).to.be.true;
		});

		it('should return false when custom config is not present even if filter contains "eslint"', () => {
			const filteredValues = ['pmd','eslint', 'retire-js'];

			const isEngineRequested = engine.isEngineRequested(filteredValues, emptyEngineOptions);

			expect(isEngineRequested).to.be.false;
		});

		it('should return false when custom config is present but filter does not contain "eslint"', () => {
			const filteredValues = ['pmd','retire-js'];

			const isEngineRequested = engine.isEngineRequested(filteredValues, engineOptionsWithEslintCustom);

			expect(isEngineRequested).to.be.false;
		});

		it('should return false when only pmd custom config is present even if filter contains "eslint"', () => {
			const filteredValues = ['pmd','eslint'];

			const isEngineRequested = engine.isEngineRequested(filteredValues, engineOptionsWithPmdCustom);

			expect(isEngineRequested).to.be.false;
		});

		it('should return false when filter is empty and engineOptions is empty', () => {
			const filteredValues = [];

			const isEngineRequested = engine.isEngineRequested(filteredValues, emptyEngineOptions);

			expect(isEngineRequested).to.be.false;
		});

		it('should return true when filter is empty and engineOptions contains eslintconfig', () => {
			const filteredValues = [];

			const isEngineRequested = engine.isEngineRequested(filteredValues, engineOptionsWithEslintCustom);

			expect(isEngineRequested).to.be.true;
		});

	});

	describe('Testing getCatalog()', () => {

		it('should return empty catalog', async () => {
			const customEslintEngine = new CustomEslintEngine();
			await customEslintEngine.init();

			const catalog = await customEslintEngine.getCatalog();

			expect(catalog).to.be.not.null;
			expect(catalog.categories).to.be.empty;
			expect(catalog.rules).to.be.empty;
			expect(catalog.rulesets).to.be.empty;
		});
	});

	describe('Testing run()', () => {

		it('Throws error when config file path is invalid', async () => {
			const fileHandlerMock = Mockito.mock(FileHandler);
			Mockito.when(fileHandlerMock.exists(configFilePath)).thenResolve(false);

			const customEslintEngine = await getCustomEslintEngine(fileHandlerMock);

			try {
				await customEslintEngine.run(
					[],
					[],
					[DataGenerator.getDummyTarget()],
					engineOptionsWithEslintCustom);

				// TODO: throw failure when exception is not thrown
			} catch (err) {
				expect(err.message).to.equal(messages.getMessage('ConfigFileDoesNotExist', [configFilePath]));
			}

		});

		it('Throws error when JSON in config file cannot be parsed', async () => {
			const target = DataGenerator.getDummyTarget();
			const invalidJsonContent = '{"someProperty": "someValue"'; //intentionally missing end braces
			const fileHandlerMock = mockFileHandlerToReturnContentForFile(configFilePath, invalidJsonContent);
			const customEslintEngine = await getCustomEslintEngine(fileHandlerMock);

			try {

				await customEslintEngine.run(
					[],
					[],
					[target],
					engineOptionsWithEslintCustom);

				// TODO: throw failure when exception is not thrown
			} catch (err) {
				expect(err.message).contains(`Something in the ESLint config JSON is invalid. Check ESLint's JSON specifications: ${configFilePath}.`);
			}

		});

		it('Invokes Eslint when config can be fetched', async () => {
			const target = DataGenerator.getDummyTarget();
			const invalidJsonContent = '{"someProperty": "someValue"}';
			const fileHandlerMock = mockFileHandlerToReturnContentForFile(configFilePath, invalidJsonContent);
			const eslintMock = mockEslint(target.paths, [DataGenerator.getDummyEsResult()]);

			const customEslintEngine = await getCustomEslintEngine(fileHandlerMock, eslintMock);

			const results = await customEslintEngine.run(
								[],
								[],
								[target],
								engineOptionsWithEslintCustom);

			expect(results).is.not.null;
			const result = results.pop();
			expect(result.engine).equals(ENGINE.ESLINT_CUSTOM.valueOf());

		});
	});
});


function mockFileHandlerToReturnContentForFile(configFilePath: string, invalidJsonContent: string) {
	const fileHandlerMock = Mockito.mock(FileHandler);
	Mockito.when(fileHandlerMock.exists(configFilePath)).thenResolve(true);
	Mockito.when(fileHandlerMock.readFile(configFilePath)).thenResolve(invalidJsonContent);
	return fileHandlerMock;
}

function mockEslint(paths: string[] = DataGenerator.getDummyTarget().paths, results: ESLint.LintResult[] = [DataGenerator.getDummyEsResult()]) {
	const ESLintMock: ESLint = Mockito.mock(ESLint);
	Mockito.when(ESLintMock.lintFiles(Mockito.anything())).thenReturn(Promise.resolve(results));
	Mockito.when(ESLintMock.getRulesMetaForResults(Mockito.anything())).thenReturn({});
	return ESLintMock;
}

async function getCustomEslintEngine(
	fileHandlerMock: FileHandler = Mockito.mock(FileHandler),
	eslintMock: ESLint = Mockito.mock(ESLint)) {

	const staticDependenciesMock = Mockito.mock(StaticDependencies);
	Mockito.when(staticDependenciesMock.getFileHandler()).thenReturn(Mockito.instance(fileHandlerMock));
	Mockito.when(staticDependenciesMock.createESLint(Mockito.anything())).thenReturn(Mockito.instance(eslintMock));

	const customEslintEngine = new CustomEslintEngine();
	await customEslintEngine.init(Mockito.instance(staticDependenciesMock));
	return customEslintEngine;
}

