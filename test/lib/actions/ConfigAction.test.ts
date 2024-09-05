import path from 'node:path';
import {CodeAnalyzerConfig} from "@salesforce/code-analyzer-core";

import {CodeAnalyzerConfigFactoryImpl} from "../../../src/lib/factories/CodeAnalyzerConfigFactory";
import {ConfigAction, ConfigDependencies, ConfigInput} from '../../../src/lib/actions/ConfigAction';

import {StubEnginePluginsFactory_withFunctionalStubEngine} from "../../stubs/StubEnginePluginsFactories";
import {SpyConfigModel} from '../../stubs/StubConfigModel';
import {SpyConfigViewer} from '../../stubs/SpyConfigViewer';
import {SpyConfigWriter} from '../../stubs/SpyConfigWriter';

describe('ConfigAction tests', () => {
	const ORIGINAL_DIRECTORY = process.cwd();

	let spyViewer: SpyConfigViewer;
	let dependencies: ConfigDependencies;

	beforeEach(() => {
		spyViewer = new SpyConfigViewer();
		dependencies = {
			logEventListeners: [],
			progressEventListeners: [],
			viewer: spyViewer,
			configFactory: new CodeAnalyzerConfigFactoryImpl(),
			modelGenerator: SpyConfigModel.fromSelection,
			pluginsFactory: new StubEnginePluginsFactory_withFunctionalStubEngine()
		};
	});

	afterEach(() => {
		// These tests will involve moving into a new directory, so we should make sure to move back into the original
		// directory after each test.
		process.chdir(ORIGINAL_DIRECTORY);
	});

	describe('When there is NOT an existing config...', () => {
		beforeEach(() => {
			const pathToTestDirectory = path.join(__dirname, '..', '..', 'fixtures', 'example-workspaces', 'ConfigAction.test.ts', 'folder-without-config');
			process.chdir(pathToTestDirectory);
		});

		it('The default configuration is used', async () => {
			await testScenario(dependencies, ['all'], 8, CodeAnalyzerConfig.withDefaults());
		});

		it('Rule selections properly synthesize with the default configuration', async () => {
			await testScenario(dependencies, ['codestyle'], 2, CodeAnalyzerConfig.withDefaults());
		});

		it('If a ConfigWriter is provided, it is used along with the ConfigViewer', async () => {
			// We need to add a Writer to the dependencies.
			const spyWriter = new SpyConfigWriter();
			dependencies.writer = spyWriter;

			await testScenario(dependencies, ['all'], 8, CodeAnalyzerConfig.withDefaults());

			// Make sure that the Writer was provided the right type of ConfigModel.
			const writerCallHistory = spyWriter.getCallHistory();
			expect(writerCallHistory).toHaveLength(1);
			expect(writerCallHistory[0]).toBeInstanceOf(SpyConfigModel);

			// Make sure that the Writer's ConfigModel was instantiated from the right things.
			const writtenSpyConfigModel: SpyConfigModel = writerCallHistory[0] as SpyConfigModel;
			// The engines we're using have a total of 8 rules.
			expect(writtenSpyConfigModel.getUserRuleSelection().getCount()).toEqual(8);
			expect(writtenSpyConfigModel.getUserConfig()).toEqual(CodeAnalyzerConfig.withDefaults());
		});
	});

	describe('When there IS an existing config...', () => {
		let expectedBaseConfig: CodeAnalyzerConfig;

		beforeEach(async () => {
			const pathToTestDirectory = path.join(__dirname, '..', '..', 'fixtures', 'example-workspaces', 'ConfigAction.test.ts', 'folder-with-config');
			process.chdir(pathToTestDirectory);
			expectedBaseConfig = CodeAnalyzerConfig.fromFile('code-analyzer.yml');
		});

		it('The existing configuration overrides the default', async () => {
			await testScenario(dependencies, ['all'], 8, expectedBaseConfig);
		});

		it('Rule selections properly synthesize with the existing configuration', async () => {
			await testScenario(dependencies, ['codestyle'], 3, expectedBaseConfig);
		});

		it('If a ConfigWriter is provided, it is used along with the ConfigViewer', async () => {
			// We need to add a Writer to the dependencies.
			const spyWriter = new SpyConfigWriter();
			dependencies.writer = spyWriter;

			await testScenario(dependencies, ['all'], 8, expectedBaseConfig);

			// Make sure that the Writer was provided the right type of ConfigModel
			const writerCallHistory = spyWriter.getCallHistory();
			expect(writerCallHistory).toHaveLength(1);
			expect(writerCallHistory[0]).toBeInstanceOf(SpyConfigModel);

			// Make sure that the Writer's ConfigModel was instantiated from the right things.
			const writtenSpyConfigModel: SpyConfigModel = writerCallHistory[0] as SpyConfigModel;
			// The engines we're using have a total of 8 rules.
			expect(writtenSpyConfigModel.getUserRuleSelection().getCount()).toEqual(8);
			expect(writtenSpyConfigModel.getUserConfig()).toEqual(expectedBaseConfig);
		});
	});

	async function testScenario(dependencies: ConfigDependencies, ruleSelectors: string[], expectedRuleCount: number, expectedBaseConfig: CodeAnalyzerConfig): Promise<void> {
		// ==== TEST SETUP ====
		const action = ConfigAction.createAction(dependencies);

		// ==== TESTED BEHAVIOR ====
		const input: ConfigInput = {
			'rule-selector': ruleSelectors
		};
		await action.execute(input);

		// ==== ASSERTIONS ====
		// Make sure the Viewer was provided the right type of ConfigModel.
		const viewerCallHistory = spyViewer.getCallHistory();
		expect(viewerCallHistory).toHaveLength(1);
		expect(viewerCallHistory[0]).toBeInstanceOf(SpyConfigModel);

		// Make sure that the Viewer's ConfigModel was instantiated from the right things.
		const spyConfigModel: SpyConfigModel = viewerCallHistory[0] as SpyConfigModel;
		// The User state should depend on the user's input.
		expect(spyConfigModel.getUserRuleSelection().getCount()).toEqual(expectedRuleCount);
		expect(spyConfigModel.getUserConfig()).toEqual(expectedBaseConfig);
		// The Default state should always be the same, using the Default config and selecting All rules.
		expect(spyConfigModel.getDefaultRuleSelection().getCount()).toEqual(8);
		expect(spyConfigModel.getDefaultConfig()).toEqual(CodeAnalyzerConfig.withDefaults());
	}
})
