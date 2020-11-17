import { expect } from 'chai';
import { CustomEslintEngine } from '../../../src/lib/eslint/CustomEslintEngine';
import * as DataGenerator from './EslintTestDataGenerator';
import { CUSTOM_CONFIG } from '../../../src/Constants';
import Mockito = require('ts-mockito');
import { FileHandler } from '../../../src/lib/util/FileHandler';
import { StaticDependencies } from '../../../src/lib/eslint/EslintProcessHelper';
import { Messages } from '@salesforce/core';
import { CLIEngine } from 'eslint';
import { ESReport, ESRule } from '../../../src/types';
import { ENGINE } from '../../../src/Constants';

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'CustomEslintEngine');

describe("Tests for CustomEslintEngine", () => {

	const configFilePath = '/some/file/path/config.json';
	const engineOptionsWithEslintCustom = new Map<string, string>([
		[CUSTOM_CONFIG.EslintConfig, configFilePath]
	]);

	const emptyEngineOptions = new Map<string, string>();

	describe("Testing shouldEngineRun()", () => {

		it("should decide to run if EngineOptions has custom config for eslint and target is not empty", async () => {
			const customEslintEngine = new CustomEslintEngine();
			await customEslintEngine.init();

			const shouldEngineRun = customEslintEngine.shouldEngineRun(
				[],
				[],
				[DataGenerator.getDummyTarget()],
				engineOptionsWithEslintCustom);

			expect(shouldEngineRun).to.be.true;
		});

		it("should decide to not run if EngineOptions is empty", async () => {
			const customEslintEngine = new CustomEslintEngine();
			await customEslintEngine.init();

			const shouldEngineRun = customEslintEngine.shouldEngineRun(
				[],
				[],
				[DataGenerator.getDummyTarget()],
				emptyEngineOptions);

			expect(shouldEngineRun).to.be.false;
		});

		it("should decide to not run if target is empty", async () => {
			const customEslintEngine = new CustomEslintEngine();
			await customEslintEngine.init();

			const shouldEngineRun = customEslintEngine.shouldEngineRun(
				[],
				[],
				[],
				engineOptionsWithEslintCustom);

			expect(shouldEngineRun).to.be.false;
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
				expect(err.message).contains(`Error while reading JSON in Eslint config (${configFilePath})`);
			}

		});

		it('Invokes Eslint when config can be fetched', async () => {
			const target = DataGenerator.getDummyTarget();
			const report = DataGenerator.getDummyEsReport();
			const invalidJsonContent = '{"someProperty": "someValue"}';
			const fileHandlerMock = mockFileHandlerToReturnContentForFile(configFilePath, invalidJsonContent);
			const cliEngineMock = mockCliEngine(target.paths, report);

			const customEslintEngine = await getCustomEslintEngine(fileHandlerMock, cliEngineMock);

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

function mockCliEngine(
	paths: string[] = DataGenerator.getDummyTarget().paths, 
	report: ESReport = DataGenerator.getDummyEsReport()) {

		const esRuleMap = new Map<string, ESRule>();
	const CLIEngineMock: typeof CLIEngine = Mockito.mock(CLIEngine);

	Mockito.when(CLIEngineMock.executeOnFiles(Mockito.anything())).thenReturn(report);
	Mockito.when(CLIEngineMock.getRules()).thenReturn(esRuleMap);

	return CLIEngineMock;
}

async function getCustomEslintEngine(
	fileHandlerMock: FileHandler = Mockito.mock(FileHandler),
	cliEngineMock: typeof CLIEngine = Mockito.mock(typeof CLIEngine)) {

	const staticDependenciesMock = Mockito.mock(StaticDependencies);
	Mockito.when(staticDependenciesMock.getFileHandler()).thenReturn(Mockito.instance(fileHandlerMock));
	Mockito.when(staticDependenciesMock.createCLIEngine(Mockito.anything())).thenReturn(Mockito.instance(cliEngineMock));

	const customEslintEngine = new CustomEslintEngine();
	await customEslintEngine.init(Mockito.instance(staticDependenciesMock));
	return customEslintEngine;
}

