import { BaseEslintEngine, StaticDependencies, EslintStrategy } from "../../../src/lib/eslint/BaseEslintEngine";
import { Rule, RuleGroup, RuleTarget, ESRule, ESResult, ESMessage, ESReport } from '../../../src/types';
import { expect } from 'chai';
import { CLIEngine } from 'eslint';
import Mockito = require('ts-mockito');
import * as TestOverrides from '../../test-related-lib/TestOverrides';

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
const EMPTY_ENGINE_OPTIONS = new Map<string, string>();

describe('Tests for BaseEslintEngine', () => {
	describe('Tests for run()', () => {
		describe('Related to target input', () => {

			afterEach(() => {
				Mockito.reset(MockStrategy);
			});

			it('should not execute when target is empty', async () => {

				//instantiate abstract engine
				const mockStrategy = Mockito.instance(MockStrategy);
				const engine = await createDummyEngine(mockStrategy);

				const results = await engine.run(
					[getDummyRuleGroup()],
					[getDummyRule()],
					[], // no target
					EMPTY_ENGINE_OPTIONS
				);

				expect(results).to.be.empty;
			});

			it('should use target as current working directory if target is a directory', async () => {
				const isDir = true;
				const target = getDummyTarget(isDir);

				const StaticDependenciesMock = mockStaticDependencies(target, getDummyCliEngine());

				// instantiate abstract engine
				const engine = await createAbstractEngine(target, StaticDependenciesMock);

				await engine.run(
					[getDummyRuleGroup()],
					[getDummyRule()],
					[target],
					EMPTY_ENGINE_OPTIONS
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
				Mockito.when(MockStrategy.getLanguages()).thenReturn(['language']);
				const mockStrategy = Mockito.instance(MockStrategy);
				const engine = await createDummyEngine(mockStrategy);

				const results = await engine.run(
					[getDummyRuleGroup()],
					[], // no rules
					[getDummyTarget(true)],
					EMPTY_ENGINE_OPTIONS
				);

				expect(results).to.be.empty;
			});
		});

		describe('Rule mapping', () => {

				afterEach(() => {
					Mockito.reset(MockStrategy);
				});

				it('should map Eslint-rule to sfdx scanner rule structure', async () => {
					const target = getDummyTarget();

					const ruleId = 'ruleId';
					const category = 'myCategory';
					const description = 'rule description';
					const message = 'this is a message';
					const esRuleMap = getDummyEsRuleMap(ruleId, category, description);
					const esReport = getDummyEsReport([getDummyEsResult([getDummyEsMessage(ruleId, message)])]);

					const cliEngineMock = getDummyCliEngine(esRuleMap, esReport);
					const StaticDependenciesMock = mockStaticDependencies(target, cliEngineMock);
					const engine = await createAbstractEngine(target, StaticDependenciesMock);

					const results = await engine.run(
						[getDummyRuleGroup()],
						[getDummyRule()],
						[target],
						EMPTY_ENGINE_OPTIONS
					);

					// verify results structure and content
					expect(results.length).greaterThan(0);
					const result = results[0];

					expect(result.engine).equals(engineName);
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
				const target = getDummyTarget();

				const ruleId = 'ruleId';
				const category = 'myCategory';
				const description = 'some lengthy description';

				const esRuleMap = getDummyEsRuleMap(ruleId, category, description);
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
				const target = getDummyTarget();
				const category = 'myCategory';
				const esRuleMap = new Map<string, ESRule>();

				const ruleId1 = 'ruleId1';
				const description1 = 'some lengthy description';
				const esRule1 = getDummyEsRule(category, description1);
				esRuleMap.set(ruleId1, esRule1);

				const ruleId2 = 'ruleId2';
				const description2 = 'some lengthy description';
				const esRule2 = getDummyEsRule(category, description2);
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

	const engine = await createDummyEngine(Mockito.instance(MockStrategy), Mockito.instance(StaticDependenciesMock));
	return engine;
}

async function createDummyEngine(strategy: EslintStrategy, baseDependencies = new StaticDependencies()) {
	const engine = new TestHarnessEngine();
	await engine.initializeContents(strategy, baseDependencies);
	return engine;
}

function getDummyCliEngine(esRuleMap: Map<string, ESRule> = getDummyEsRuleMap(), esReport: ESReport = getDummyEsReport()): CLIEngine {
	const CLIEngineMock: CLIEngine = Mockito.mock(CLIEngine);

	Mockito.when(CLIEngineMock.getRules()).thenReturn(esRuleMap);
	Mockito.when(CLIEngineMock.executeOnFiles(Mockito.anything())).thenReturn(esReport);

	return Mockito.instance(CLIEngineMock);
}

function getDummyRuleGroup(): RuleGroup {
	return { engine: engineName, name: "Group name", paths: ['/some/random/path'] };
}

function getDummyRule(myEngineName = engineName): Rule {
	return {
		engine: myEngineName,
		name: "MyTestRule",
		description: "my test rule",
		categories: ["some category"],
		languages: ["language"],
		sourcepackage: "MySourcePackage",
		rulesets: [],
		defaultEnabled: true
	}
}

function getDummyTarget(isDir: boolean = true): RuleTarget {
	return {
		target: "/some/target",
		isDirectory: isDir,
		paths: ['/some/target/path1', '/some/target/path2']
	}
}

function getDummyEsRuleMap(ruleId: string = 'ruleId', category: string = 'myCategory', description: string = 'my description'): Map<string, ESRule> {
	const map = new Map<string, ESRule>();
	map.set(ruleId, getDummyEsRule(category, description));
	return map;
}

function getDummyEsRule(category: string = 'myCategory', description: string = 'my description'): ESRule {
	return {
		meta: {
			docs: {
				description: description,
				category: category,
				recommended: true,
				url: 'someURL'
			},
			schema: [{
				element: 'value'
			}]
		},
		create: () => { }
	}
}

function getDummyEsReport(results: ESResult[] = [getDummyEsResult()]): ESReport {
	return {
		results: results,
		errorCount: 0,
		warningCount: 0,
		fixableErrorCount: 0,
		fixableWarningCount: 0,
		usedDeprecatedRules: []
	}
}

function getDummyEsResult(messages: ESMessage[] = [getDummyEsMessage()]): ESResult {
	return {
		filePath: "filePath",
		messages: messages
	};
}

function getDummyEsMessage(ruleId: string = 'rule', message: string = 'message'): ESMessage {
	return {
		fatal: true,
		ruleId: ruleId,
		severity: 2,
		line: 35,
		column: 7,
		message: message,
		fix: {
			range: [23, 78],
			text: "some fix string"
		}
	}
}
