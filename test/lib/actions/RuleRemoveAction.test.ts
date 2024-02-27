import {Logger} from '@salesforce/core';
import {assert, expect} from 'chai';
import {FileHandler} from '../../../src/lib/util/FileHandler';
import sinon = require('sinon');
import path = require('path');

import {FakeDisplay} from '../FakeDisplay';
import {initializeTestSetup} from '../../test-related-lib/TestOverrides';

import {Controller} from '../../../src/Controller';
import {Inputs} from '../../../src/types';
import {CUSTOM_PATHS_FILE} from '../../../src/Constants';
import {BundleName, getMessage} from '../../../src/MessageCatalog';
import {InputProcessor, InputProcessorImpl} from '../../../src/lib/InputProcessor';
import {RuleRemoveAction} from '../../../src/lib/actions/RuleRemoveAction';

describe('RuleRemoveAction', () => {
	let testLogger: Logger;
	let testDisplay: FakeDisplay;
	let testInputProcessor: InputProcessor;
	let testAction: RuleRemoveAction;

	beforeEach(async () => {
		initializeTestSetup();
		testLogger = await Logger.child('RuleRemoveAction.test.ts');
		testDisplay = new FakeDisplay();
		testInputProcessor = new InputProcessorImpl('test', testDisplay);
		testAction = new RuleRemoveAction(testLogger, testDisplay, testInputProcessor);
	});

	describe('#validateInputs()' ,() => {
		it('Rejects empty `.path` property', async () => {
			const inputs: Inputs = {
				path: []
			};

			try {
				await testAction.validateInputs(inputs);
				assert.fail('Exception should have been thrown');
			} catch (e) {
				expect(e.message).to.equal(getMessage(BundleName.Remove, 'validations.pathCannotBeEmpty', []));
			}
		});

		it('rejects `.path` containing empty string', async () => {
			const inputs: Inputs = {
				path: ['']
			};

			try {
				await testAction.validateInputs(inputs);
				assert.fail('Exception should have been thrown');
			} catch (e) {
				expect(e.message).to.equal(getMessage(BundleName.Remove, 'validations.pathCannotBeEmpty', []));
			}
		});
	});

	describe('#run', () => {
		const FAKE_PARENT_FOLDER = path.resolve('test', 'test-jars', 'apex');
		const FAKE_PATH_1 = path.resolve('test', 'test-jars', 'apex', 'testjar1.jar');
		const FAKE_PATH_2 = path.resolve('test', 'test-jars', 'apex', 'testjar2.jar');
		const FAKE_PATH_3 = path.resolve('test', 'test-jars', 'apex', 'testjar3.jar');
		const FAKE_PATH_4 = path.resolve('test', 'test-jars', 'apex', 'testjar4.jar');
		const FAKE_CUSTOM_RULE_DESCRIPTOR = {
			pmd: {
				apex: [FAKE_PATH_1, FAKE_PATH_2, FAKE_PATH_3]
			}
		};

		afterEach(() => {
			sinon.restore();
		});

		describe('When there ARE NOT registered rule paths...', () => {

			beforeEach(() => {
				// Stub out the file handler, so it thinks the custom rule file has our desired contents.
				const fullRulePath = path.join(Controller.getSfdxScannerPath(), CUSTOM_PATHS_FILE);
				sinon.stub(FileHandler.prototype, 'readFile').callThrough()
					.withArgs(fullRulePath).resolves('{}');
			});

			it('Omitting --path causes dry-run with appropriate message', async () => {
				// ==== EXECUTE TESTED METHOD ====
				// Run the dry run.
				const inputs: Inputs = {};
				await testAction.run(inputs);

				// ==== ASSERTIONS ====
				const expectedDryRunOutput = getMessage(BundleName.Remove, 'output.dryRunReturnedNoRules', []);
				expect(testDisplay.getOutputArray()).to.contain(`[Info]: ${expectedDryRunOutput}`);
			});

			it('Using --path causes Match Failure error, since there no paths to match', async () => {
				// ==== EXECUTE TESTED METHOD ====
				const inputs: Inputs = {
					path: [FAKE_PATH_1]
				};
				try {
					await testAction.run(inputs);
					assert.fail('Error should have been thrown');
				} catch (e) {
					// ==== ASSERTIONS ====
					expect(e.message).to.equal(getMessage(BundleName.Remove, 'errors.noMatchingPaths'));
				}
			});
		});

		describe('When there ARE registered rule paths...', () => {
			let writtenFile: string;

			beforeEach(() => {
				writtenFile = '';
				// Stub out the file handler, so it thinks the custom rule file has our desired contents.
				const fullRulePath = path.join(Controller.getSfdxScannerPath(), CUSTOM_PATHS_FILE);
				sinon.stub(FileHandler.prototype, 'readFile').callThrough()
					.withArgs(fullRulePath).resolves(JSON.stringify(FAKE_CUSTOM_RULE_DESCRIPTOR));
				sinon.stub(FileHandler.prototype, 'writeFile').callThrough()
					.withArgs(fullRulePath).callsFake(async (file, content) => {
						writtenFile = content;
				});
			});

			it('Omitting --path causes dry-run and logs all paths', async () => {
				// ==== EXECUTE TESTED METHOD ====
				// Run the dry run.
				const inputs: Inputs = {};
				await testAction.run(inputs);

				// ==== ASSERTIONS ====
				const expectedDryRunOutput = getMessage(BundleName.Remove, 'output.dryRunOutput',
					[3, FAKE_CUSTOM_RULE_DESCRIPTOR.pmd.apex.join('\n')]);
				expect(testDisplay.getOutputArray()).to.contain(`[Info]: ${expectedDryRunOutput}`);
			});

			describe('Using --path allows removal with confirmation prompt', () => {
				it('Test Case: Can remove a single JAR', async () => {
					// ==== EXECUTE TESTED METHOD ====
					// Remove just one JAR.
					const inputs = {
						path: [FAKE_PATH_1]
					};
					await testAction.run(inputs);

					// ==== ASSERTIONS ====
					// The JAR we removed should be gone.
					expect(writtenFile).to.not.contain(JSON.stringify(FAKE_PATH_1), 'JAR should have been removed');
					// The other JARs should still be there.
					expect(writtenFile).to.contain(JSON.stringify(FAKE_PATH_2), 'JAR should NOT have been removed');
					expect(writtenFile).to.contain(JSON.stringify(FAKE_PATH_3), 'JAR should NOT have been removed');
				});

				it('Test Case: Can remove a list of JARs', async () => {
					// ==== EXECUTE TESTED METHOD ====
					// Remove two JARs.
					const inputs = {
						path: [FAKE_PATH_1, FAKE_PATH_2]
					};
					await testAction.run(inputs);

					// ==== ASSERTIONS ====
					// The JARs we removed should be gone.
					expect(writtenFile).to.not.contain(JSON.stringify(FAKE_PATH_1), 'JAR should have been removed');
					expect(writtenFile).to.not.contain(JSON.stringify(FAKE_PATH_2), 'JAR should have been removed');
					// The other JAR should still be there.
					expect(writtenFile).to.contain(JSON.stringify(FAKE_PATH_3), 'JAR should NOT have been removed');
				});

				it('Test Case: Can remove a folder', async () => {
					// ==== EXECUTE TESTED METHOD ====
					// Remove the folder containing all the JARs.
					const inputs = {
						path: [FAKE_PARENT_FOLDER]
					};
					await testAction.run(inputs);

					// ==== ASSERTIONS ====
					// All JARs should be gone.
					expect(writtenFile).to.not.contain(JSON.stringify(FAKE_PATH_1), 'JAR should have been removed');
					expect(writtenFile).to.not.contain(JSON.stringify(FAKE_PATH_2), 'JAR should have been removed');
					expect(writtenFile).to.not.contain(JSON.stringify(FAKE_PATH_3), 'JAR should have been removed');
				});

				it('Test Case: Throws error for unregistered path', async () => {
					// ==== EXECUTE TESTED METHOD ====
					// Attempt to remove a JAR that isn't already registered.
					const inputs = {
						path: [FAKE_PATH_4]
					};
					try {
						await testAction.run(inputs);
						assert.fail('Error should have thrown');
					} catch (e) {
						expect(e.message).to.equal(getMessage(BundleName.Remove, 'errors.noMatchingPaths', []));
					}
				});

				it('Test Case: Action can be aborted during confirmation prompt', async () => {
					// ==== SETUP ====
					// Configure the display so that aborts instead of confirming.
					testDisplay.setConfirmationPromptResponse(false);

					// ==== EXECUTE TESTED METHOD ====
					// Attempt to remove a JAR.
					const inputs = {
						path: [FAKE_PATH_1]
					};
					await testAction.run(inputs);

					// ==== ASSERTIONS ====
					// Action should have aborted and persisted no changes.
					expect(testDisplay.getOutputArray()).to.contain(`[Info]: ${getMessage(BundleName.Remove, 'output.aborted', [])}`);
					expect(writtenFile).to.equal('', 'Nothing should have been persisted');
				});
			});

			describe('Using --force bypasses the confirmation prompt', () => {
				it('Test Case: Removing a single JAR', async () => {
					// ==== SETUP ====
					// Configure the display such that the confirmation prompt will reject if it's encountered.
					// This is guaranteeing that the confirmation prompt was never given, meaning that --force
					// bypassed it.
					testDisplay.setConfirmationPromptResponse(false);

					// ==== EXECUTE TESTED METHOD ====
					// Remove just one JAR.
					const inputs = {
						path: [FAKE_PATH_1],
						force: true
					};
					await testAction.run(inputs);

					// ==== ASSERTIONS ====
					// The JAR we removed should be gone.
					expect(writtenFile).to.not.contain(JSON.stringify(FAKE_PATH_1), 'JAR should have been removed');
					// The other JARs should still be there.
					expect(writtenFile).to.contain(JSON.stringify(FAKE_PATH_2), 'JAR should NOT have been removed');
					expect(writtenFile).to.contain(JSON.stringify(FAKE_PATH_3), 'JAR should NOT have been removed');
				});

				it('Test Case: Throws error for unregistered path', async () => {
					// ==== EXECUTE TESTED METHOD ====
					// Attempt to remove a JAR that isn't already registered.
					const inputs = {
						path: [FAKE_PATH_4],
						force: true
					};
					try {
						await testAction.run(inputs);
						assert.fail('Error should have thrown');
					} catch (e) {
						expect(e.message).to.equal(getMessage(BundleName.Remove, 'errors.noMatchingPaths', []));
					}
				});
			});
		});
	});
});
