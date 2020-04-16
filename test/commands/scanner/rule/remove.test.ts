import {expect, test} from '@salesforce/command/lib/test';
import {Messages} from '@salesforce/core';
//import * as os from 'os';
import {SFDX_SCANNER_PATH} from '../../../../src/Constants';
import {PmdEngine} from '../../../../src/lib/pmd/PmdEngine';
import fs = require('fs');
import path = require('path');

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'remove');

const CATALOG_OVERRIDE = 'RemoveTestPmdCatalog.json';
const CUSTOM_PATH_OVERRIDE = 'RemoveTestCustomPaths.json';

// Delete any existing JSONs associated with the tests so they run fresh each time.
if (fs.existsSync(path.join(SFDX_SCANNER_PATH, CATALOG_OVERRIDE))) {
	fs.unlinkSync(path.join(SFDX_SCANNER_PATH, CATALOG_OVERRIDE));
}
if (fs.existsSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATH_OVERRIDE))) {
	fs.unlinkSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATH_OVERRIDE));
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
	[PmdEngine.NAME]: {
		'apex': [pathToApexJar1, pathToApexJar2, pathToApexJar3]
	}
};

let removeTest = test
	.env({PMD_CATALOG_NAME: CATALOG_OVERRIDE, CUSTOM_PATH_FILE: CUSTOM_PATH_OVERRIDE})
	.do(() => {
		fs.writeFileSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATH_OVERRIDE), JSON.stringify(customPathDescriptor));
	});

describe('scanner:rule:remove', () => {
	describe('E2E', () => {
		describe('Rule Removal', () => {
			describe('Test Case: Removing a single PMD JAR', () => {
				removeTest
					.stdout()
					.stderr()
					// We'll wait three seconds then send in a 'y', to simulate the user confirming the request.
					.stdin('y\n', 3000)
					.timeout(10000)
					.command(['scanner:rule:remove',
						'--language', 'apex',
						'--path', pathToApexJar1
					])
					.it('The specified JAR is deleted.', ctx => {
						expect(ctx.stdout).to.contain(
							messages.getMessage('output.resultSummary', [pathToApexJar1]),
							'Console should report deletion.'
						);
						const updatedCustomPathJson = JSON.parse(fs.readFileSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATH_OVERRIDE)).toString());
						expect(updatedCustomPathJson).to.deep.equal({
							[PmdEngine.NAME]: {
								'apex': [pathToApexJar2, pathToApexJar3]
							}
						}, 'Deletion should have been persisted');
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
						'--language', 'apex',
						'--path', [pathToApexJar1, pathToApexJar2].join(',')
					])
					.it('The specified JARs are deleted', ctx => {
						expect(ctx.stdout).to.contain(
							messages.getMessage('output.resultSummary', [[pathToApexJar1, pathToApexJar2].join(', ')]),
							'Console should report deletion'
						);
						const updatedCustomPathJson = JSON.parse(fs.readFileSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATH_OVERRIDE)).toString());
						expect(updatedCustomPathJson).to.deep.equal({
							[PmdEngine.NAME]: {
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
						'--language', 'apex',
						'--path', parentFolderForJars
					])
					.it('All JARs in the target folder are deleted', ctx => {
						expect(ctx.stdout).to.contain(
							messages.getMessage('output.resultSummary', [[pathToApexJar1, pathToApexJar2, pathToApexJar3].join(', ')]),
							'Console should report deletion'
						);
						const updatedCustomPathJson = JSON.parse(fs.readFileSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATH_OVERRIDE)).toString());
						expect(updatedCustomPathJson).to.deep.equal({
							[PmdEngine.NAME]: {
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
						'--language', 'apex',
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
						'--language', 'apex',
						'--path', pathToApexJar1
					])
					.it('Request is successfully cancelled', ctx => {
						expect(ctx.stdout).to.contain(messages.getMessage('output.aborted'), 'Transaction should have been aborted');
						const updatedCustomPathJson = JSON.parse(fs.readFileSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATH_OVERRIDE)).toString());
						expect(updatedCustomPathJson).to.deep.equal(customPathDescriptor, 'Custom paths should not have changed');
					});
			});

			describe('Test Case: User uses --force flag to skip confirmation prompt', () => {
				removeTest
					.stdout()
					.stderr()
					.command(['scanner:rule:remove',
						'--language', 'apex',
						'--path', pathToApexJar1,
						'--force'
					])
					.it('--force flag bypasses need for confirmation', ctx => {
						expect(ctx.stdout).to.contain(
							messages.getMessage('output.resultSummary', [pathToApexJar1]),
							'Console should report deletion.'
						);
						const updatedCustomPathJson = JSON.parse(fs.readFileSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATH_OVERRIDE)).toString());
						expect(updatedCustomPathJson).to.deep.equal({
							[PmdEngine.NAME]: {
								'apex': [pathToApexJar2, pathToApexJar3]
							}
						}, 'Deletion should have been persisted');
					});
			});
		});

		describe('Validations', () => {
			describe('Language validations', () => {
				// Test for failure scenario doesn't need to do any special setup or cleanup.
				test
					.stdout()
					.stderr()
					.command(['scanner:rule:remove', '--path', '/some/local/path'])
					.it('should complain about missing --language flag', ctx => {
						expect(ctx.stderr).contains(messages.getMessage('flags.languageDescription'));
					});

				// Test for failure scenario doesn't need to do any special setup or cleanup.
				test
					.stdout()
					.stderr()
					.command(['scanner:rule:remove', '--language', '', '--path', '/some/local/path'])
					.it('should complain about empty language entry', ctx => {
						expect(ctx.stderr).contains(messages.getMessage('validations.languageCannotBeEmpty'));
					});
			});

			describe('Path validations', () => {
				// Test for failure scenario doesn't need to do any special setup or cleanup.
				test
					.stdout()
					.stderr()
					.command(['scanner:rule:remove', '--language', 'apex'])
					.it('should complain about missing --path flag', ctx => {
						expect(ctx.stderr).contains('Missing required flag:\n -p, --path PATH');
					});

				// Test for failure scenario doesn't need to do any special setup or cleanup.
				test
					.stdout()
					.stderr()
					.command(['scanner:rule:remove', '--language', 'apex', '--path', ''])
					.it('should complain about empty path', ctx => {
						expect(ctx.stderr).contains(messages.getMessage('validations.pathCannotBeEmpty'));
					});
			});
		});
	});
});
