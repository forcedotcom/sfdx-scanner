import path = require('path');
import {Logger} from '@salesforce/core';
import {expect} from 'chai';

import {Results} from '../../../src/lib/output/Results';
import {Inputs, RuleResult} from '../../../src/types';
import {RunAction} from '../../../src/lib/actions/RunAction';
import {InputProcessorImpl} from '../../../src/lib/InputProcessor';
import {RuleFilterFactoryImpl} from '../../../src/lib/RuleFilterFactory';
import {RunEngineOptionsFactory} from '../../../src/lib/EngineOptionsFactory';

import {initializeTestSetup} from '../../test-related-lib/TestOverrides';
import {FakeDisplay} from '../FakeDisplay';
import {FakeResultsProcessorFactory, RawResultsProcessor} from './fakes';


describe('Misc scanner run tests', () => {
	const PATH_TO_CODE_FIXTURES = path.join('.', 'test', 'code-fixtures');

	let logger: Logger;
	let display: FakeDisplay;
	let inputProcessor: InputProcessorImpl;
	let resultsProcessor: RawResultsProcessor;
	let runAction: RunAction;

	beforeEach(async () => {
		initializeTestSetup();
		logger = await Logger.child('RunAction.test.ts');
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

	it('We can successful call the retire engine and get a violation', async () => {
		const inputs: Inputs = {
			target: [path.join(PATH_TO_CODE_FIXTURES, 'projects', 'dep-test-app', 'folder-a')],
			engine: ['retire-js']
		};
		await runAction.run(inputs);

		const results: Results = resultsProcessor.getResults();
		let ruleResults: RuleResult[] = results.getRuleResults();
		expect(ruleResults).to.have.length(1);
		expect(ruleResults[0].engine).to.equal('retire-js');
		expect(ruleResults[0].violations).to.have.length(1);
		expect(ruleResults[0].violations[0].ruleName).to.equal('insecure-bundled-dependencies');
	});

	it('Our eslint and eslint-lwc engines should not complain about the aura namespace $A variable (W-15080616)', async () => {
		const inputs: Inputs = {
			target: [path.join(PATH_TO_CODE_FIXTURES, 'projects', 'app', 'force-app', 'main', 'default', 'aura', 'AccountRepeat')],
			engine: ['eslint', 'eslint-lwc']
		};
		await runAction.run(inputs);

		for (let ruleResult of resultsProcessor.getResults().getRuleResults()) {
			for (let violation of ruleResult.violations) {
				if (violation.ruleName == 'no-undef' && violation.message.includes("$A")) {
					expect.fail("The " + ruleResult.engine + " engine complained about $A being undefined when it shouldn't have. Message:\n"  + violation.message)
				}
			}
		}

	})
});
