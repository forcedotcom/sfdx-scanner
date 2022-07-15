import {expect} from '@salesforce/command/lib/test';
import {setupCommandTest} from '../../TestUtils';
import {Messages} from '@salesforce/core';
import path = require('path');


Messages.importMessagesDirectory(__dirname);
const runMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run');

describe('scanner:run', function () {

	describe('E2E', () => {

		describe('--severity-threshold flag', () => {

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls'),
					'--format', 'json',
					'--severity-threshold', '3'
				])
				.it('When no violations are found, no error is thrown', ctx => {
					expect(ctx.stdout).to.contain(runMessages.getMessage('output.noViolationsDetected', ['pmd']));
					expect(ctx.stderr).to.not.contain(runMessages.getMessage('output.sevThresholdSummary', ['3']), 'Error should not be present');
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--format', 'json',
					'--severity-threshold', '1'
				])
				.it('When no violations are found equal to or greater than flag value, no error is thrown', ctx => {
					// The first line is a header. Strip that away, and parse the rest as JSON.
                    const output = JSON.parse(ctx.stdout.split('\n')[1]);
                    // check that test file still has severities of 3
                    for (let i=0; i<output.length; i++) {
                        for (let j=0; j<output[i].violations.length; j++) {
                            expect(output[i].violations[j].normalizedSeverity).to.equal(3);
                        }
                    }

                    expect(ctx.stderr).not.to.contain(runMessages.getMessage('output.sevThresholdSummary', ['1']));

				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--format', 'json',
					'--severity-threshold', '3'
				])
				.it('When violations are found equal to or greater than flag value, an error is thrown', ctx => {
					// The first line is a header. Strip that away, and parse the rest as JSON.
					const output = JSON.parse(ctx.stdout.split('\n')[1]);
                    // check that test file still has severities of 3
                    for (let i=0; i<output.length; i++) {
                        for (let j=0; j<output[i].violations.length; j++) {
                            expect(output[i].violations[j].normalizedSeverity).to.equal(3);
                        }
                    }
                    expect(ctx.stderr).to.contain(runMessages.getMessage('output.sevThresholdSummary', ['3']));

				});

			setupCommandTest
                .command(['scanner:run',
                    '--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
                    '--format', 'json',
                    '--severity-threshold', '-1'
                ])
                .it('Ensure values below the min for severity-threshold cause an error', ctx => {
                    expect(ctx.stderr).to.contain('Expected integer greater than or equal to 1 but received -1');
                });

            setupCommandTest
                .command(['scanner:run',
                    '--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
                    '--format', 'json',
                    '--severity-threshold', '5'
                ])
                .it('Ensure values above the max for severity-threshold cause an error', ctx => {
                    expect(ctx.stderr).to.contain('Expected integer less than or equal to 3 but received 5');
                });

		});

		describe('--normalize-severity flag', () => {

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures'),
					'--format', 'json',
					'--normalize-severity'
				])
				.it('Ensure normalized severity is correct', ctx => {
					// The first line is a header. Strip that away, and parse the rest as JSON.
					const output = JSON.parse(ctx.stdout.split('\n')[1]);

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

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures'),
					'--format', 'json'
				])
				.it('Ensure normalized severity is not outputted when --normalize-severity not provided', ctx => {
					// The first line is a header. Strip that away, and parse the rest as JSON.
					const output = JSON.parse(ctx.stdout.split('\n')[1]);

					for (let i=0; i<output.length; i++) {
                        for (let j=0; j<output[i].violations.length; j++) {
							expect(output[i].violations[j].normalizedSeverity).to.equal(undefined);
                        }
                    }
				});

		});

	});
});
