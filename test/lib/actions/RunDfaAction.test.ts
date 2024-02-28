import {Logger} from '@salesforce/core';
import {assert, expect} from 'chai';
import path = require('path');

import {FakeDisplay} from '../FakeDisplay';
import {initializeTestSetup} from '../../test-related-lib/TestOverrides';

import {OutputFormat} from '../../../src/lib/output/OutputFormat';
import {getMessage, BundleName} from '../../../src/MessageCatalog';
import {Inputs} from '../../../src/types';
import {RunDfaAction} from '../../../src/lib/actions/RunDfaAction';
import {InputProcessor, InputProcessorImpl} from '../../../src/lib/InputProcessor';
import {RuleFilterFactory, RuleFilterFactoryImpl} from '../../../src/lib/RuleFilterFactory';
import {EngineOptionsFactory, RunDfaEngineOptionsFactory} from '../../../src/lib/EngineOptionsFactory';
import {ResultsProcessorFactory, ResultsProcessorFactoryImpl} from '../../../src/lib/output/ResultsProcessorFactory';

describe('RunDfaAction', () => {
	let display: FakeDisplay;
	let logger: Logger;
	let inputProcessor: InputProcessor;
	let ruleFilterFactory: RuleFilterFactory;
	let engineOptionsFactory: EngineOptionsFactory;
	let resultsProcessorFactory: ResultsProcessorFactory;

	let runDfaAction: RunDfaAction;

	beforeEach(async () => {
		initializeTestSetup();
		display = new FakeDisplay();
		logger = await Logger.child('RunDfaAction.test.ts');
		inputProcessor = new InputProcessorImpl('test', display);
		ruleFilterFactory = new RuleFilterFactoryImpl();
		engineOptionsFactory = new RunDfaEngineOptionsFactory(inputProcessor);
		resultsProcessorFactory = new ResultsProcessorFactoryImpl();
		runDfaAction = new RunDfaAction(logger, display, inputProcessor,
			ruleFilterFactory, engineOptionsFactory, resultsProcessorFactory);
	});

	describe('#validateInputs()', () => {
		it('--projectdir cannot be a glob', async () => {
			const inputs: Inputs = {
				'projectdir': ['./**/*.cls']
			};

			try {
				await runDfaAction.validateInputs(inputs);
				assert.fail('Error should have been thrown');
			} catch (e) {
				expect(e.message).to.equal(getMessage(BundleName.CommonRun, 'validations.projectdirCannotBeGlob'));
			}
		});

		it('--projectdir must be real', async () => {
			const inputs: Inputs = {
				'projectdir': ['./not/a/real/file.txt']
			};

			try {
				await runDfaAction.validateInputs(inputs);
				assert.fail('Error should have been thrown');
			} catch (e) {
				expect(e.message).to.equal(getMessage(BundleName.CommonRun, 'validations.projectdirMustExist'));
			}
		});

		it('--projectdir must be directory', async () => {
			const inputs: Inputs = {
				// The path is relative to the root directory of the project, not this specific file.
				'projectdir': [path.resolve('./src/Controller.ts')]
			};

			try {
				await runDfaAction.validateInputs(inputs);
				assert.fail('Error should have been thrown');
			} catch (e) {
				expect(e.message).to.equal(getMessage(BundleName.CommonRun, 'validations.projectdirMustBeDir'));
			}
		});

		it('--outfile cannot be used if --format is "table"', async () => {
			const inputs: Inputs = {
				'outfile': 'beep.csv',
				'format': 'table'
			};

			try {
				await runDfaAction.validateInputs(inputs);
				assert.fail('Error should have been thrown');
			} catch (e) {
				expect(e.message).to.equal(getMessage(BundleName.CommonRun, 'validations.cannotWriteTableToFile'));
			}
		});

		it('If --outfile and --format conflict, message is logged', async () => {
			const inputs: Inputs = {
				'outfile': 'beep.csv',
				'format': OutputFormat.XML
			};

			await runDfaAction.validateInputs(inputs);
			expect(display.getOutputArray()).to.contain(`[Info]: ${getMessage(BundleName.CommonRun, 'validations.outfileFormatMismatch', ['xml', 'csv'])}`);
		});

		it('Method-level --target values cannot be globs', async () => {
			const inputs: Inputs = {
				'target': ['./**/*.cls#beep']
			};

			try {
				await runDfaAction.validateInputs(inputs);
				assert.fail('Error should have been thrown');
			} catch (e) {
				expect(e.message).to.equal(getMessage(BundleName.RunDfa, 'validations.methodLevelTargetCannotBeGlob'));
			}
		});

		it('Method-level --target values must be a real file', async () => {
			const file = './not/a/real/file.cls';
			const inputs: Inputs = {
				'target': [`${file}#beep`]
			};

			try {
				await runDfaAction.validateInputs(inputs);
				assert.fail('Error should have been thrown');
			} catch (e) {
				expect(e.message).to.equal(getMessage(BundleName.RunDfa, 'validations.methodLevelTargetMustBeRealFile', [file]));
			}
		});
	});

	describe('#run()', () => {
		const apexControllerStr = 'UnsafeVfController';
		const customSettingsStr = 'none found';
		const customSettingsMessage = getMessage(BundleName.EventKeyTemplates, 'info.sfgeMetaInfoCollected', ['Custom Settings', customSettingsStr]);
		const apexControllerMessage = getMessage(BundleName.EventKeyTemplates, 'info.sfgeMetaInfoCollected', ['Apex Controllers', apexControllerStr]);
		const dfaTarget = path.join('test', 'code-fixtures', 'projects', 'sfge-smoke-app', 'src');
		const projectdir = path.join('test', 'code-fixtures', 'projects', 'sfge-smoke-app', 'src');

		describe('With no special flags', () => {
			it('Pilot rules are not executed', async () => {
				const inputs: Inputs = {
					target:  [dfaTarget],
					projectdir: [projectdir],
					format: 'json'
				};

				await runDfaAction.run(inputs);

				expect(display.getOutputText()).to.not.contain('RemoveUnusedMethod', 'Experimental rules should not be executed');
			});

			it('Verbose-only information is not logged', async () => {
				const inputs: Inputs = {
					target:  [dfaTarget],
					projectdir: [projectdir],
					format: 'json'
				};

				await runDfaAction.run(inputs);

				expect(display.getOutputText()).to.not.contain(customSettingsMessage, 'Custom Settings verbose message should not have been logged');
				expect(display.getOutputText()).to.not.contain(apexControllerMessage, 'Apex Controller verbose message should not have been logged');
			});
		});

		it('Using --with-pilot runs experimental rules', async () => {
			const inputs: Inputs = {
				target:  [dfaTarget],
				projectdir: [projectdir],
				format: 'json',
				'with-pilot': true
			};

			await runDfaAction.run(inputs);
			expect(display.getOutputText()).to.contain('RemoveUnusedMethod', 'Expected violation for experimental rule');
		});

		// TODO: This test fails because the event handler for the messages is assigned in
		//       ScannerCommand.ts, which is no longer invoked in this style of test.
		//       When we move away from the event passing model, it should allow us to enable
		//       this test (hopefully).
		xit('Using --verbose flag logs more detailed information', async () => {
			const inputs: Inputs = {
				target: [dfaTarget],
				projectdir: [projectdir],
				format: 'json',
				verbose: true
			};

			await runDfaAction.run(inputs);
			expect(display.getOutputText()).to.contain(customSettingsMessage, 'Expected Custom Settings verbose message');
			expect(display.getOutputText()).to.contain(apexControllerMessage, 'Expected Apex Controller verbose message');
		});
	});

});
