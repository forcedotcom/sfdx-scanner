import { BaseEslintEngine, EslintStrategy } from "../../../src/lib/eslint/BaseEslintEngine";
import {StaticDependencies} from "../../../src/lib/eslint/EslintCommons";
import { RuleTarget, ESRule, ESReport, RuleViolation } from '../../../src/types';
import { expect } from 'chai';
import { CLIEngine } from 'eslint';
import {CUSTOM_CONFIG} from '../../../src/Constants';
import Mockito = require('ts-mockito');
import * as TestOverrides from '../../test-related-lib/TestOverrides';
import * as DataGenerator from './EslintTestDataGenerator';

TestOverrides.initializeTestSetup();

const engineName = 'TestHarnessEngine';
class TestHarnessEngine extends BaseEslintEngine {
	public init(): Promise<void> {
		throw new Error("Method not implemented.");
	}

	public async initializeContents(strategy: EslintStrategy, baseDependencies: StaticDependencies) {
		await super.initializeContents(strategy, baseDependencies);
	}

	public getName(): string {
		return engineName;
	}
}

const MockStrategy: EslintStrategy = Mockito.mock<EslintStrategy>();
const emptyEngineOptions = new Map<string, string>();

const configFilePath = '/some/file/path/config.json';
const engineOptionsWithEslintCustom = new Map<string, string>([
	[CUSTOM_CONFIG.EslintConfig, configFilePath]
]);
const engineOptionsWithPmdCustom = new Map<string, string>([
	[CUSTOM_CONFIG.PmdConfig, configFilePath]
]);

describe('Tests for BaseEslintEngine', () => {
	describe('Tests for shouldEngineRun()', () => {
		afterEach(() => {
			Mockito.reset(MockStrategy);
		});

		it('should decide to not run when target is empty', async () => {
			//instantiate abstract engine
			const mockStrategy = Mockito.instance(MockStrategy);
			const engine = await createDummyEngine(mockStrategy);

			const shouldEngineRun = engine.shouldEngineRun(
				[DataGenerator.getDummyRuleGroup()],
				[DataGenerator.getDummyRule()],
				[], // no target
				emptyEngineOptions
			);

			expect(shouldEngineRun).to.be.false;
		});

		it('should decide to not run when rules are empty', async () => {
			//instantiate abstract engine
			const mockStrategy = Mockito.instance(MockStrategy);
			const engine = await createDummyEngine(mockStrategy);

			const shouldEngineRun = engine.shouldEngineRun(
				[DataGenerator.getDummyRuleGroup()],
				[], //no rules
				[DataGenerator.getDummyTarget()],
				emptyEngineOptions
			);

			expect(shouldEngineRun).to.be.false;
		});

		it('should decide to not run when EngineOptions has eslint custom config', async () => {
			//instantiate abstract engine
			const mockStrategy = Mockito.instance(MockStrategy);
			const engine = await createDummyEngine(mockStrategy);

			const engineOptions = new Map<string, string>();
			engineOptions.set(CUSTOM_CONFIG.EslintConfig, '/some/dummy/path');

			const shouldEngineRun = engine.shouldEngineRun(
				[DataGenerator.getDummyRuleGroup()],
				[DataGenerator.getDummyRule()],
				[DataGenerator.getDummyTarget()],
				engineOptions
			);

			expect(shouldEngineRun).to.be.false;
		});

		it('should decide to run when target, rules and options look right', async () => {
			//instantiate abstract engine
			const mockStrategy = Mockito.instance(MockStrategy);
			const engine = await createDummyEngine(mockStrategy);

			const shouldEngineRun = engine.shouldEngineRun(
				[DataGenerator.getDummyRuleGroup()],
				[DataGenerator.getDummyRule()],
				[DataGenerator.getDummyTarget()],
				emptyEngineOptions
			);

			expect(shouldEngineRun).to.be.true;
		});

	});

	describe('Tests for run()', () => {
		describe('Related to target input', () => {

			afterEach(() => {
				Mockito.reset(MockStrategy);
			});

			it('should use target as current working directory if target is a directory', async () => {
				const isDir = true;
				const target = DataGenerator.getDummyTarget(isDir);

				const StaticDependenciesMock = mockStaticDependencies(target, getDummyCliEngine());

				// instantiate abstract engine
				const engine = await createAbstractEngine(target, StaticDependenciesMock);

				await engine.run(
					[DataGenerator.getDummyRuleGroup()],
					[DataGenerator.getDummyRule()],
					[target],
					emptyEngineOptions
				);

				Mockito.verify(StaticDependenciesMock.resolveTargetPath(target.target)).called();

				// verify config
				const capturedConfig = Mockito.capture(StaticDependenciesMock.createCLIEngine).second();
				expect(capturedConfig[0]).instanceOf(Object);
				const config = <Object>capturedConfig[0];
				expect(config['cwd']).equals(target.target);
			});
		});

		describe('Related to rules input', () => {

			afterEach(() => {
				Mockito.reset(MockStrategy);
			});

			it('should not execute when rules are empty', async () => {
				// instantiate abstract engine
				const mockStrategy = Mockito.instance(MockStrategy);
				const engine = await createDummyEngine(mockStrategy);

				const results = await engine.run(
					[DataGenerator.getDummyRuleGroup()],
					[], // no rules
					[DataGenerator.getDummyTarget(true)],
					emptyEngineOptions
				);

				expect(results).to.be.empty;
			});
		});

		describe('Rule mapping', () => {

				afterEach(() => {
					Mockito.reset(MockStrategy);
				});

				it('should map Eslint-rule to sfdx scanner rule structure', async () => {
					const target = DataGenerator.getDummyTarget();

					const ruleId = 'ruleId';
					const category = 'myCategory';
					const description = 'rule description';
					const message = 'this is a message';
					const esRuleMap = DataGenerator.getDummyEsRuleMap(ruleId, category, description);
					const esReport = DataGenerator.getDummyEsReport([DataGenerator.getDummyEsResult([DataGenerator.getDummyEsMessage(ruleId, message)])]);

					const cliEngineMock = getDummyCliEngine(esRuleMap, esReport);
					const StaticDependenciesMock = mockStaticDependencies(target, cliEngineMock);
					const engine = await createAbstractEngine(target, StaticDependenciesMock);

					const results = await engine.run(
						[DataGenerator.getDummyRuleGroup()],
						[DataGenerator.getDummyRule()],
						[target],
						emptyEngineOptions
					);

					// verify results structure and content
					expect(results.length).greaterThan(0);
					const result = results[0];

					// TODO: verify engineName - right now, unless we use a real ENGINE enum type, this won't work
					expect(result.fileName).equals(esReport.results[0].filePath);
					expect(result.violations.length).greaterThan(0);
					const violation = result.violations[0];
					expect(violation.ruleName).equals(ruleId);
					expect(violation.message).equals(message);
					expect(violation.category).equals(category);
				});



		});
	});

	describe('Tests for getCatalog()', () => {
		describe('Related to mapping all rules to Catalog', () => {
			afterEach(() => {
				Mockito.reset(MockStrategy);
			});

			it('should map ESRules to Catalog', async () => {
				const target = DataGenerator.getDummyTarget();

				const ruleId = 'ruleId';
				const category = 'myCategory';
				const description = 'some lengthy description';

				const esRuleMap = DataGenerator.getDummyEsRuleMap(ruleId, category, description);
				const cliEngineMock = getDummyCliEngine(esRuleMap);
				const StaticDependenciesMock = mockStaticDependencies(target, cliEngineMock);
				const engine = await createAbstractEngine(target, StaticDependenciesMock);

				// execute
				const catalog = await engine.getCatalog();

				//verify
				expect(catalog.categories.length).equals(1);
				const catalogCategory = catalog.categories[0];
				expect(catalogCategory.engine).equals(engineName);
				expect(catalogCategory.name).equals(category);

				expect(catalog.rules.length).equals(1);
				const catalogRule = catalog.rules[0];
				expect(catalogRule.name).equals(ruleId);
				expect(catalogRule.description).equals(description);
			});

			it('should add rule to an existing category if applicable', async () => {
				const target = DataGenerator.getDummyTarget();
				const category = 'myCategory';
				const esRuleMap = new Map<string, ESRule>();

				const ruleId1 = 'ruleId1';
				const description1 = 'some lengthy description';
				const esRule1 = DataGenerator.getDummyEsRule(category, description1);
				esRuleMap.set(ruleId1, esRule1);

				const ruleId2 = 'ruleId2';
				const description2 = 'some lengthy description';
				const esRule2 = DataGenerator.getDummyEsRule(category, description2);
				esRuleMap.set(ruleId2, esRule2);

				const cliEngineMock = getDummyCliEngine(esRuleMap);
				const StaticDependenciesMock = mockStaticDependencies(target, cliEngineMock);
				const engine = await createAbstractEngine(target, StaticDependenciesMock);

				// execute
				const catalog = await engine.getCatalog();

				// verify
				expect(catalog.categories.length).equals(1);
				expect(catalog.categories[0].name).equals(category);
				expect(catalog.rules.length).equals(2, 'Rules with the same category string should be grouped together in the catalog');


			});
		});
	});

	describe('Tests for shouldEngineRun()', () => {

		const mockStrategy = Mockito.instance(MockStrategy);
		let engine;

		before(async () => {
			engine = await createDummyEngine(mockStrategy);
		});
		

		it ('should decide to run if custom config, rules and target are correct', () => {

			const shouldRunEngine = engine.shouldEngineRun(
				[],
				[DataGenerator.getDummyRule()],
				[DataGenerator.getDummyTarget()],
				emptyEngineOptions
			);

			expect(shouldRunEngine).to.be.true;
		});


		it ('should decide to not run if using custom config', () => {

			const shouldRunEngine = engine.shouldEngineRun(
				[],
				[DataGenerator.getDummyRule()],
				[DataGenerator.getDummyTarget()],
				engineOptionsWithEslintCustom
			);

			expect(shouldRunEngine).to.be.false;
		});

		it('should decide to not run if target paths is empty', () => {

			const shouldRunEngine = engine.shouldEngineRun(
				[],
				[DataGenerator.getDummyRule()],
				[],
				emptyEngineOptions
			);

			expect(shouldRunEngine).to.be.false;
		});

		it('should decide to not run if no rules are chosen', () => {

			const shouldRunEngine = engine.shouldEngineRun(
				[],
				[],
				[DataGenerator.getDummyTarget()],
				emptyEngineOptions
			);

			expect(shouldRunEngine).to.be.false;
		});

		it ('should decide to run if using custom config contains PMD but not Eslint', () => {

			const shouldRunEngine = engine.shouldEngineRun(
				[],
				[DataGenerator.getDummyRule()],
				[DataGenerator.getDummyTarget()],
				engineOptionsWithPmdCustom
			);

			expect(shouldRunEngine).to.be.true;
		});
	});

	describe('Tests for isEngineRequested()', () => {
		const mockStrategy = Mockito.instance(MockStrategy);
		let engine;

		before(async () => {
			engine = await createDummyEngine(mockStrategy);
		});

		it('should return true when custom config is not present and filter contains engine name', () => {
			const filteredNames = ['pmd', engine.getName(), 'retire-js'];

			const isEngineRequested = engine.isEngineRequested(filteredNames, emptyEngineOptions);

			expect(isEngineRequested).to.be.true;
		});

		it('should return false when custom config is present even if filter contains engine name', () => {
			const filteredNames = ['pmd', engine.getName(), 'retire-js'];

			const isEngineRequested = engine.isEngineRequested(filteredNames, engineOptionsWithEslintCustom);

			expect(isEngineRequested).to.be.false;	
		});

		it('should return false when custom config is not present but filter does not contain engine name', () => {
			const filteredNames = ['pmd', 'retire-js'];

			const isEngineRequested = engine.isEngineRequested(filteredNames, emptyEngineOptions);

			expect(isEngineRequested).to.be.false;	
		});

		it('should return false when custom config is not present and filter starts with "eslint"', () => {
			const filteredNames = ['pmd', 'retire-js', 'eslint-custom'];

			const isEngineRequested = engine.isEngineRequested(filteredNames, emptyEngineOptions);

			expect(isEngineRequested).to.be.false;	
		});

		it('should return true when only PMD custom config is present and filter contains engine name', () => {
			const filteredNames = ['pmd', engine.getName(), 'retire-js'];

			const isEngineRequested = engine.isEngineRequested(filteredNames, engineOptionsWithPmdCustom);

			expect(isEngineRequested).to.be.true;
		});
	});
});


/** HELPER FUNCTIONS TO KEEP TESTS EASIER TO READ */



function mockStaticDependencies(target: RuleTarget, cliEngineMock: any) {
	const StaticDependenciesMock = Mockito.mock(StaticDependencies);
	Mockito.when(StaticDependenciesMock.resolveTargetPath(target.target)).thenReturn(target.target);
	Mockito.when(StaticDependenciesMock.createCLIEngine(Mockito.anything())).thenReturn(cliEngineMock);
	return StaticDependenciesMock;
}

async function createAbstractEngine(target: RuleTarget, StaticDependenciesMock: StaticDependencies) {
	Mockito.when(MockStrategy.filterUnsupportedPaths(target.paths)).thenReturn(target.paths);
	Mockito.when(MockStrategy.getLanguages()).thenReturn(['language']);
	Mockito.when(MockStrategy.processRuleViolation()).thenReturn((filename: string, ruleViolation: RuleViolation)=> {
		//do nothing
	});

	const engine = await createDummyEngine(Mockito.instance(MockStrategy), Mockito.instance(StaticDependenciesMock));
	return engine;
}

async function createDummyEngine(strategy: EslintStrategy, baseDependencies = new StaticDependencies()) {
	const engine = new TestHarnessEngine();
	await engine.initializeContents(strategy, baseDependencies);
	return engine;
}

function getDummyCliEngine(esRuleMap: Map<string, ESRule> = DataGenerator.getDummyEsRuleMap(), esReport: ESReport = DataGenerator.getDummyEsReport()): typeof CLIEngine {
	const CLIEngineMock: typeof CLIEngine = Mockito.mock(CLIEngine);

	Mockito.when(CLIEngineMock.getRules()).thenReturn(esRuleMap);
	Mockito.when(CLIEngineMock.executeOnFiles(Mockito.anything())).thenReturn(esReport);

	return Mockito.instance(CLIEngineMock);
}

