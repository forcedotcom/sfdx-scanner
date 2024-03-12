import path = require('path');
import {Logger} from '@salesforce/core';
import {expect} from 'chai';
import tildify = require('tildify');

import {Results} from '../../../src/lib/output/Results';
import {Inputs, RuleResult} from '../../../src/types';
import {RunAction} from '../../../src/lib/actions/RunAction';
import {InputProcessorImpl} from '../../../src/lib/InputProcessor';
import {RuleFilterFactoryImpl} from '../../../src/lib/RuleFilterFactory';
import {RunEngineOptionsFactory} from '../../../src/lib/EngineOptionsFactory';

import {initializeTestSetup} from '../../test-related-lib/TestOverrides';
import {FakeDisplay} from '../FakeDisplay';
import {FakeResultsProcessorFactory, RawResultsProcessor} from './fakes';


describe('scanner run, targeting capabilities', () => {
	const PATH_TO_TEST_FOLDER = path.join('.', 'test', 'code-fixtures', 'apex');
	const PATH_TO_SOME_TEST_CLASS = path.join('.', 'test', 'code-fixtures', 'apex', 'SomeTestClass.cls');
	const PATH_TO_SOME_OTHER_TEST_CLASS = path.join('.', 'test', 'code-fixtures', 'apex', 'SomeOtherTestClass.cls');
	const PATH_TO_SOQL_IN_LOOP = path.join('.', 'test', 'code-fixtures', 'apex', 'SoqlInLoop.cls');

	let logger: Logger;
	let display: FakeDisplay;
	let inputProcessor: InputProcessorImpl;
	let resultsProcessor: RawResultsProcessor;
	let runAction: RunAction;

	beforeEach(async () => {
		initializeTestSetup();
		logger = await Logger.child('RunAction.target.test.ts');
		display = new FakeDisplay();
		inputProcessor = new InputProcessorImpl('testing', display);
		resultsProcessor = new RawResultsProcessor();
		runAction = new RunAction(
			logger,
			display,
			inputProcessor,
			new RuleFilterFactoryImpl(),
			new RunEngineOptionsFactory(inputProcessor),
			new FakeResultsProcessorFactory(resultsProcessor)
		);
	});

	describe('Test Case: Can target a single file...', () => {
		it('...with a relative path', async () => {
			// Prepare Input
			const target = [PATH_TO_SOME_TEST_CLASS];
			const inputs: Inputs = {
				target,
				engine: ['pmd'],
				category: ['Performance'],
				format: 'xml'
			};

			// Invoke tested method
			await runAction.run(inputs);

			// Assert against results
			const results: Results = resultsProcessor.getResults();
			assertPerformanceViolations(results, target.map(p => path.resolve(p)));
		});

		it('... with an absolute path', async () => {
			// Prepare Input
			const target = [path.resolve(PATH_TO_SOME_TEST_CLASS)];
			const inputs: Inputs = {
				target,
				engine: ['pmd'],
				category: ['Performance'],
				format: 'xml'
			};

			// Invoke tested method
			await runAction.run(inputs);

			// Assert against results
			const results: Results = resultsProcessor.getResults();
			assertPerformanceViolations(results, target);
		});

		it('With a tilde-style path', async () => {
			// Prepare Input
			const target = [tildify(path.resolve(PATH_TO_SOME_TEST_CLASS))];
			const inputs: Inputs = {
				target,
				engine: ['pmd'],
				category: ['Performance'],
				format: 'xml'
			};

			// Invoke tested method
			await runAction.run(inputs);

			// Assert against results
			const results: Results = resultsProcessor.getResults();
			assertPerformanceViolations(results, [path.resolve(PATH_TO_SOME_TEST_CLASS)]);
		});
	});

	it('Test Case: Can target a list of files', async () => {
		// Prepare Input
		const target = [PATH_TO_SOME_TEST_CLASS, PATH_TO_SOME_OTHER_TEST_CLASS];
		const inputs: Inputs = {
			target,
			engine: ['pmd'],
			category: ['Performance'],
			format: 'xml'
		};

		// Invoke tested method
		await runAction.run(inputs);

		// Assert against results
		const results: Results = resultsProcessor.getResults();
		assertPerformanceViolations(results, target.map(p => path.resolve(p)));
	});

	it('Test Case: Can target a whole folder', async () => {
		// Prepare Input
		const inputs: Inputs = {
			target: [PATH_TO_TEST_FOLDER],
			engine: ['pmd'],
			category: ['Performance'],
			format: 'xml'
		};

		// Invoke tested method
		await runAction.run(inputs);

		// Assert against results
		const expectedTargets = [PATH_TO_SOME_TEST_CLASS, PATH_TO_SOME_OTHER_TEST_CLASS, PATH_TO_SOQL_IN_LOOP].map(p => path.resolve(p));
		const results: Results = resultsProcessor.getResults();
		assertPerformanceViolations(results, expectedTargets);
	});

	it('Test Case: Can target a glob', async () => {
		// Prepare Input
		const inputs: Inputs = {
			target: [path.join(PATH_TO_TEST_FOLDER, 'Some*.cls')],
			engine: ['pmd'],
			category: ['Performance'],
			format: 'xml'
		};

		// Invoke tested method
		await runAction.run(inputs);

		// Assert against results
		const expectedTargets = [PATH_TO_SOME_TEST_CLASS, PATH_TO_SOME_OTHER_TEST_CLASS].map(p => path.resolve(p));
		const results: Results = resultsProcessor.getResults();
		assertPerformanceViolations(results, expectedTargets);
	});

	function assertPerformanceViolations(results: Results, fileList: string[]): void {
		expect(results.getExecutedEngines().size).to.equal(1, 'Wrong executedEngines count');
		expect(results.getExecutedEngines()).to.contain('pmd', 'Wrong engines executed');
		const ruleResults: RuleResult[] = results.getRuleResults();
		expect(ruleResults).to.have.length(fileList.length, 'Wrong number of results');
		for (const ruleResult of ruleResults) {
			const fullFileName = path.resolve(ruleResult.fileName);
			expect(fileList).to.contain(fullFileName, `Violations in unexpected file ${fullFileName}`);
			for (const violation of ruleResult.violations) {
				expect(violation.category).to.equal('Performance', `Wrong category of violation found in ${ruleResult.fileName}`);
			}
		}
	}
});
