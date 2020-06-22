import { BaseEslintEngine, StaticDependencies, EslintStrategy } from "../../../src/lib/eslint/BaseEslintEngine";
import { Rule, RuleGroup, RuleTarget, /*ESRule, ESReport*/ } from '../../../src/types';
import { expect } from 'chai';
// import { CLIEngine } from 'eslint';
import Mockito = require('ts-mockito');


const engineName = 'TestHarnessEngine';
class TestHarnessEngine extends BaseEslintEngine {
	public init(): Promise<void> {
		throw new Error("Method not implemented.");
	}

	public async initializeContents(strategy: EslintStrategy, baseDependencies: StaticDependencies) {
		await super.initializeContents(strategy, baseDependencies);
	}
}

const MockStrategy: EslintStrategy = Mockito.mock<EslintStrategy>();

describe('Tests for BaseEslintEngine', () => {
	describe('Tests for run()', () => {
		describe('Related to target input', () => {

			afterEach(() => {
				Mockito.reset(MockStrategy);
			});

			it('should not execute when target is empty', async () => {

				//instantiate abstract engine
				Mockito.when(MockStrategy.getName()).thenReturn(engineName);
				const mockStrategy = Mockito.instance(MockStrategy);
				const engine = await createDummyEngine(mockStrategy);

				const results = await engine.run(
					[getDummyRuleGroup()],
					[getDummyRule()],
					[]); // no target

				expect(results).to.be.empty;
			});

			// TODO: test in progress
			// it('should use target as current working directory if target is a directory', async () => {
			// 	const isDir = true;
			// 	const target = getDummyTarget(isDir);
			// 	const cliEngineMock = getDummyCliEngine([],[]);

			// 	const StaticDependenciesMock = Mockito.mock(StaticDependencies);
			// 	Mockito.when(StaticDependenciesMock.resolveTargetPath(target.target)).thenReturn(target.target);
			// 	Mockito.when(StaticDependenciesMock.createCLIEngine(Mockito.anyOfClass(Object))).thenReturn(cliEngineMock);

			// 	const rule = getDummyRule();

			// 	// instantiate abstract engine
			// 	Mockito.when(MockStrategy.getName()).thenReturn(engineName);
			// 	Mockito.when(MockStrategy.filterUnsupportedPaths(target.paths)).thenReturn(target.paths);
			// 	const engine = await createDummyEngine(Mockito.instance(MockStrategy), Mockito.instance(StaticDependenciesMock));

			// 	await engine.run(
			// 		[getDummyRuleGroup()],
			// 		[rule],
			// 		[target]
			// 	);

			// 	Mockito.verify(StaticDependenciesMock.resolveTargetPath(target.target)).called();

			// });
		});

		describe('Related to rules input', () => {

			afterEach(() => {
				Mockito.reset(MockStrategy);
			});

			it('should not execute when rules are empty', async () => {
				// instantiate abstract engine
				Mockito.when(MockStrategy.getName()).thenReturn(engineName);
				const mockStrategy = Mockito.instance(MockStrategy);
				const engine = await createDummyEngine(mockStrategy);

				const results = await engine.run(
					[getDummyRuleGroup()],
					[], // no rules
					[getDummyTarget(true)]);

				expect(results).to.be.empty;
			});

			it('should not execute when no rules are relevant', async () => {
				const differentEngine = 'differentEngineName';
				const irrelevantRule = getDummyRule(differentEngine);

				Mockito.when(MockStrategy.getName()).thenReturn(engineName);
				const mockStrategy = Mockito.instance(MockStrategy);
				const engine = await createDummyEngine(mockStrategy);

				const results = await engine.run(
					[getDummyRuleGroup()],
					[irrelevantRule],
					[getDummyTarget(true)]);

				expect(results).to.be.empty;
			});

		});
	});
});

async function createDummyEngine(strategy: EslintStrategy, baseDependencies = new StaticDependencies()) {
	const engine = new TestHarnessEngine();
	await engine.initializeContents(strategy, baseDependencies);
	return engine;
}

// function getDummyCliEngine(esRules?: ESRule[], esReport?: ESReport[]): CLIEngine {
// 	const CLIEngineMock: CLIEngine = Mockito.mock(CLIEngine);

// 	Mockito.when(CLIEngineMock.getRules()).thenReturn(esRules);
// 	Mockito.when(CLIEngineMock.executeOnFiles(Mockito.anything)).thenReturn(esReport);

// 	return Mockito.instance(CLIEngineMock);
// }

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

function getDummyTarget(isDir: boolean): RuleTarget {
	return {
		target: "/some/target",
		isDirectory: isDir,
		paths: ['/some/target/path1', '/some/target/path2']
	}
}
