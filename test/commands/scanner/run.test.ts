import {expect} from '@salesforce/command/lib/test';
import {setupCommandTest} from '../../TestUtils';
import {Messages} from '@salesforce/core';
import fs = require('fs');
import path = require('path');
import process = require('process');
import tildify = require('tildify');
import events = require('../../../messages/EventKeyTemplates');

Messages.importMessagesDirectory(__dirname);
const processorMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'RunOutputProcessor');
const commonMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run-common');
const runMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run-pathless');

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

			function validateNoViolationsXmlOutput(xml: string): void {
				// We'll split the output by the <violation> tag, so we can get individual violations.
				const violations = xml.split('<violation');
				// The first list item is going to be the header, so we need to pull that off.
				violations.shift();
				// we expect no violations
				expect(violations.length).to.equal(0, `Should be no violations detected in the file:\n ${xml}`);
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
					.it('When the file contains no violations, XML with no violations is logged to the console', ctx => {
						validateNoViolationsXmlOutput(ctx.stdout);
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
				setupCommandTest
					.command(['scanner:run',
						'--target', path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls'),
						'--ruleset', 'ApexUnit',
						'--outfile', 'testout.xml'
					])
					.finally(ctx => {
						// Regardless of what happens in the test itself, we need to delete the file we created.
						if (fs.existsSync('testout.xml')) {
							fs.unlinkSync('testout.xml');
						}
					})
					.it('An XML file with no violations is created', ctx => {
						// Verify that an empty XML file was actually created.
						expect(fs.existsSync('testout.xml')).to.equal(true, 'The command should have created an empty output file');
						const fileContents = fs.readFileSync('testout.xml').toString();
						validateNoViolationsXmlOutput(fileContents);
					});
			});
		});

		describe('Output Type: CSV', () => {
			function validateCsvOutput(contents: string, expectSummary=true): void {
				// If there's a summary, then it'll be separated from the CSV by an empty line.
				const [csv, summary] = contents.trim().split(/\n\r?\n/);
				if (expectSummary) {
					expect(summary).to.not.equal(undefined, 'Expected summary to be not undefined');
					expect(summary).to.not.equal(null, 'Expected summary to be not null');
					expect(summary).to.contain(processorMessages.getMessage('output.engineSummaryTemplate', ['pmd', 2, 1]), 'Summary should be correct');
				}
				// Since it's a CSV, the rows themselves are separated by newline characters, and there's a header row we
				// need to discard.
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

			function validateNoViolationsCsvOutput(contents: string, expectSummary=true): void {
				// If there's a summary, then it'll be separated from the CSV by an empty line.
				const [csv, summary] = contents.trim().split(/\n\r?\n/);
				if (expectSummary) {
					expect(summary).to.not.equal(undefined, 'Expected summary to be not undefined');
					expect(summary).to.not.equal(null, 'Expected summary to be not null');
					expect(summary).to.contain(processorMessages.getMessage('output.engineSummaryTemplate', ['pmd', 2, 1]), 'Summary should be correct');
				}
				// Since it's a CSV, the rows themselves are separated by newline characters, and there's a header row we
				// need to discard.
				const rows = csv.trim().split('\n');
				rows.shift();

				// There should be no rows (besides the header) because there are no violations.
				expect(rows.length).to.equal(0, 'Should be two violations detected');
			}

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--format', 'csv'
				])
				.it('Properly writes CSV to console', ctx => {
					// Split the output by newline characters and throw away the first entry, so we're left with just the rows.
					validateCsvOutput(ctx.stdout, false);
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
					expect(ctx.stdout).to.contain(processorMessages.getMessage('output.engineSummaryTemplate', ['pmd', 2, 1]), 'Expected summary to be correct');
					expect(ctx.stdout).to.contain(processorMessages.getMessage('output.writtenToOutFile', ['testout.csv']));
					expect(ctx.stdout).to.not.contain(processorMessages.getMessage('output.noViolationsDetected', []));

					// Verify that the file we wanted was actually created.
					expect(fs.existsSync('testout.csv')).to.equal(true, 'The command should have created the expected output file');
					const fileContents = fs.readFileSync('testout.csv').toString();
					validateCsvOutput(fileContents, false);
				});

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--format', 'csv'
				])
				.it('When no violations are detected, empty CSV is printed to the console', ctx => {
					validateNoViolationsCsvOutput(ctx.stdout, false);
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
				.it('When --outfile is provided and no violations are detected, CSV file with no violations is created', ctx => {
					expect(ctx.stdout).to.contain(processorMessages.getMessage('output.writtenToOutFile', ['testout.csv']));

					const fileContents = fs.readFileSync('testout.csv').toString();
					expect(fs.existsSync('testout.csv')).to.be.true;
					validateNoViolationsCsvOutput(fileContents, false);
				});
		});

		describe('Output Type: HTML', () => {
			const outputFile = 'testout.hTmL';
			function validateHtmlOutput(html: string): void {
				const result = html.match(/const violations = (\[.*);/);
				expect(result).to.be.not.null;
				expect(result[1]).to.be.not.null;
				const rows = JSON.parse(result[1]);

				expect(rows.length).to.equal(2);

				// Verify that each row looks approximately right.
				expect(rows[0]['line']).to.equal(11, 'Violation #1 should occur on the expected line');
				expect(rows[1]['line']).to.equal(19, 'Violation #2 should occur on the expected line');
				expect(rows[0]['ruleName']).to.equal('ApexUnitTestClassShouldHaveAsserts', 'Violation #1 should be of the expected type');
				expect(rows[1]['ruleName']).to.equal('ApexUnitTestClassShouldHaveAsserts', 'Violation #2 should be of the expected type');
			}

			function validateNoViolationsHtmlOutput(html: string): void {
				// there should be no instance of a filled violations object
				const result = html.match(/const violations = (\[.+\]);/);
				expect(result).to.be.null;
				// there should be an empty violations object
				const emptyResult = html.match(/const violations = \[\];/);
				expect(emptyResult).to.be.not.null;
			}

			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--format', 'html'
				])
				.it('Properly writes HTML to console', ctx => {
					// Parse out the HTML results
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
					expect(ctx.stdout).to.contain(processorMessages.getMessage('output.writtenToOutFile', [outputFile]));
					expect(ctx.stdout).to.not.contain(processorMessages.getMessage('output.noViolationsDetected', []));

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
				.it('When no violations are detected, HTML with no violations is logged to the console', ctx => {
					validateNoViolationsHtmlOutput(ctx.stdout);
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
				.it('When --outfile is provided and no violations are detected, HTML file with no violations should be created', ctx => {
					expect(ctx.stdout).to.contain(processorMessages.getMessage('output.writtenToOutFile', [outputFile]));
					expect(fs.existsSync(outputFile)).to.be.true;
					const fileContents = fs.readFileSync('testout.html').toString();
					validateNoViolationsHtmlOutput(fileContents);
				});
		});

		describe('Output Type: JSON', () => {
			function validateJsonOutput(json: string): void {
				const output = JSON.parse(json);
				// Only PMD rules should have run.
				expect(output.length).to.equal(1, 'Should only be violations from one engine');
				expect(output[0].engine).to.equal('pmd', 'Engine should be PMD');

				expect(output[0].violations.length).to.equal(2, 'Should be 2 violations');
				expect(output[0].violations[0].line).to.equal(11, 'Violation #1 should occur on the expected line');
				expect(output[0].violations[1].line).to.equal(19, 'Violation #2 should occur on the expected line');
			}

			function validateNoViolationsJsonOutput(json: string): void {
				const output = JSON.parse(json);
				// There should be no violations.
				expect(output.length).to.equal(0, 'Should be no violations from one engine');
			}


			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls'),
					'--ruleset', 'ApexUnit',
					'--format', 'json'
				])
				.it('Properly writes JSON to console', ctx => {
					const stdout = ctx.stdout;
					validateJsonOutput(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
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
					expect(ctx.stdout).to.contain(processorMessages.getMessage('output.engineSummaryTemplate', ['pmd', 2, 1]), 'Expected summary to be correct');
					expect(ctx.stdout).to.contain(processorMessages.getMessage('output.writtenToOutFile', ['testout.json']));
					expect(ctx.stdout).to.not.contain(processorMessages.getMessage('output.noViolationsDetected', []));

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
				.it('When no violations are detected, JSON with no violations is logged to the console', ctx => {
					validateNoViolationsJsonOutput(ctx.stdout);
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
				.it('When --outfile is provided and no violations are detected, a JSON file with no violations is created', ctx => {
					expect(ctx.stdout).to.contain(processorMessages.getMessage('output.writtenToOutFile', ['testout.json']));
					expect(fs.existsSync('testout.json')).to.be.true;
					const fileContents = fs.readFileSync('testout.json').toString();
					validateNoViolationsJsonOutput(fileContents);
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
				.it('When no violations are detected, an empty table is logged to the console', ctx => {
					// Split the output by newline characters and throw away the first two rows, which are the column names and a separator.
					// That will leave us with just the rows.
					const rows = ctx.stdout.trim().split('\n');

					// Expect to find no violations listing this class.
					expect(rows.find(r => r.indexOf("SomeTestClass.cls") > 0)).to.equal(undefined, "more rows??");
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
					expect(result[0].violations[0].line).to.equal(11, 'Violation #1 should occur on the expected line');
					expect(result[0].violations[1].line).to.equal(19, 'Violation #2 should occur on the expected line');
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
					expect(result).to.contain(processorMessages.getMessage('output.writtenToOutFile', ['testout.xml']));
					expect(result).to.not.contain(processorMessages.getMessage('output.noViolationsDetected', []));
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
					expect(output.result).to.not.contain(processorMessages.getMessage('output.noViolationsDetected', ['pmd, retire-js']));
					expect(output.result.length).to.equal(0, 'When no violations are present, JSON result should be empty array.')
				});
		});

		// TODO: this test has become more of an integration test, than just testing the run command. break it up to make it more manageable, maybe based on engine or flags.

		describe('--engine flag', () => {
		});

		describe('Dynamic Input', () => {

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
						expect(violations.length).greaterThan(0);
					});
			});
		});

		describe('Error handling', () => {
			setupCommandTest
				.command(['scanner:run', '--target', 'path/that/does/not/matter', '--ruleset', 'ApexUnit', '--outfile', 'NotAValidFileName'])
				.it('Error thrown when output file is malformed', ctx => {
					expect(ctx.stderr).to.contain(`ERROR running scanner:run:  ${commonMessages.getMessage('validations.outfileMustBeValid')}`);
				});

			setupCommandTest
				.command(['scanner:run', '--target', 'path/that/does/not/matter', '--ruleset', 'ApexUnit', '--outfile', 'badtype.pdf'])
				.it('Error thrown when output file is unsupported type', ctx => {
					expect(ctx.stderr).to.contain(`ERROR running scanner:run:  ${commonMessages.getMessage('validations.outfileMustBeSupportedType')}`);
				});

			setupCommandTest
				.command(['scanner:run', '--target', 'path/that/does/not/matter', '--format', 'csv', '--outfile', 'notcsv.xml'])
				.finally(ctx => {
					// Regardless of what happens in the test itself, we need to delete the file we created.
					if (fs.existsSync('notcsv.xml')) {
						fs.unlinkSync('notcsv.xml');
					}
				})
				.it('Warning logged when output file format does not match format', ctx => {
					expect(ctx.stdout).to.contain(commonMessages.getMessage('validations.outfileFormatMismatch', ['csv', 'xml']));
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
					const results = JSON.parse(ctx.stdout.substring(ctx.stdout.indexOf("[{"), ctx.stdout.lastIndexOf("}]") + 2));
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
				// If there's a summary, then it'll be separated from the CSV by an empty line. Throw it away.
				const [csv, _] = ctx.stdout.trim().split(/\n\r?\n/);

				// Since it's a CSV, the rows themselves are separated by newline characters, and there's a header row we
				// need to discard.
				const rows = csv.trim().split('\n');
				rows.shift();

				// There should be no rows (besides the header) because there are no violations.
				expect(rows.length).to.equal(0, 'Should be two violations detected');
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
				const stdout = ctx.stdout;
				const parsedCtx = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
				expect(parsedCtx[0].violations.length).to.equal(2, `Should be 2 violations ${JSON.stringify(parsedCtx[0].violations)}`);
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
				// If there's a summary, then it'll be separated from the CSV by an empty line. Throw it away.
				const [csv, _] = ctx.stdout.trim().split(/\n\r?\n/);

				// Since it's a CSV, the rows themselves are separated by newline characters, and there's a header row we
				// need to discard.
				const rows = csv.trim().split('\n');
				rows.shift();

				// There should be no rows (besides the header) because there are no violations.
				expect(rows.length).to.equal(0, 'Should be two violations detected');
			});
	});

	describe('run with format --json', () => {
		setupCommandTest
		.command(['scanner:run',
		'--target', path.join('test', 'code-fixtures', 'apex', 'AnotherTestClass.cls'),
		'--format', 'json'
	])
	.it('provides only json in stdout', ctx => {
		try {
			JSON.parse(ctx.stdout);
		} catch (error) {
			expect.fail("Invalid JSON output from --format json: " + ctx.stdout, error);
		}

		});
	});

	describe('Validation on custom config flags', () => {
		setupCommandTest
			.command(['scanner:run',
				'--target', '/some/path',
				'--tsconfig', '/some/path/tsconfig.json',
				'--eslintconfig', '/some/path/.eslintrc.json'
			])
			.it('Handle --tsconfig and --eslintconfig as mutially exclusive flags and throw an informative error message', ctx => {
				expect(ctx.stderr).to.contain(runMessages.getMessage('validations.tsConfigEslintConfigExclusive'));
			});

		setupCommandTest
			.command(['scanner:run',
				'--target', '/some/path',
				'--pmdconfig', '/some/path/ruleref.xml',
				'--category', 'Security'
			])
			.it('Display informative message when rule filters are provided along with custom config - pmdconfig', ctx => {
				expect(ctx.stdout).to.contain(runMessages.getMessage('output.filtersIgnoredCustom'));
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
				// if this test is not passing and the output seems very large, that's because the test reruns on failures,
				// and the output accumulates each time, so the output on failure is not the true length of the output
				// from individual runs. To get what the actual value is, divide the value in the test failure by 6, since
				// there are five retries in addition to the initial run.
				// Note: Please keep this up-to-date. It will make it way easier to debug if needed.
				// The following categories are implicitly included:
				// - 8 PMD categories
				// - 3 ESLint categories
				// - 3 ESLint-Typescript categories
				// - 1 RetireJS category
				// For a total of 15
				expect(implicitMessages || []).to.have.lengthOf(15, `Entries for implicitly added categories from all engines:\n ${JSON.stringify(implicitMessages)}`);
				// TODO: revisit test, should be improved because of issue above
			});
	});
});
