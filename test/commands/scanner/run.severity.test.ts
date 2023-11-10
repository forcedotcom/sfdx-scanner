import {expect} from '@salesforce/command/lib/test';
// @ts-ignore
import {runCommand} from '../../TestUtils';
import {Messages} from '@salesforce/core';
import path = require('path');


Messages.importMessagesDirectory(__dirname);
const processorMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'RunOutputProcessor');

describe('scanner:run', function () {

	describe('E2E', () => {

		describe('--severity-threshold flag', () => {
			describe('Flag functionality', () => {
				it('When no violations are found, no error is thrown', () => {
					const output = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls')} --format json --severity-threshold 3`);
					expect(output.shellOutput.stdout).to.contain(processorMessages.getMessage('output.noViolationsDetected', ['pmd, retire-js']));
					expect(output.shellOutput.stderr).to.not.contain(processorMessages.getMessage('output.sevThresholdSummary', ['3']), 'Error should not be present');
				});

				it('When no violations exceed the flag value, no error is thrown', () => {
					const commandOutput = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls')} --format json --severity-threshold 1`);

					const output = JSON.parse(commandOutput.shellOutput.stdout);
					// check that test file still has severities of 3
					for (let i = 0; i < output.length; i++) {
						for (let j = 0; j < output[i].violations.length; j++) {
							expect(output[i].violations[j].normalizedSeverity).to.equal(3);
						}
					}

					expect(commandOutput.shellOutput.stderr).not.to.contain(processorMessages.getMessage('output.sevThresholdSummary', ['1']));
				});

				it('When flag value is exceeded, an error is thrown', () => {
					const commandOutput = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls')} --format json --severity-threshold 3`);
					const output = JSON.parse(commandOutput.shellOutput.stdout);
					// check that test file still has severities of 3
					for (let i = 0; i < output.length; i++) {
						for (let j = 0; j < output[i].violations.length; j++) {
							expect(output[i].violations[j].normalizedSeverity).to.equal(3);
						}
					}
					expect(commandOutput.shellOutput.stderr).to.contain(processorMessages.getMessage('output.sevThresholdSummary', ['3']));
				});
			});

			describe('Input validation', () => {
				it('Input cannot be less than 1', () => {
					const commandOutput = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls')} --format json --severity-threshold -1`);
					expect(commandOutput.shellOutput.stderr).to.contain('Expected integer greater than or equal to 1 but received -1');
				});

				it('Input cannot be greater than 3', () => {
					const commandOutput = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls')} --format json --severity-threshold 5`);
					expect(commandOutput.shellOutput.stderr).to.contain('Expected integer less than or equal to 3 but received 5');
				});
			});
		});

		describe('--normalize-severity flag', () => {

			it('Normalized severities are correct', () => {
				const commandOutput = runCommand(`scanner run --target ${path.join('test', 'code-fixtures')} --format json --normalize-severity`);
				const output = JSON.parse(commandOutput.shellOutput.stdout);
				for (let i=0; i<output.length; i++) {
					for (let j=0; j<output[i].violations.length; j++) {
						if (output[i].engine.includes("pmd")){
							if (output[i].violations[j].severity == 1) {
								expect(output[i].violations[j].normalizedSeverity).to.equal(1);
							} else if (output[i].violations[j].severity == 2) {
								expect(output[i].violations[j].normalizedSeverity).to.equal(2);
							} else if (output[i].violations[j].severity == 3) {
								expect(output[i].violations[j].normalizedSeverity).to.equal(3);
							} else if (output[i].violations[j].severity == 4) {
								expect(output[i].violations[j].normalizedSeverity).to.equal(3);
							} else if (output[i].violations[j].severity == 5) {
								expect(output[i].violations[j].normalizedSeverity).to.equal(3);
							}
						} else if (output[i].engine.includes("eslint")) {
							if (output[i].violations[j].severity == 1) {
								expect(output[i].violations[j].normalizedSeverity).to.equal(2);
							} else if (output[i].violations[j].severity == 2) {
								expect(output[i].violations[j].normalizedSeverity).to.equal(1);
							}
						}

					}
				}
			});

			it('Omitting --normalize-severity yields non-normalized severities', () => {
				const commandOutput = runCommand(`scanner run --target ${path.join('test', 'code-fixtures')} --format json`);
				const output = JSON.parse(commandOutput.shellOutput.stdout);

				for (let i=0; i<output.length; i++) {
					for (let j=0; j<output[i].violations.length; j++) {
						expect(output[i].violations[j].normalizedSeverity).to.equal(undefined);
					}
				}
			});
		});

	});
});
