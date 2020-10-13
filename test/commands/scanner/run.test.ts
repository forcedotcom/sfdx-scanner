import {expect} from '@salesforce/command/lib/test';
import {setupCommandTest} from '../../TestUtils';
import {Messages} from '@salesforce/core';
import fs = require('fs');
import path = require('path');
import process = require('process');
import tildify = require('tildify');
import events = require('../../../messages/EventKeyTemplates');

Messages.importMessagesDirectory(__dirname);
const runMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run');
const eventMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'EventKeyTemplates');

describe('scanner:run', function () {
	this.timeout(10000); // TODO why do we get timeouts at the default of 5000?  What is so expensive here?

	describe('E2E', () => {
		describe('Output Type: XML', () => {
			function validateXmlOutput(xml: string): void {
				// We'll split the output by the <violation> tag, so we can get individual violations.
				const violations = xml.split('<violation');
				// The first list item is going to be the header, so we need to pull that off.
				violations.shift();
				// There should be two violations.
				expect(violations.length).to.equal(2, `Should be two violations detected in the file:\n ${xml}`);
				// We'll check each violation in enough depth to be confident that the expected violations were returned in the
				// expected order.
				expect(violations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);
				expect(violations[1]).to.match(/line="19".+rule="ApexUnitTestClassShouldHaveAsserts"/);
			}

			describe('Test Case: Running rules against a single file', () => {
				setupCommandTest
					.command(['scanner:run',
						'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
						'--ruleset', 'ApexUnit',
						'--format', 'xml'
					])
					.it('When the file contains violations, they are logged out as an XML', ctx => {
						validateXmlOutput(ctx.stdout);
					});

				setupCommandTest
					.command(['scanner:run',
						'--target', path.join('.', 'test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
						'--ruleset', 'ApexUnit',
						'--format', 'xml'
					])
					.it('Target path may be relative or absolute', ctx => {
						validateXmlOutput(ctx.stdout);
					});

				setupCommandTest
					.command(['scanner:run',
						'--target', path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls'),
						'--ruleset', 'ApexUnit',
						'--format', 'xml'
					])
					.it('When the file contains no violations, a message is logged to the console', ctx => {
						expect(ctx.stdout).to.contain(runMessages.getMessage('output.noViolationsDetected'));
					});
			});

			describe('Test Case: Running rules against multiple specified files', () => {
				setupCommandTest
					.command(['scanner:run',
						'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls') + ',' + path.join('test', 'code-fixtures', 'apex', 'SomeOtherTestClass.cls'),
						'--ruleset', 'ApexUnit',
						'--format', 'xml'
					])
					.it('Both files are evaluated, and any violations are logged', ctx => {
						// We'll split the output by the <file> tag first, so we can get each file that violated rules.
						const results = ctx.stdout.split('<result ');
						results.shift();
						// Verify that each set of violations corresponds to the expected file.
						expect(results.length).to.equal(2, 'Only two files should have violated the rules');
						expect(results[0]).to.match(/file="test(\/|\\)code-fixtures(\/|\\)apex(\/|\\)SomeOtherTestClass.cls"/);
						expect(results[1]).to.match(/file="test(\/|\\)code-fixtures(\/|\\)apex(\/|\\)SomeTestClass.cls"/);

						// Now, split each file's violations by the <violation> tag so we can inspect individual violations.
						const firstFileViolations = results[0].split('<violation');
						firstFileViolations.shift();
						expect(firstFileViolations.length).to.equal(1, 'Should be one violation detected in SomeOtherTestClass.cls');
						expect(firstFileViolations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);

						const secondFileViolations = results[1].split('<violation');
						secondFileViolations.shift();
						expect(secondFileViolations.length).to.equal(2, 'Should be two violations detected in SomeTestClass.cls');
						// We'll check each violation in enough depth to be confident that the expected violations were returned in the
						// expected order.
						expect(secondFileViolations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);
						expect(secondFileViolations[1]).to.match(/line="19".+rule="ApexUnitTestClassShouldHaveAsserts"/);
					});
			});

			describe('Test Case: Running rules against a folder', () => {
				setupCommandTest
					.command(['scanner:run',
						'--target', path.join('test', 'code-fixtures', 'apex'),
						'--ruleset', 'ApexUnit',
						'--format', 'xml'
					])
					.it('Any violations in the folder are logged as an XML', ctx => {
						// We'll split the output by the <file> tag first, so we can get each file that violated rules.
						const results = ctx.stdout.split('<result ');
						// The first list item is going to be the header, so we need to pull that off.
						results.shift();
						// Verify that each set of violations corresponds to the expected file.
						expect(results.length).to.equal(3, 'Only three files should have violated the rules');
						expect(results[0]).to.match(/file="test(\/|\\)code-fixtures(\/|\\)apex(\/|\\)AnotherTestClass.cls"/);
						expect(results[1]).to.match(/file="test(\/|\\)code-fixtures(\/|\\)apex(\/|\\)SomeOtherTestClass.cls"/);
						expect(results[2]).to.match(/file="test(\/|\\)code-fixtures(\/|\\)apex(\/|\\)SomeTestClass.cls"/);

						// Now, split each file's violations by the <violation> tag so we can inspect individual violations.
						const firstFileViolations = results[0].split('<violation');
						firstFileViolations.shift();
						expect(firstFileViolations.length).to.equal(1, 'Should be one violation detected in AnotherTestClass.cls');
						expect(firstFileViolations[0]).to.match(/line="6".+rule="ApexUnitTestClassShouldHaveAsserts"/);

						const secondFileViolations = results[1].split('<violation');
						secondFileViolations.shift();
						expect(secondFileViolations.length).to.equal(1, 'Should be one violation detected in SomeOtherTestClass.cls');
						expect(secondFileViolations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);

						const thirdFileViolations = results[2].split('<violation');
						thirdFileViolations.shift();
						expect(thirdFileViolations.length).to.equal(2, 'Should be two violations detected in SomeTestClass.cls');
						// We'll check each violation in enough depth to be confident that the expected violations were returned in the
						// expected order.
						expect(thirdFileViolations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);
						expect(thirdFileViolations[1]).to.match(/line="19".+rule="ApexUnitTestClassShouldHaveAsserts"/);
					});
			});

			describe('Test Case: Running multiple rulesets at once', () => {
				setupCommandTest
					.command(['scanner:run', '--target', path.join('test', 'code-fixtures', 'apex', 'AnotherTestClass.cls'),
						'--ruleset', 'ApexUnit',
						'--format', 'xml'])
					.it('--ruleset option shows deprecation warning', ctx => {
						expect(ctx.stderr).contains(runMessages.getMessage('rulesetDeprecation'));
					});

				setupCommandTest
					.command(['scanner:run',
						'--target', path.join('test', 'code-fixtures', 'apex', 'AnotherTestClass.cls'),
						'--ruleset', 'ApexUnit,Style',
						'--format', 'xml'
					])
					.it('Violations from each rule are logged as an XML', ctx => {
						// We'll split the output by the <violation> tag, so we can get individual violations.
						const violations = ctx.stdout.split('<violation');
						// The first list item is going to be the header, so we need to pull that off.
						violations.shift();
						expect(violations.length).to.equal(2, 'Should be two violations detected in the file');
						// We'll check each violation in enough depth to be confident that the expected violations were returned in the
						// expected order.
						expect(violations[0]).to.match(/line="3".+rule="VariableNamingConventions"/);
						expect(violations[1]).to.match(/line="6".+rule="ApexUnitTestClassShouldHaveAsserts"/);
					});
			});

			describe('Test Case: Writing XML results to a file', () => {
				setupCommandTest
					.command(['scanner:run',
						'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
						'--ruleset', 'ApexUnit',
						'--outfile', 'testout.xml'
					])
					.finally(ctx => {
						// Regardless of what happens in the test itself, we need to delete the file we created.
						if (fs.existsSync('testout.xml')) {
							fs.unlinkSync('testout.xml');
						}
					})
					.it('The violations are written to the file as an XML', ctx => {
						// Verify that the file we wanted was actually created.
						expect(fs.existsSync('testout.xml')).to.equal(true, 'The command should have created the expected output file');
						const fileContents = fs.readFileSync('testout.xml').toString();
						validateXmlOutput(fileContents);
					});
			});
		});

		describe('Output Type: CSV', () => {
			function validateCsvOutput(csv: string): void {
				const rows = csv.trim().split('\n');
				rows.shift();

				// There should be two rows.
				expect(rows.length).to.equal(2, 'Should be two violations detected');

				// Split each row by commas, so we'll have each cell.
				const data = rows.map(val => val.split(','));
				// Verify that each row looks approximately right.
				expect(data[0][3]).to.equal('"11"', 'Violation #1 should occur on the expected line');
				expect(data[1][3]).to.equal('"19"', 'Violation #2 should occur on the expected line');
				expect(data[0][5]).to.equal('"ApexUnitTestClassShouldHaveAsserts"', 'Violation #1 should be of the expected type');
				expect(data[1][5]).to.equal('"ApexUnitTestClassShouldHaveAsserts"', 'Violation #2 should be of the expected type');
			}

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--format', 'csv'
				])
				.it('Properly writes CSV to console', ctx => {
					// Split the output by newline characters and throw away the first entry, so we're left with just the rows.
					validateCsvOutput(ctx.stdout);
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--outfile', 'testout.csv'
				])
				.finally(ctx => {
					// Regardless of what happens in the test itself, we need to delete the file we created.
					if (fs.existsSync('testout.csv')) {
						fs.unlinkSync('testout.csv');
					}
				})
				.it('Properly writes CSV to file', ctx => {
					// Verify that the correct message is displayed to user
					expect(ctx.stdout).to.contain(runMessages.getMessage('output.writtenToOutFile', ['testout.csv']));
					expect(ctx.stdout).to.not.contain(runMessages.getMessage('output.noViolationsDetected', []));

					// Verify that the file we wanted was actually created.
					expect(fs.existsSync('testout.csv')).to.equal(true, 'The command should have created the expected output file');
					const fileContents = fs.readFileSync('testout.csv').toString();
					validateCsvOutput(fileContents);
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--format', 'csv'
				])
				.it('When no violations are detected, a message is logged to the console', ctx => {
					expect(ctx.stdout).to.contain(runMessages.getMessage('output.noViolationsDetected'));
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--outfile', 'testout.csv'
				])
				.finally(ctx => {
					// Regardless of what happens in the test itself, we need to delete the file we created.
					if (fs.existsSync('testout.csv')) {
						fs.unlinkSync('testout.csv');
					}
				})
				.it('When --oufile is provided and no violations are detected, output file should not be created', ctx => {
					expect(ctx.stdout).to.contain(runMessages.getMessage('output.noViolationsDetected', []));
					expect(ctx.stdout).to.not.contain(runMessages.getMessage('output.writtenToOutFile', ['testout.csv']));
					expect(fs.existsSync('testout.csv')).to.be.false;
				});
		});

		describe('Output Type: HTML', () => {
			const outputFile = 'testout.html';
			function validateHtmlOutput(html: string): void {
				const result = html.match(/const violations = (\[.*);/);
				expect(result).to.be.not.null;
				expect(result[1]).to.be.not.null;
				const rows = JSON.parse(result[1]);

				expect(rows.length).to.equal(2);

				// Verify that each row looks approximately right.
				expect(rows[0]['line']).to.equal('11', 'Violation #1 should occur on the expected line');
				expect(rows[1]['line']).to.equal('19', 'Violation #2 should occur on the expected line');
				expect(rows[0]['ruleName']).to.equal('ApexUnitTestClassShouldHaveAsserts', 'Violation #1 should be of the expected type');
				expect(rows[1]['ruleName']).to.equal('ApexUnitTestClassShouldHaveAsserts', 'Violation #2 should be of the expected type');
			}

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--format', 'html'
				])
				.it('Properly writes HTML to console', ctx => {
					// Parse out the JSON results
					validateHtmlOutput(ctx.stdout);
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--outfile', outputFile
				])
				.finally(ctx => {
					// Regardless of what happens in the test itself, we need to delete the file we created.
					if (fs.existsSync(outputFile)) {
						fs.unlinkSync(outputFile);
					}
				})
				.it('Properly writes HTML to file', ctx => {
					// Verify that the correct message is displayed to user
					expect(ctx.stdout).to.contain(runMessages.getMessage('output.writtenToOutFile', [outputFile]));
					expect(ctx.stdout).to.not.contain(runMessages.getMessage('output.noViolationsDetected', []));

					// Verify that the file we wanted was actually created.
					expect(fs.existsSync(outputFile)).to.equal(true, 'The command should have created the expected output file');
					const fileContents = fs.readFileSync(outputFile).toString();
					validateHtmlOutput(fileContents);
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--format', 'html'
				])
				.it('When no violations are detected, a message is logged to the console', ctx => {
					expect(ctx.stdout).to.contain(runMessages.getMessage('output.noViolationsDetected'));
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--outfile', outputFile
				])
				.finally(ctx => {
					// Regardless of what happens in the test itself, we need to delete the file we created.
					if (fs.existsSync(outputFile)) {
						fs.unlinkSync(outputFile);
					}
				})
				.it('When --oufile is provided and no violations are detected, output file should not be created', ctx => {
					expect(ctx.stdout).to.contain(runMessages.getMessage('output.noViolationsDetected', []));
					expect(ctx.stdout).to.not.contain(runMessages.getMessage('output.writtenToOutFile', [outputFile]));
					expect(fs.existsSync(outputFile)).to.be.false;
				});
		});

		describe('Output Type: JSON', () => {
			function validateJsonOutput(json: string): void {
				const output = JSON.parse(json);
				// Only PMD rules should have run.
				expect(output.length).to.equal(1, 'Should only be violations from one engine');
				expect(output[0].engine).to.equal('pmd', 'Engine should be PMD');

				expect(output[0].violations.length).to.equal(2, 'Should be 2 violations');
				expect(output[0].violations[0].line).to.equal('11', 'Violation #1 should occur on the expected line');
				expect(output[0].violations[1].line).to.equal('19', 'Violation #2 should occur on the expected line');
			}

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--format', 'json'
				])
				.it('Properly writes JSON to console', ctx => {
					validateJsonOutput(ctx.stdout);
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--outfile', 'testout.json'
				])
				.finally(ctx => {
					// Regardless of what happens in the test itself, we need to delete the file we created.
					if (fs.existsSync('testout.json')) {
						fs.unlinkSync('testout.json');
					}
				})
				.it('Properly writes JSON to file', ctx => {
					// Verify that the correct message is displayed to user
					expect(ctx.stdout).to.contain(runMessages.getMessage('output.writtenToOutFile', ['testout.json']));
					expect(ctx.stdout).to.not.contain(runMessages.getMessage('output.noViolationsDetected', []));

					// Verify that the file we wanted was actually created.
					expect(fs.existsSync('testout.json')).to.equal(true, 'The command should have created the expected output file');
					const fileContents = fs.readFileSync('testout.json').toString();
					validateJsonOutput(fileContents);
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--format', 'json'
				])
				.it('When no violations are detected, a message is logged to the console', ctx => {
					expect(ctx.stdout).to.contain(runMessages.getMessage('output.noViolationsDetected'));
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--outfile', 'testout.json'
				])
				.finally(ctx => {
					// Regardless of what happens in the test itself, we need to delete the file we created.
					if (fs.existsSync('testout.json')) {
						fs.unlinkSync('testout.json');
					}
				})
				.it('When --oufile is provided and no violations are detected, output file should not be created', ctx => {
					expect(ctx.stdout).to.contain(runMessages.getMessage('output.noViolationsDetected', []));
					expect(ctx.stdout).to.not.contain(runMessages.getMessage('output.writtenToOutFile', ['testout.json']));
					expect(fs.existsSync('testout.json')).to.be.false;
				});

		});

		describe('Output Type: Table', () => {
			// The table can't be written to a file, so we're just testing the console.
			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--format', 'table'
				])
				.it('Properly writes table to the console', ctx => {
					// Split the output by newline characters and throw away the first two rows, which are the column names and a separator.
					// That will leave us with just the rows.
					const rows = ctx.stdout.trim().split('\n');

					// Assert rows have the right error on the right line.
					expect(rows.find(r => r.indexOf("SomeTestClass.cls:11") > 0)).to.contain('Apex unit tests should System.assert()');
					expect(rows.find(r => r.indexOf("SomeTestClass.cls:19") > 0)).to.contain('Apex unit tests should System.assert()');
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--format', 'table'
				])
				.it('When no violations are detected, a message is logged to the console', ctx => {
					expect(ctx.stdout).to.contain(runMessages.getMessage('output.noViolationsDetected'));
				});
		});

		describe('--json flag', () => {
			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--json'
				])
				.it('--json flag uses default format of JSON', ctx => {
					const output = JSON.parse(ctx.stdout);
					expect(output.status).to.equal(0, 'Should have finished properly');
					const result = output.result;
					// Only PMD rules should have run.
					expect(result.length).to.equal(1, 'Should only be violations from one engine');
					expect(result[0].engine).to.equal('pmd', 'Engine should be PMD');

					expect(result[0].violations.length).to.equal(2, 'Should be 2 violations');
					expect(result[0].violations[0].line).to.equal('11', 'Violation #1 should occur on the expected line');
					expect(result[0].violations[1].line).to.equal('19', 'Violation #2 should occur on the expected line');
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--format', 'xml',
					'--json'
				])
				.it('--json flag wraps other formats in a string', ctx => {
					const output = JSON.parse(ctx.stdout);
					expect(output.status).to.equal(0, 'Should have finished properly');
					// We'll split the output by the <violation> tag, so we can get individual violations.
					const violations = output.result.split('<violation');
					// The first list item is going to be the header, so we need to pull that off.
					violations.shift();
					// There should be two violations.
					expect(violations.length).to.equal(2, 'Should be two violations detected in the file');
					// We'll check each violation in enough depth to be confident that the expected violations were returned in the
					// expected order.
					expect(violations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);
					expect(violations[1]).to.match(/line="19".+rule="ApexUnitTestClassShouldHaveAsserts"/);
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--outfile', 'testout.xml',
					'--json'
				])
				.finally(ctx => {
					// Regardless of what happens in the test itself, we need to delete the file we created.
					if (fs.existsSync('testout.xml')) {
						fs.unlinkSync('testout.xml');
					}
				})
				.it('--json flag wraps message about writing to outfile', ctx => {
					const output = JSON.parse(ctx.stdout);
					expect(output.status).to.equal(0, 'Should finish properly');
					const result = output.result;
					expect(result).to.contain(runMessages.getMessage('output.writtenToOutFile', ['testout.xml']));
					expect(result).to.not.contain(runMessages.getMessage('output.noViolationsDetected', []));
					// Verify that the file we wanted was actually created.
					expect(fs.existsSync('testout.xml')).to.equal(true, 'The command should have created the expected output file');
					const fileContents = fs.readFileSync('testout.xml').toString();
					// We'll split the output by the <violation> tag, so we can get individual violations.
					const violations = fileContents.split('<violation');
					// The first list item is going to be the header, so we need to pull that off.
					violations.shift();
					expect(violations.length).to.equal(2, 'Should be two violations detected in the file');
					// We'll check each violation in enough depth to be confident that the expected violations were returned in the
					// expected order.
					expect(violations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);
					expect(violations[1]).to.match(/line="19".+rule="ApexUnitTestClassShouldHaveAsserts"/);
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--json'
				])
				.it('--json flag wraps message about no violations occuring', ctx => {
					const output = JSON.parse(ctx.stdout);
					expect(output.status).to.equal(0, 'Should have finished properly');
					expect(output.result).to.contain(runMessages.getMessage('output.noViolationsDetected'));
				});
		});

		describe('--violations-cause-error flag', () => {

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--format', 'xml',
					'--violations-cause-error'
				])
				.it('When no violations are found, no error is thrown', ctx => {
					expect(ctx.stdout).to.contain(runMessages.getMessage('output.noViolationsDetected'));
					expect(ctx.stderr).to.not.contain(runMessages.getMessage('output.pleaseSeeAbove'), 'Error should not be present');
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--format', 'table',
					'--violations-cause-error'
				])
				.it('When violations are found, an error is thrown', ctx => {
					// Split the output by newline characters and throw away the first two rows, which are the column names and a separator.
					// That will leave us with just the rows.
					const rows = ctx.stdout.trim().split('\n');

					// Assert rows have the right error on the right line.
					expect(rows.find(r => r.indexOf("SomeTestClass.cls:11") > 0)).to.contain('Apex unit tests should System.assert()');
					expect(rows.find(r => r.indexOf("SomeTestClass.cls:19") > 0)).to.contain('Apex unit tests should System.assert()');
					expect(ctx.stderr).to.contain(runMessages.getMessage('output.pleaseSeeAbove'), 'Error should be present');
				});
		});

		describe('Eslint Javascript Engine --category flag', () => {
			const category = 'Stylistic Issues';
			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'projects', 'app', 'force-app', 'main', 'default', 'aura', 'dom_parser', 'dom_parserController.js'),
					'--format', 'json',
					'--engine', 'eslint',
					'--category', category
				])
				.it('Only correct categories are returned', ctx => {
					const output = JSON.parse(ctx.stdout);
					expect(output.length).to.equal(1, 'Should only be violations from one engine');
					expect(output[0].engine).to.equal('eslint');
					expect(output[0].violations, JSON.stringify(output[0].violations)).to.be.lengthOf(55);

					// Make sure only violations are returned for the requested category
					for (const v of output[0].violations) {
						expect(v.category, JSON.stringify(v)).to.equal(category);
					}
				});

		});

		describe('Eslint Javascript Engine --category flag', () => {
			const category = 'Best Practices';
			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'projects', 'app', 'force-app', 'main', 'default', 'aura', 'dom_parser', 'dom_parserController.js'),
					'--format', 'json',
					'--engine', 'eslint-lwc',
					'--category', category
				])
				.it('Only correct categories are returned', ctx => {
					const output = JSON.parse(ctx.stdout);
					expect(output.length).to.equal(1, 'Should only be violations from one engine');
					expect(output[0].engine).to.equal('eslint-lwc');
					expect(output[0].violations, JSON.stringify(output[0].violations)).to.be.lengthOf(13);

					// Make sure only violations are returned for the requested category
					for (const v of output[0].violations) {
						expect(v.category, JSON.stringify(v)).to.equal(category);
					}
				});

		});

		describe('Eslint Typescript Engine --category flag', () => {
			const category = 'Possible Errors';
			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'projects', 'ts', 'src', 'simpleYetWrong.ts'),
					'--tsconfig', path.join('test', 'code-fixtures', 'projects', 'tsconfig.json'),
					'--format', 'json',
					'--engine', 'eslint-typescript',
					'--category', category
				])
				.it('Only correct categories are returned', ctx => {
					const output = JSON.parse(ctx.stdout);
					expect(output.length).to.equal(1, 'Should only be violations from one engine');
					expect(output[0].engine).to.equal('eslint-typescript');
					expect(output[0].violations, JSON.stringify(output[0].violations)).to.be.lengthOf(2);

					// Make sure only violations are returned for the requested category
					for (const v of output[0].violations) {
						expect(v.category, JSON.stringify(v)).to.equal(category);
					}
				});

		});

		describe('PMD Engine --category flag', () => {
			const category = 'Code Style';
			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex'),
					'--format', 'json',
					'--engine', 'pmd',
					'--category', category
				])
				.it('Only correct categories are returned', ctx => {
					const output = JSON.parse(ctx.stdout);
					expect(output.length).to.equal(1, 'Should only be violations from one engine');
					expect(output[0].engine).to.equal('pmd');
					expect(output[0].violations, JSON.stringify(output[0].violations)).to.be.lengthOf(2);

					// Make sure only violations are returned for the requested category
					for (const v of output[0].violations) {
						expect(v.category, JSON.stringify(v)).to.equal(category);
					}
				});

		});

		// TODO: this test has become more of an integration test, than just testing the run command. break it up to make it more manageable, maybe based on engine or flags.

		describe('--engine flag', () => {
			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'lwc'),
					'--format', 'csv',
					'--engine', 'eslint-lwc'
				])
				.it('LWC Engine Successfully parses LWC code', ctx => {
					expect(ctx.stdout).to.contain('No rule violations found.');
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'invalid-lwc'),
					'--format', 'json',
					'--engine', 'eslint-lwc'
				])
				.it('LWC Engine detects LWC errors', ctx => {
					const results = JSON.parse(ctx.stdout);
					expect(results, `results does not have expected length. ${results.map(r => r.fileName).join(',')}`)
						.to.be.an('Array').that.has.length(1);
					const messages = results[0].violations.map(v => v.message);
					const expectedMessages = ['Invalid public property initialization for "foo". Boolean public properties should not be initialized to "true", consider initializing the property to "false".',
						`'Foo' is defined but never used.`];
					for (const expectedMessage of expectedMessages) {
						expect(messages).to.contain(expectedMessage);
					}
				});
		});

		describe('Dynamic Input', () => {
			describe('Positive Globs', () => {
				describe('Test Case: Running rules against one positive glob', () => {
					setupCommandTest
						.command(['scanner:run',
							// NOTE: When running the command for real, a glob would have to be wrapped in SINGLE-QUOTES. But the tests sidestep that, somehow.
							'--target', 'test/code-fixtures/apex/Some*.cls',
							'--ruleset', 'ApexUnit',
							'--format', 'xml'
						])
						.it('Glob is resolved to files, and those files are evaluated', ctx => {
							// We'll split the output by the <file> tag first, so we can get each file that violated rules.
							const results = ctx.stdout.split('<result ');
							// The first list item is going to be the header, so we need to pull that off.
							results.shift();
							// Verify that each set of violations corresponds to the expected file.
							expect(results.length).to.equal(2, 'Only two files should have violated the rules');
							expect(results[0]).to.match(/file="test(\/|\\)code-fixtures(\/|\\)apex(\/|\\)SomeOtherTestClass.cls"/);
							expect(results[1]).to.match(/file="test(\/|\\)code-fixtures(\/|\\)apex(\/|\\)SomeTestClass.cls"/);

							// Now, split each file's violations by the <violation> tag so we can inspect individual violations.
							const firstFileViolations = results[0].split('<violation');
							firstFileViolations.shift();
							expect(firstFileViolations.length).to.equal(1, 'Should be one violation detected in SomeOtherTestClass.cls');
							expect(firstFileViolations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);

							const secondFileViolations = results[1].split('<violation');
							secondFileViolations.shift();
							expect(secondFileViolations.length).to.equal(2, 'Should be two violations detected in SomeTestClass.cls');
							// We'll check each violation in enough depth to be confident that the expected violations were returned in the
							// expected order.
							expect(secondFileViolations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);
							expect(secondFileViolations[1]).to.match(/line="19".+rule="ApexUnitTestClassShouldHaveAsserts"/);
						});
				});

				describe('Test Case: Running rules against multiple positive globs', () => {
					setupCommandTest
						.command(['scanner:run',
							// NOTE: When running the command for real, a glob would have to be wrapped in SINGLE-QUOTES. But the tests sidestep that, somehow.
							'--target', 'test/code-fixtures/apex/A*.cls,test/code-fixtures/apex/S*.cls',
							'--ruleset', 'ApexUnit',
							'--format', 'xml'
						])
						.it('Files matching even a single positive glob are evaluated', ctx => {
							// We'll split the output by the <file> tag first, so we can get each file that violated rules.
							const results = ctx.stdout.split('<result ');
							// The first list item is going to be the header, so we need to pull that off.
							results.shift();
							// Verify that each set of violations corresponds to the expected file.
							expect(results.length).to.equal(3, 'Only three files should have violated the rules');
							expect(results[0]).to.match(/file="test(\/|\\)code-fixtures(\/|\\)apex(\/|\\)AnotherTestClass.cls"/);
							expect(results[1]).to.match(/file="test(\/|\\)code-fixtures(\/|\\)apex(\/|\\)SomeOtherTestClass.cls"/);
							expect(results[2]).to.match(/file="test(\/|\\)code-fixtures(\/|\\)apex(\/|\\)SomeTestClass.cls"/);

							// Now, split each file's violations by the <violation> tag so we can inspect individual violations.
							const firstFileViolations = results[0].split('<violation');
							firstFileViolations.shift();
							expect(firstFileViolations.length).to.equal(1, 'Should be one violation detected in AnotherTestClass.cls');
							expect(firstFileViolations[0]).to.match(/line="6".+rule="ApexUnitTestClassShouldHaveAsserts"/);

							const secondFileViolations = results[1].split('<violation');
							secondFileViolations.shift();
							expect(secondFileViolations.length).to.equal(1, 'Should be one violation detected in SomeOtherTestClass.cls');
							expect(secondFileViolations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);

							const thirdFileViolations = results[2].split('<violation');
							thirdFileViolations.shift();
							expect(thirdFileViolations.length).to.equal(2, 'Should be two violations detected in SomeTestClass.cls');
							// We'll check each violation in enough depth to be confident that the expected violations were returned in the
							// expected order.
							expect(thirdFileViolations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);
							expect(thirdFileViolations[1]).to.match(/line="19".+rule="ApexUnitTestClassShouldHaveAsserts"/);
						});
				});
			});

			describe('Negative Globs', () => {
				setupCommandTest
					.command(['scanner:run',
						// NOTE: When running the command for real, a glob would have to be wrapped in SINGLE-QUOTES.
						// But the tests sidestep that somehow.
						'--target', 'test/code-fixtures/apex/*Class.cls,!**/A*,!**/*Other*',
						'--ruleset', 'ApexUnit',
						'--format', 'xml'
					])
					.it('When mixing negative and positive globs, files must match ALL negative globs to be evaluated', ctx => {
						// We'll split the output by the <file> tag first, so we can get each file that violated rules.
						const results = ctx.stdout.split('<result ');
						// The first list item is going to be the header, so we need to pull that off.
						results.shift();
						// Verify that each set of violations corresponds to the expected file.
						expect(results.length).to.equal(1, 'Only one file should have violated the rules');
						expect(results[0]).to.match(/file="test(\/|\\)code-fixtures(\/|\\)apex(\/|\\)SomeTestClass.cls"/);

						// Now, split each file's violations by the <violation> tag so we can inspect individual violations.
						const fileViolations = results[0].split('<violation');
						fileViolations.shift();
						expect(fileViolations.length).to.equal(2, 'Should be two violations detected in SomeTestClass.cls');
						// We'll check each violation in enough depth to be confident that the expected violations were returned in the
						// expected order.
						expect(fileViolations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);
						expect(fileViolations[1]).to.match(/line="19".+rule="ApexUnitTestClassShouldHaveAsserts"/);
					});

				setupCommandTest
					.command(['scanner:run',
						// NOTE: When running the command for real, a glob would have to be wrapped in SINGLE-QUOTES.
						// But the tests sidestep that somehow.
						'--target', 'test/code-fixtures/apex/*Class.cls,!test/code-fixtures/apex/A*,!./test/code-fixtures/apex/*Other*',
						'--ruleset', 'ApexUnit',
						'--format', 'xml'
					])
					.it('Relative negative globs are properly processed', ctx => {
						// We'll split the output by the <file> tag first, so we can get each file that violated rules.
						const results = ctx.stdout.split('<result ');
						// The first list item is going to be the header, so we need to pull that off.
						results.shift();
						// Verify that each set of violations corresponds to the expected file.
						expect(results.length).to.equal(1, 'Only one file should have violated the rules');
						expect(results[0]).to.match(/file="test(\/|\\)code-fixtures(\/|\\)apex(\/|\\)SomeTestClass.cls"/);

						// Now, split each file's violations by the <violation> tag so we can inspect individual violations.
						const fileViolations = results[0].split('<violation');
						fileViolations.shift();
						expect(fileViolations.length).to.equal(2, 'Should be two violations detected in SomeTestClass.cls');
						// We'll check each violation in enough depth to be confident that the expected violations were returned in the
						// expected order.
						expect(fileViolations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);
						expect(fileViolations[1]).to.match(/line="19".+rule="ApexUnitTestClassShouldHaveAsserts"/);
					});

				setupCommandTest
					.command(['scanner:run',
						// NOTE: When running the command for real, a glob would have to be wrapped in SINGLE-QUOTES.
						// But the tests sidestep that somehow.
						'--target', 'test/code-fixtures/apex,!**/A*,!**/*Other*',
						'--ruleset', 'ApexUnit',
						'--format', 'xml'
					])
					.it('When mixing negative globs and directories, files must match ALL negative globs to be evaluated', ctx => {
						// We'll split the output by the <file> tag first, so we can get each file that violated rules.
						const results = ctx.stdout.split('<result ');
						// The first list item is going to be the header, so we need to pull that off.
						results.shift();
						// Verify that each set of violations corresponds to the expected file.
						expect(results.length).to.equal(1, 'Only one file should have violated the rules');
						expect(results[0]).to.match(/file="test(\/|\\)code-fixtures(\/|\\)apex(\/|\\)SomeTestClass.cls"/);

						// Now, split each file's violations by the <violation> tag so we can inspect individual violations.
						const fileViolations = results[0].split('<violation');
						fileViolations.shift();
						expect(fileViolations.length).to.equal(2, 'Should be two violations detected in SomeTestClass.cls');
						// We'll check each violation in enough depth to be confident that the expected violations were returned in the
						// expected order.
						expect(fileViolations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);
						expect(fileViolations[1]).to.match(/line="19".+rule="ApexUnitTestClassShouldHaveAsserts"/);
					});
			});

			describe('Test Case: Using ~/ shorthand in target', () => {
				const pathWithTilde = tildify(path.join(process.cwd(), 'test', 'code-fixtures', 'apex', 'SomeTestClass.cls'));
				setupCommandTest
					.command(['scanner:run',
						'--target', pathWithTilde,
						'--ruleset', 'ApexUnit',
						'--format', 'xml'
					])
					.it('Tilde is expanded to full directory', ctx => {
						// We'll split the output by the <violation> tag, so we can get individual violations.
						const violations = ctx.stdout.split('<violation');
						// The first list item is going to be the header, so we need to pull that off.
						violations.shift();
						expect(violations.length).to.equal(2, 'Should be two violations detected in the file');
						// We'll check each violation in enough depth to be confident that the expected violations were returned in the
						// expected order.
						expect(violations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);
						expect(violations[1]).to.match(/line="19".+rule="ApexUnitTestClassShouldHaveAsserts"/);
					});
			});
		});

		describe('Edge Cases', () => {
			describe('Test case: No output specified', () => {
				setupCommandTest
					.command(['scanner:run',
						'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
						'--ruleset', 'ApexUnit'
					])
					.it('When no format is specified, we default to a TABLE', ctx => {
						// Split the output by newline characters and throw away the first two rows, which are the column names and a separator.
						// That will leave us with just the rows.
						const rows = ctx.stdout.trim().split('\n');
						rows.shift();
						rows.shift();

						// Assert rows have the right error on the right line.
						expect(rows.find(r => r.indexOf("SomeTestClass.cls:11") > 0)).to.contain('Apex unit tests should System.assert()');
						expect(rows.find(r => r.indexOf("SomeTestClass.cls:19") > 0)).to.contain('Apex unit tests should System.assert()');
					});
			});

			describe('Test Case: No rules specified', () => {
				setupCommandTest
					.command(['scanner:run',
						'--target', path.join('test', 'code-fixtures', 'apex', 'AnotherTestClass.cls'),
						'--format', 'xml'
					])
					.it('When no rules are explicitly specified, all rules are run', ctx => {
						// We'll split the output by the <violation> tag, so we can get individual violations.
						const violations = ctx.stdout.split('<violation');
						// The first list item is going to be the header, so we need to pull that off.
						violations.shift();
						// ApexUnitTestClassShouldHaveAsserts, FieldNamingConventions, UnusedLocalVariable, and VariableNamingConventions
						// We'll just make sure that we have the right number of them.
						expect(violations.length).to.equal(4, 'Violations detected in the file');
					});
			});

			describe('Test Case: Evaluating rules against invalid code', () => {
				const pathToBadSyntax = path.join('test', 'code-fixtures', 'invalid-apex', 'BadSyntax1.cls');
				const pathToGoodSyntax = path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls');
				setupCommandTest
					.command(['scanner:run',
						'--ruleset', 'ApexUnit',
						'--target', pathToBadSyntax,
						'--format', 'xml'
					])
					.it('When only malformed code is supplied, no violations are detected but a warning is logged', ctx => {
						// Expect the output to include the "No violations" string.
						expect(ctx.stdout).to.contain(runMessages.getMessage('output.noViolationsDetected'), 'No violations should be found');
						// Expect stderr to include the warning indicating that the file's output was skipped. We don't care much
						// about the message from PMD, so just replace it with an empty string so it doesn't fail anything.
						expect(ctx.stderr).to.contain(eventMessages.getMessage('warning.pmdSkippedFile', [path.resolve(pathToBadSyntax), '']), 'Warning should be displayed');
					});

				setupCommandTest
					.command(['scanner:run',
						'--ruleset', 'ApexUnit',
						'--target', `${pathToBadSyntax},${pathToGoodSyntax}`,
						'--format', 'xml'
					])
					.it('When a malformed file and a valid file are supplied, the malformed file does not tank the process', ctx => {
						// stdout should be the same as if we'd only run against the good file.
						// We'll split the output by the <violation> tag, so we can get individual violations.
						const violations = ctx.stdout.split('<violation');
						// The first list item is going to be the header, so we need to pull that off.
						violations.shift();
						expect(violations.length).to.equal(2, 'Should be two violations detected in the file');
						// We'll check each violation in enough depth to be confident that the expected violations were returned in the
						// expected order.
						expect(violations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);
						expect(violations[1]).to.match(/line="19".+rule="ApexUnitTestClassShouldHaveAsserts"/);
						// stderr should include the warning indicating that the file was skipped.
						expect(ctx.stderr).to.contain(eventMessages.getMessage('warning.pmdSkippedFile', [path.resolve(pathToBadSyntax), '']), 'Warning should be displayed');
					});
			});
		});

		describe('Error handling', () => {
			setupCommandTest
				.command(['scanner:run', '--ruleset', 'ApexUnit', '--format', 'xml'])
				.it('Error thrown when no target is specified', ctx => {
					expect(ctx.stderr).to.contain(`ERROR running scanner:run:  ${runMessages.getMessage('validations.mustTargetSomething')}`);
				});

			setupCommandTest
				.command(['scanner:run', '--target', 'path/that/does/not/matter', '--ruleset', 'ApexUnit', '--outfile', 'NotAValidFileName'])
				.it('Error thrown when output file is malformed', ctx => {
					expect(ctx.stderr).to.contain(`ERROR running scanner:run:  ${runMessages.getMessage('validations.outfileMustBeValid')}`);
				});

			setupCommandTest
				.command(['scanner:run', '--target', 'path/that/does/not/matter', '--ruleset', 'ApexUnit', '--outfile', 'badtype.pdf'])
				.it('Error thrown when output file is unsupported type', ctx => {
					expect(ctx.stderr).to.contain(`ERROR running scanner:run:  ${runMessages.getMessage('validations.outfileMustBeSupportedType')}`);
				});

			setupCommandTest
				.command(['scanner:run', '--target', 'path/that/does/not/matter', '--format', 'csv', '--outfile', 'notcsv.xml'])
				.it('Warning logged when output file format does not match format', ctx => {
					expect(ctx.stdout).to.contain(runMessages.getMessage('validations.outfileFormatMismatch', ['csv', 'xml']));
				});
		});
	});

	describe('MultiEngine', () => {
		describe('Project: JS', () => {
			setupCommandTest
				.do(() => process.chdir(path.join('test', 'code-fixtures', 'projects', 'app')))
				.command(['scanner:run', '--target', '**/*.js,**/*.cls', '--format', 'json'])
				.finally(() => process.chdir("../../../.."))
				.it('Polyglot project triggers pmd and eslint rules', ctx => {
					expect(ctx.stderr, ctx.stdout).to.be.empty;
					const results = JSON.parse(ctx.stdout.substring(ctx.stdout.indexOf("[{")));
					// Look through all of the results and gather a set of unique engines
					const uniqueEngines = new Set(results.map(r => { return r.engine }));
					expect(uniqueEngines).to.be.an("Set").that.has.length(2);
					expect(uniqueEngines).to.contain("eslint");
					expect(uniqueEngines).to.contain("pmd");
					// Validate that all of the results have an expected property
					for (const result of results) {
						expect(result.violations[0], `Message is ${result.violations[0].message}\n ${ctx.stdout}`).to.have.property("ruleName").that.is.not.null;
					}
				});
		});
	});

	describe('BaseConfig Environment Tests For Javascript', () => {
		setupCommandTest
			.command(['scanner:run',
				'--target', path.join('test', 'code-fixtures', 'projects', 'js', 'src', 'baseConfigEnv.js'),
				'--format', 'csv'
			])
			.it('The baseConfig enables the usage of default Javascript Types', ctx => {
				// There should be no violations.
				expect(ctx.stdout).to.contains('No rule violations found.', 'Should be no violations found in the file.');
			});

		// TODO: THIS TEST WAS IMPLEMENTED FOR W-7791882. THE FIX FOR THAT BUG WAS SUB-OPTIMAL, AND WE NEED TO CHANGE IT IN 3.0.
		//       DON'T BE AFRAID TO CHANGE/DELETE THIS TEST AT THAT POINT.
		setupCommandTest
			.command(['scanner:run',
				'--target', path.join('test', 'code-fixtures', 'projects', 'js', 'src', 'fileThatUsesQUnit.js'),
				'--format', 'json'
			])
			.it('By default, frameworks such as QUnit are not included in the baseConfig', ctx => {
				// We expect there to be 2 errors about qunit-related syntax being undefined.
				const parsedCtx = JSON.parse(ctx.stdout);
				expect(parsedCtx[0].violations.length).to.equal(6, `Should be 2 violations ${JSON.stringify(parsedCtx[0].violations)}`);
				expect(parsedCtx[0].violations[0].message).to.contain("'QUnit' is not defined.");
			});

		// TODO: THIS TEST WAS IMPLEMENTED FOR W-7791882. THE FIX FOR THAT BUG WAS SUB-OPTIMAL AND WE NEED TO REDO IT IN 3.0.
		//       DON'T BE AFRAID TO CHANGE/DELETE THIS TEST AT THAT POINT.
		setupCommandTest
			.command(['scanner:run',
				'--target', path.join('test', 'code-fixtures', 'projects', 'js', 'src', 'fileThatUsesQUnit.js'),
				'--format', 'json',
				'--env', '{"qunit": true}'
			])
			.it('Providing qunit in the --env override should resolve errors about that framework', ctx => {
				expect(ctx.stdout).to.not.contain('No rule violations found.', 'Should be no violations found in the file.');
			});
	});

	// Any commands that specify the --verbose cause subsequent commands to execute as if --verbose was specified.
	// Put all --verbose commands at the end of this file.
	describe('Verbose tests must come last. Verbose does not reset', () => {
		setupCommandTest
			.command(['scanner:run',
				'--target', path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls'),
				'--format', 'xml',
				'--verbose'
			])
			.it('When the --verbose flag is supplied, info about implicitly run rules is logged', ctx => {
				// We'll split the output by the <violation> tag, so we can get individual violations.
				const violations = ctx.stdout.split('<violation');
				// Before the violations are logged, there should be 16 log runMessages about implicitly included PMD categories.
				const regex = new RegExp(events.info.categoryImplicitlyRun.replace(/%s/g, '.*'), 'g');
				const implicitMessages = violations[0].match(regex);
				expect(implicitMessages || []).to.have.lengthOf(33, `Entries for implicitly added categories from all engines:\n ${JSON.stringify(implicitMessages)}`);
			});
	});
});
