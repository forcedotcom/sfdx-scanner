import {expect} from 'chai';
import {Interaction} from '@salesforce/cli-plugins-testkit';
// @ts-ignore
import {runCommand, runInteractiveCommand} from '../../../TestUtils';
import {Controller} from '../../../../src/Controller';
import { CUSTOM_PATHS_FILE } from '../../../../src/Constants';
import * as TestOverrides from './../../../test-related-lib/TestOverrides';
import fs = require('fs');
import path = require('path');
import {Bundle, getMessage} from "../../../../src/MessageCatalog";

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

describe('scanner rule remove', () => {
	beforeEach(() => {
		TestOverrides.initializeTestSetup();
		writePopulatedCustomPathFile();
	});

	afterEach(() => {
		// Clean up after ourselves.
		writeEmptyCustomPathFile();
	});

	describe('E2E', () => {
		describe('Interactivity', () => {
			it('Omitting --path outputs list of removeable paths', () => {
				const output = runCommand(`scanner rule remove`);
				const expectedRuleSummary = [pathToApexJar1, pathToApexJar2, pathToApexJar3]
					.map(p => getMessage(Bundle.Remove, 'output.dryRunRuleTemplate', [p]))
					.join('\n');
				expect(output.shellOutput.stdout).to.contain(getMessage(Bundle.Remove, 'output.dryRunOutput', [3, expectedRuleSummary]), 'All paths should be logged');
			});

			it('By default, rule removal must be confirmed', async () => {
				const output = await runInteractiveCommand(`scanner rule remove --path ${pathToApexJar1}`, {
					'These rules will be unregistered': Interaction.Yes
				});
				expect(output.stdout).to.contain(
					getMessage(Bundle.Remove, 'output.resultSummary', [pathToApexJar1]),
					'Console should report deletion.'
				);
				const updatedCustomPathJson = getCustomPathFileContent();
				expect(updatedCustomPathJson).to.deep.equal({
					'pmd': {
						'apex': [pathToApexJar2, pathToApexJar3]
					}
				}, `Deletion should have been persisted ${JSON.stringify(updatedCustomPathJson)}`);
			});

			it('Rule removal can be safely aborted', async () => {
				const output = await runInteractiveCommand(`scanner rule remove --path ${pathToApexJar1}`, {
					'These rules will be unregistered': Interaction.No
				});
				expect(output.stdout).to.contain(getMessage(Bundle.Remove, 'output.aborted'), 'Transaction should have been aborted');
				const updatedCustomPathJson = getCustomPathFileContent();
				expect(updatedCustomPathJson).to.deep.equal(customPathDescriptor, 'Custom paths should not have changed');
			});
		});

		describe('Functionality', () => {
			it('Successfully removes a single JAR', () => {
				const output = runCommand(`scanner rule remove --path ${pathToApexJar1} --force`);
				expect(output.shellOutput.stdout).to.contain(
					getMessage(Bundle.Remove, 'output.resultSummary', [pathToApexJar1]),
					'Console should report deletion.'
				);
				const updatedCustomPathJson = getCustomPathFileContent();
				expect(updatedCustomPathJson).to.deep.equal({
					'pmd': {
						'apex': [pathToApexJar2, pathToApexJar3]
					}
				}, `Deletion should have been persisted ${JSON.stringify(updatedCustomPathJson)}`);
			});

			it('Successfully removes multiple JARs at once', () => {
				const output = runCommand(`scanner rule remove --path ${pathToApexJar1},${pathToApexJar2} --force`);
				expect(output.shellOutput.stdout).to.contain(
					getMessage(Bundle.Remove, 'output.resultSummary', [[pathToApexJar1, pathToApexJar2].join(', ')]),
					'Console should report deletion'
				);
				const updatedCustomPathJson = getCustomPathFileContent();
				expect(updatedCustomPathJson).to.deep.equal({
					'pmd': {
						'apex': [pathToApexJar3]
					}
				}, 'Deletion should have been persisted');
			});

			it('Successfully removes a whole folder', () => {
				const output = runCommand(`scanner rule remove --path ${parentFolderForJars} --force`);
				expect(output.shellOutput.stdout).to.contain(
					getMessage(Bundle.Remove, 'output.resultSummary', [[pathToApexJar1, pathToApexJar2, pathToApexJar3].join(', ')]),
					'Console should report deletion'
				);
				const updatedCustomPathJson = getCustomPathFileContent();
				expect(updatedCustomPathJson).to.deep.equal({
					'pmd': {
						'apex': []
					}
				}, 'Deletion should have been persisted');
			});

			it('Throws error when requested path is not already registered', () => {
				const output = runCommand(`scanner rule remove --path ${pathToApexJar4}`);
				expect(output.shellOutput.stderr).to.contain(getMessage(Bundle.Remove, 'errors.noMatchingPaths'), 'Should throw expected error');
			});
		});

		describe('Validations', () => {
			it('Complains about empty --path', () => {
				const output = runCommand(`scanner rule remove --path ''`);
				expect(output.shellOutput.stderr).to.contain(getMessage(Bundle.Remove, 'validations.pathCannotBeEmpty'));
			});
		});
	});
});

function writeEmptyCustomPathFile() {
	fs.writeFileSync(path.join(getSfdxScannerPath(), CUSTOM_PATHS_FILE), "{}");
}

function writePopulatedCustomPathFile() {
	fs.writeFileSync(path.join(getSfdxScannerPath(), CUSTOM_PATHS_FILE), JSON.stringify(customPathDescriptor));
}

function getCustomPathFileContent(): string {
	return JSON.parse(fs.readFileSync(path.join(getSfdxScannerPath(), CUSTOM_PATHS_FILE)).toString());
}

