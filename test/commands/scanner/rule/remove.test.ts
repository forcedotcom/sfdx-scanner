import {expect, test} from '@salesforce/command/lib/test';
import {Messages} from '@salesforce/core';
import {SFDX_SCANNER_PATH} from '../../../../src/Constants';
import {Controller} from '../../../../src/ioc.config';
import fs = require('fs');
import path = require('path');

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'remove');

const CATALOG_OVERRIDE = 'RemoveTestCatalog.json';
const CUSTOM_PATHS_OVERRIDE = 'RemoveTestCustomPaths.json';

// Delete any existing JSONs associated with the tests so they run fresh each time.
if (fs.existsSync(path.join(SFDX_SCANNER_PATH, CATALOG_OVERRIDE))) {
	fs.unlinkSync(path.join(SFDX_SCANNER_PATH, CATALOG_OVERRIDE));
}
if (fs.existsSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATHS_OVERRIDE))) {
	fs.unlinkSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATHS_OVERRIDE));
}

// NOTE: The relative paths are relative to the root of the project instead of to the location of this file,
// because the root is the working directory during test evaluation.
const parentFolderForJars = path.resolve('./test/test-jars/apex');
const pathToApexJar1 = path.resolve('./test/test-jars/apex/testjar1.jar');
const pathToApexJar2 = path.resolve('./test/test-jars/apex/testjar2.jar');
const pathToApexJar3 = path.resolve('./test/test-jars/apex/testjar3.jar');
const pathToApexJar4 = path.resolve('./test/test-jars/apex/testjar4.jar');
// For our tests, we'll include three Apex JARs.
const customPathDescriptor = {
	'pmd': {
		'apex': [pathToApexJar1, pathToApexJar2, pathToApexJar3]
	}
};

const removeTest = test
	.env({CATALOG_FILE: CATALOG_OVERRIDE, CUSTOM_PATHS_FILE: CUSTOM_PATHS_OVERRIDE})
	.do(() => {
		fs.writeFileSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATHS_OVERRIDE), JSON.stringify(customPathDescriptor));
	});

describe('scanner:rule:remove', () => {
	// Reset our controller for each test because a) we are using file overrides and b) these tests muck with them.
	beforeEach(() => Controller.reset());

	describe('E2E', () => {
		describe('Dry-Run (omitting --path parameter)', () => {
			removeTest
				.stdout()
				.stderr()
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
					.stdout()
					.stderr()
					// We'll wait three seconds then send in a 'y', to simulate the user confirming the request.
					.stdin('y\n', 3000)
					.timeout(10000)
					.command(['scanner:rule:remove',
						'--path', pathToApexJar1
					])
					.it('The specified JAR is deleted.', ctx => {
						expect(ctx.stdout).to.contain(
							messages.getMessage('output.resultSummary', [pathToApexJar1]),
							'Console should report deletion.'
						);
						const updatedCustomPathJson = JSON.parse(fs.readFileSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATHS_OVERRIDE)).toString());
						expect(updatedCustomPathJson).to.deep.equal({
							'pmd': {
								'apex': [pathToApexJar2, pathToApexJar3]
							}
						}, `Deletion should have been persisted ${JSON.stringify(updatedCustomPathJson)}`);
					});
			});

			describe('Test Case: Removing multiple PMD JARs', () => {
				removeTest
					.stdout()
					.stderr()
					// We'll wait three seconds then send in a 'y', to simulate the user confirming the request.
					.stdin('y\n', 3000)
					.timeout(10000)
					.command(['scanner:rule:remove',
						'--path', [pathToApexJar1, pathToApexJar2].join(',')
					])
					.it('The specified JARs are deleted', ctx => {
						expect(ctx.stdout).to.contain(
							messages.getMessage('output.resultSummary', [[pathToApexJar1, pathToApexJar2].join(', ')]),
							'Console should report deletion'
						);
						const updatedCustomPathJson = JSON.parse(fs.readFileSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATHS_OVERRIDE)).toString());
						expect(updatedCustomPathJson).to.deep.equal({
							'pmd': {
								'apex': [pathToApexJar3]
							}
						}, 'Deletion should have been persisted');
					});
			});

			describe('Test Case: Removing an entire folder of PMD JARs', () => {
				removeTest
					.stdout()
					.stderr()
					// We'll wait three seconds then send in a 'y', to simulate the user confirming the request.
					.stdin('y\n', 3000)
					.timeout(10000)
					.command(['scanner:rule:remove',
						'--path', parentFolderForJars
					])
					.it('All JARs in the target folder are deleted', ctx => {
						expect(ctx.stdout).to.contain(
							messages.getMessage('output.resultSummary', [[pathToApexJar1, pathToApexJar2, pathToApexJar3].join(', ')]),
							'Console should report deletion'
						);
						const updatedCustomPathJson = JSON.parse(fs.readFileSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATHS_OVERRIDE)).toString());
						expect(updatedCustomPathJson).to.deep.equal({
							'pmd': {
								'apex': []
							}
						}, 'Deletion should have been persisted');
					});
			});

			describe('Edge Case: Provided path is not registered as a custom rule', () => {
				removeTest
					.stdout()
					.stderr()
					// We'll wait three seconds then send in a 'y', to simulate the user confirming the request.
					.stdin('y\n', 3000)
					.timeout(10000)
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
					.stdout()
					.stderr()
					// We'll wait three seconds and then send in a 'n', to simulate the user aborting the request.
					.stdin('n\n', 3000)
					.timeout(10000)
					.command(['scanner:rule:remove',
						'--path', pathToApexJar1
					])
					.it('Request is successfully cancelled', ctx => {
						expect(ctx.stdout).to.contain(messages.getMessage('output.aborted'), 'Transaction should have been aborted');
						const updatedCustomPathJson = JSON.parse(fs.readFileSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATHS_OVERRIDE)).toString());
						expect(updatedCustomPathJson).to.deep.equal(customPathDescriptor, 'Custom paths should not have changed');
					});
			});

			describe('Test Case: User uses --force flag to skip confirmation prompt', () => {
				removeTest
					.stdout()
					.stderr()
					.command(['scanner:rule:remove',
						'--path', pathToApexJar1,
						'--force'
					])
					.it('--force flag bypasses need for confirmation', ctx => {
						expect(ctx.stdout).to.contain(
							messages.getMessage('output.resultSummary', [pathToApexJar1]),
							'Console should report deletion.'
						);
						const updatedCustomPathJson = JSON.parse(fs.readFileSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATHS_OVERRIDE)).toString());
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
					.stdout()
					.stderr()
					.command(['scanner:rule:remove', '--path', ''])
					.it('should complain about empty path', ctx => {
						expect(ctx.stderr).contains(messages.getMessage('validations.pathCannotBeEmpty'));
					});
			});
		});
	});
});
