import {expect} from '@salesforce/command/lib/test';
import {setupCommandTest} from '../../../TestUtils';
import {Messages} from '@salesforce/core';
import {Controller} from '../../../../src/Controller';
import { CUSTOM_PATHS_FILE } from '../../../../src/Constants';
import fs = require('fs');
import path = require('path');

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'remove');

function getSfdxScannerPath(): string {
	return Controller.getSfdxScannerPath();
}

// NOTE: The relative paths are relative to the root of the project instead of to the location of this file,
// because the root is the working directory during test evaluation.
const parentFolderForJars = path.resolve('test', 'test-jars', 'apex');
const pathToApexJar1 = path.resolve('test', 'test-jars', 'apex', 'testjar1.jar');
const pathToApexJar2 = path.resolve('test', 'test-jars', 'apex', 'testjar2.jar');
const pathToApexJar3 = path.resolve('test', 'test-jars', 'apex', 'testjar3.jar');
const pathToApexJar4 = path.resolve('test', 'test-jars', 'apex', 'testjar4.jar');
// For our tests, we'll include three Apex JARs.
const customPathDescriptor = {
	'pmd': {
		'apex': [pathToApexJar1, pathToApexJar2, pathToApexJar3]
	}
};

const removeTest = setupCommandTest
	.do(() => {
		writeNewCustomPathFile();
	});

describe('scanner:rule:remove', () => {
	describe('E2E', () => {
		describe('Dry-Run (omitting --path parameter)', () => {
			removeTest
				.command(['scanner:rule:remove'])
				.it('When custom rules are registered, all paths are returned', ctx => {
					const expectedRuleSummary = [pathToApexJar1, pathToApexJar2, pathToApexJar3]
						.map(p => messages.getMessage('output.dryRunRuleTemplate', [p]))
						.join('\n');
					expect(ctx.stdout).to.contain(messages.getMessage('output.dryRunOutput', [3, expectedRuleSummary]), 'All paths should be logged');
				});
		});

		describe('Rule Removal', () => {
			describe('Test Case: Removing a single PMD JAR', () => {
				removeTest
					// We'll wait ten seconds then send in a 'y', to simulate the user confirming the request.
					// Note: The real time until the prompt is given shouldn't be anywhere near this long,
					// but a high delay prevents test failures due to the input being sent before the prompt
					// is displayed.
					.stdin('y\n', 10000)
					// The timeout is twenty seconds, not because we expect it to take that long, but just to make
					// sure that there's enough time for the test to complete.
					.timeout(20000)
					.command(['scanner:rule:remove',
						'--path', pathToApexJar1
					])
					.it('The specified JAR is deleted.', ctx => {
						expect(ctx.stdout).to.contain(
							messages.getMessage('output.resultSummary', [pathToApexJar1]),
							'Console should report deletion.'
						);
						const updatedCustomPathJson = getCustomPathFileContent();
						expect(updatedCustomPathJson).to.deep.equal({
							'pmd': {
								'apex': [pathToApexJar2, pathToApexJar3]
							}
						}, `Deletion should have been persisted ${JSON.stringify(updatedCustomPathJson)}`);
					});
			});

			describe('Test Case: Removing multiple PMD JARs', () => {
				removeTest
					// We'll wait ten seconds then send in a 'y', to simulate the user confirming the request.
					// Note: The real time until the prompt is given shouldn't be anywhere near this long,
					// but a high delay prevents test failures due to the input being sent before the prompt
					// is displayed.
					.stdin('y\n', 10000)
					// The timeout is twenty seconds, not because we expect it to take that long, but just to make
					// sure that there's enough time for the test to complete.
					.timeout(20000)
					.command(['scanner:rule:remove',
						'--path', [pathToApexJar1, pathToApexJar2].join(',')
					])
					.it('The specified JARs are deleted', ctx => {
						expect(ctx.stdout).to.contain(
							messages.getMessage('output.resultSummary', [[pathToApexJar1, pathToApexJar2].join(', ')]),
							'Console should report deletion'
						);
						const updatedCustomPathJson = getCustomPathFileContent();
						expect(updatedCustomPathJson).to.deep.equal({
							'pmd': {
								'apex': [pathToApexJar3]
							}
						}, 'Deletion should have been persisted');
					});
			});

			describe('Test Case: Removing an entire folder of PMD JARs', () => {
				removeTest
					// We'll wait ten seconds then send in a 'y', to simulate the user confirming the request.
					// Note: The real time until the prompt is given shouldn't be anywhere near this long,
					// but a high delay prevents test failures due to the input being sent before the prompt
					// is displayed.
					.stdin('y\n', 10000)
					// The timeout is twenty seconds, not because we expect it to take that long, but just to make
					// sure that there's enough time for the test to complete.
					.timeout(20000)
					.command(['scanner:rule:remove',
						'--path', parentFolderForJars
					])
					.it('All JARs in the target folder are deleted', ctx => {
						expect(ctx.stdout).to.contain(
							messages.getMessage('output.resultSummary', [[pathToApexJar1, pathToApexJar2, pathToApexJar3].join(', ')]),
							'Console should report deletion'
						);
						const updatedCustomPathJson = getCustomPathFileContent();
						expect(updatedCustomPathJson).to.deep.equal({
							'pmd': {
								'apex': []
							}
						}, 'Deletion should have been persisted');
					});
			});

			describe('Edge Case: Provided path is not registered as a custom rule', () => {
				removeTest
					// We'll wait ten seconds then send in a 'y', to simulate the user confirming the request.
					// Note: The real time until the prompt is given shouldn't be anywhere near this long,
					// but a high delay prevents test failures due to the input being sent before the prompt
					// is displayed.
					.stdin('y\n', 10000)
					// The timeout is twenty seconds, not because we expect it to take that long, but just to make
					// sure that there's enough time for the test to complete.
					.timeout(20000)
					.command(['scanner:rule:remove',
						'--path', pathToApexJar4
					])
					.it('All JARs in the target folder are deleted', ctx => {
						expect(ctx.stderr).to.contain(messages.getMessage('errors.noMatchingPaths'), 'Should throw expected error');
					});
			});
		});

		describe('User prompt', () => {
			describe('Test Case: User chooses to abort transaction instead of confirming', () => {
				removeTest
					// We'll wait ten seconds then send in a 'y', to simulate the user confirming the request.
					// Note: The real time until the prompt is given shouldn't be anywhere near this long,
					// but a high delay prevents test failures due to the input being sent before the prompt
					// is displayed.
					.stdin('n\n', 10000)
					// The timeout is twenty seconds, not because we expect it to take that long, but just to make
					// sure that there's enough time for the test to complete.
					.timeout(20000)
					.command(['scanner:rule:remove',
						'--path', pathToApexJar1
					])
					.it('Request is successfully cancelled', ctx => {
						expect(ctx.stdout).to.contain(messages.getMessage('output.aborted'), 'Transaction should have been aborted');
						const updatedCustomPathJson = getCustomPathFileContent();
						expect(updatedCustomPathJson).to.deep.equal(customPathDescriptor, 'Custom paths should not have changed');
					});
			});

			describe('Test Case: User uses --force flag to skip confirmation prompt', () => {
				removeTest
					.command(['scanner:rule:remove',
						'--path', pathToApexJar1,
						'--force'
					])
					.it('--force flag bypasses need for confirmation', ctx => {
						expect(ctx.stdout).to.contain(
							messages.getMessage('output.resultSummary', [pathToApexJar1]),
							'Console should report deletion.'
						);
						const updatedCustomPathJson = getCustomPathFileContent();
						expect(updatedCustomPathJson).to.deep.equal({
							'pmd': {
								'apex': [pathToApexJar2, pathToApexJar3]
							}
						}, 'Deletion should have been persisted');
					});
			});
		});

		describe('Validations', () => {
			describe('Path validations', () => {
				// Test for failure scenario doesn't need to do any special setup or cleanup.
				removeTest
					.command(['scanner:rule:remove', '--path', ''])
					.it('should complain about empty path', ctx => {
						expect(ctx.stderr).contains(messages.getMessage('validations.pathCannotBeEmpty'));
					});
			});
		});
	});
});

function writeNewCustomPathFile() {
	fs.writeFileSync(path.join(getSfdxScannerPath(), CUSTOM_PATHS_FILE), JSON.stringify(customPathDescriptor));
}

function getCustomPathFileContent(): string {
	return JSON.parse(fs.readFileSync(path.join(getSfdxScannerPath(), CUSTOM_PATHS_FILE)).toString());
}

