import {expect} from '@salesforce/command/lib/test';
import {setupCommandTest} from '../../TestUtils';
import {Messages} from '@salesforce/core';
import path = require('path');


Messages.importMessagesDirectory(__dirname);
const runMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run');

describe('scanner:run', function () {
	this.timeout(10000); // TODO why do we get timeouts at the default of 5000?  What is so expensive here?

	describe('E2E', () => {
		
		describe('--severity-for-error flag', () => {

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls'),
					'--format', 'json',
					'--severity-for-error', '3'
				])
				.it('When no violations are found, no error is thrown', ctx => {
					expect(ctx.stdout).to.contain(runMessages.getMessage('output.noViolationsDetected', ['pmd']));
					expect(ctx.stderr).to.not.contain(runMessages.getMessage('output.sevDetectionSummary', ['3']), 'Error should not be present');
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--format', 'json',
					'--severity-for-error', '1'
				])
				.it('When no violations are found equal to or greater than flag value, no error is thrown', ctx => {
					//error should not be thrown and we don't see a severity that fits flag value
					const output = JSON.parse(ctx.stdout);

                    let shouldError: boolean = false;

                    for (let i=0; i<output.length; i++) {
                        for (let j=0; j<output[i].violations.length; j++) {
                            if (output[i].violations[j].severity == 1) {
                                shouldError = true;
                            }
                        }
                    }

                    if (shouldError) {
                        expect(ctx.stderr).to.contain(runMessages.getMessage('output.sevDetectionSummary', ['1']));
                    } else {
                        const output = JSON.parse(ctx.stdout);
                        expect(output[0].violations[0].message).to.contain('Apex unit tests should System.assert()');
                        expect(output[0].violations[1].message).to.contain('Apex unit tests should System.assert()');
                        expect(output[0].violations[2].message).to.contain("Variable 'd1' defined but not used");
                        expect(output[0].violations[3].message).to.contain("Variable 'd2' defined but not used");
                        expect(ctx.stderr).not.to.contain(runMessages.getMessage('output.engineSummaryTemplate', ['pmd', 4, 1]), 'Error should be present');
                    }
					
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--format', 'json',
					'--severity-for-error', '3'
				])
				.it('When violations are found equal to or greater than flag value, an error is thrown', ctx => {

                    const output = JSON.parse(ctx.stdout);

                    let shouldError: boolean = false;

                    for (let i=0; i<output.length; i++) {
                        for (let j=0; j<output[i].violations.length; j++) {
                            if (output[i].violations[j].severity >= 3) {
                                shouldError = true;
                                break;
                            }
                        }
                    }

                    if (shouldError) {
                        expect(ctx.stderr).to.contain(runMessages.getMessage('output.sevDetectionSummary', ['3']));
                    } else {
                        const output = JSON.parse(ctx.stdout);
                        expect(output[0].violations[0].message).to.contain('Apex unit tests should System.assert()');
                        expect(output[0].violations[1].message).to.contain('Apex unit tests should System.assert()');
                        expect(output[0].violations[2].message).to.contain("Variable 'd1' defined but not used");
                        expect(output[0].violations[3].message).to.contain("Variable 'd2' defined but not used");
                        expect(ctx.stderr).not.to.contain(runMessages.getMessage('output.engineSummaryTemplate', ['pmd', 4, 1]), 'Error should be present');
                    }
				});
		});

	});
});
