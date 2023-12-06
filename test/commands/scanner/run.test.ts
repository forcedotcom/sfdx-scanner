import {expect} from 'chai';
// @ts-ignore
import {runCommand} from '../../TestUtils';
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

const pathToApexFolder = path.join('test', 'code-fixtures', 'apex');
const pathToSomeTestClass = path.join('test', 'code-fixtures', 'apex', 'SomeTestClass.cls');
const pathToSomeOtherTestClass = path.join('test', 'code-fixtures', 'apex', 'SomeOtherTestClass.cls');
const pathToAnotherTestClass = path.join('test', 'code-fixtures', 'apex', 'AnotherTestClass.cls');
const pathToYetAnotherTestClass = path.join('test', 'code-fixtures', 'apex', 'YetAnotherTestClass.cls');

describe('scanner:run', function () {
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
				// we expect no violations
				expect(xml.indexOf('<violation')).to.equal(-1, `Should be no violations detected in the file:\n ${xml}`);
			}

			describe('Test Case: Running rules against a single file', () => {
				it('When the file contaisn violations, they are logged out as an XML', () => {
					const output = runCommand(`scanner run --target ${pathToSomeTestClass} --ruleset ApexUnit --format xml`);
					validateXmlOutput(output.shellOutput.stdout);
				});

				it('Target path may be relative or absolute', () => {
					const output = runCommand(`scanner run --target ${path.join('.', pathToSomeTestClass)} --ruleset ApexUnit --format xml`);
					validateXmlOutput(output.shellOutput.stdout);
				});

				it('When the file contains no violations, a message is logged to the console', () => {
					const output = runCommand(`scanner run --target ${pathToYetAnotherTestClass} --ruleset ApexUnit --format xml`);
					expect(output.shellOutput.stdout).to.contain(processorMessages.getMessage('output.noViolationsDetected', ['pmd, retire-js']));
				});
			});

			describe('Test Case: Running rules against multiple specified files', () => {
				it('Both files are evaluated, and any violations are logged', () => {
					const output = runCommand(`scanner run --target "${pathToSomeTestClass},${pathToSomeOtherTestClass}" --ruleset ApexUnit --format xml`);
					// We'll split the output by the <file> tag first, so we can get each file that violated rules.
					const results = output.shellOutput.stdout.split('<result ');
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
				it('Any violations in the folder are logged as an XML', () => {
					const output = runCommand(`scanner run --target ${pathToApexFolder} --ruleset ApexUnit --format xml`);
					// We'll split the output by the <file> tag first, so we can get each file that violated rules.
					const results = output.shellOutput.stdout.split('<result ');
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
				it('Violations from each rule are logged as an XML', () => {
					const output = runCommand(`scanner run --target ${pathToAnotherTestClass} --ruleset ApexUnit,Style --format xml`);
					expect(output.shellOutput.stderr).contains(runMessages.getMessage('rulesetDeprecation'), 'Expected ruleset deprecation message');
					// We'll split the output by the <violation> tag, so we can get individual violations.
					const violations = output.shellOutput.stdout.split('<violation');
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
				const testout = 'testout.xml';
				afterEach(() => {
					if (fs.existsSync(testout)) {
						fs.unlinkSync(testout);
					}
				});

				it('Returned violations are written to file as XML', () => {
					runCommand(`scanner run --target ${pathToSomeTestClass} --ruleset ApexUnit --outfile ${testout}`);
					// Verify that the file we wanted was actually created.
					expect(fs.existsSync(testout)).to.equal(true, 'The command should have created the expected output file');
					const fileContents = fs.readFileSync(testout).toString();
					validateXmlOutput(fileContents);
				});

				it('Absence of violations yields empty XML file', () => {
					runCommand(`scanner run --target ${pathToYetAnotherTestClass} --ruleset ApexUnit --outfile ${testout}`);
					// Verify that an empty XML file was actually created.
					expect(fs.existsSync(testout)).to.equal(true, 'The command should have created an empty output file');
					const fileContents = fs.readFileSync(testout).toString();
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

				// Since it's a CSV, the rows themselves are separated by newline characters.
				// Test to check there are no violations.
				// There should be a header and no other lines, meaning no newline characters.
				expect(csv.indexOf('\n')).to.equal(-1, "Should be no violations detected");
			}

			const testout = 'testout.csv';
			afterEach(() => {
				if (fs.existsSync(testout)) {
					fs.unlinkSync(testout);
				}
			});

			it('Properly writes CSV to console', () => {
				const output = runCommand(`scanner run --target ${pathToSomeTestClass} --ruleset ApexUnit --format csv`);
				// Split the output by newline characters and throw away the first entry, so we're left with just the rows.
				validateCsvOutput(output.shellOutput.stdout, false);
			});

			it('Properly writes CSV to file', () => {
				const output = runCommand(`scanner run --target ${pathToSomeTestClass} --ruleset ApexUnit --outfile ${testout}`);
				// Verify that the correct message is displayed to user
				expect(output.shellOutput.stdout).to.contain(processorMessages.getMessage('output.engineSummaryTemplate', ['pmd', 2, 1]), 'Expected summary to be correct');
				expect(output.shellOutput.stdout).to.contain(processorMessages.getMessage('output.writtenToOutFile', [testout]));

				// Verify that the file we wanted was actually created.
				expect(fs.existsSync(testout)).to.equal(true, 'The command should have created the expected output file');
				const fileContents = fs.readFileSync(testout).toString();
				validateCsvOutput(fileContents, false);
			});

			it('When no violations are detected, a message is logged to the console', () => {
				const output = runCommand(`scanner run --target ${pathToYetAnotherTestClass} --ruleset ApexUnit --format csv`);
				expect(output.shellOutput.stdout).to.contain(processorMessages.getMessage('output.noViolationsDetected', ['pmd, retire-js']));
			});

			it('When --outfile is provided and no violations are detected, CSV file with no violations is created', () => {
				const output = runCommand(`scanner run --target ${pathToYetAnotherTestClass} --ruleset ApexUnit --outfile ${testout}`);
				expect(output.shellOutput.stdout).to.contain(processorMessages.getMessage('output.writtenToOutFile', [testout]));

				const fileContents = fs.readFileSync(testout).toString();
				expect(fs.existsSync(testout)).to.be.true;
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

			afterEach(() => {
				if (fs.existsSync(outputFile)) {
					fs.unlinkSync(outputFile);
				}
			});

			it('Properly writes HTML to console', () => {
				const output = runCommand(`scanner run --target ${pathToSomeTestClass} --ruleset ApexUnit --format html`);
				// Parse out the HTML results
				validateHtmlOutput(output.shellOutput.stdout);
			});

			it('Properly writes HTML to file', () => {
				const output = runCommand(`scanner run --target ${pathToSomeTestClass} --ruleset ApexUnit --outfile ${outputFile}`);
				// Verify that the correct message is displayed to user
				expect(output.shellOutput.stdout).to.contain(processorMessages.getMessage('output.writtenToOutFile', [outputFile]));

				// Verify that the file we wanted was actually created.
				expect(fs.existsSync(outputFile)).to.equal(true, 'The command should have created the expected output file');
				const fileContents = fs.readFileSync(outputFile).toString();
				validateHtmlOutput(fileContents);
			});

			it('When no violations are detected, a message is logged to the console', () => {
				const output = runCommand(`scanner run --target ${pathToYetAnotherTestClass} --ruleset ApexUnit --format html`);
				expect(output.shellOutput.stdout).to.contain(processorMessages.getMessage('output.noViolationsDetected', ['pmd, retire-js']));
			});

			it('When --outfile is provided and no violations are detected, HTML file with no violations should be created', () => {
				const output = runCommand(`scanner run --target ${pathToYetAnotherTestClass} --ruleset ApexUnit --outfile ${outputFile}`);
				expect(output.shellOutput.stdout).to.contain(processorMessages.getMessage('output.writtenToOutFile', [outputFile]));
				expect(fs.existsSync(outputFile)).to.be.true;
				const fileContents = fs.readFileSync(outputFile).toString();
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

			const testout = 'testout.json';
			afterEach(() => {
				if (fs.existsSync(testout)) {
					fs.unlinkSync(testout);
				}
			});

			it('Properly writes JSON to console', () => {
				const output = runCommand(`scanner run --target ${pathToSomeTestClass} --ruleset ApexUnit --format json`);
				const stdout = output.shellOutput.stdout;
				validateJsonOutput(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
			});

			it('Properly writes JSON to file', () => {
				const output = runCommand(`scanner run --target ${pathToSomeTestClass} --ruleset ApexUnit --outfile ${testout}`);
				// Verify that the correct message is displayed to user
				expect(output.shellOutput.stdout).to.contain(processorMessages.getMessage('output.engineSummaryTemplate', ['pmd', 2, 1]), 'Expected summary to be correct');
				expect(output.shellOutput.stdout).to.contain(processorMessages.getMessage('output.writtenToOutFile', [testout]));

				// Verify that the file we wanted was actually created.
				expect(fs.existsSync(testout)).to.equal(true, 'The command should have created the expected output file');
				const fileContents = fs.readFileSync(testout).toString();
				validateJsonOutput(fileContents);
			});

			it('When no violations are detected, a message is logged to the console', () => {
				const output = runCommand(`scanner run --target ${pathToYetAnotherTestClass} --ruleset ApexUnit --format json`);
				expect(output.shellOutput.stdout).to.contain(processorMessages.getMessage('output.noViolationsDetected', ['pmd, retire-js']));
			});

			it('When --outfile is provided and no violations are detected, a JSON file with no violations is created', () => {
				const output = runCommand(`scanner run --target ${pathToYetAnotherTestClass} --ruleset ApexUnit --outfile ${testout}`);
				expect(output.shellOutput.stdout).to.contain(processorMessages.getMessage('output.writtenToOutFile', [testout]));
				expect(fs.existsSync(testout)).to.be.true;
				const fileContents = fs.readFileSync(testout).toString();
				validateNoViolationsJsonOutput(fileContents);
			});

		});

		describe('Output Type: Table', () => {
			// The table can't be written to a file, so we're just testing the console.
			it('Properly writes table to the console', () => {
				const output = runCommand(`scanner run --target ${pathToSomeTestClass} --ruleset ApexUnit --format table`);
				// Split the output by newline characters and throw away the first two rows, which are the column names and a separator.
				// That will leave us with just the rows.
				const rows = output.shellOutput.stdout.trim().split('\n');

				// Assert rows have the right error on the right line.
				expect(rows.find(r => r.indexOf("SomeTestClass.cls:11") > 0)).to.contain('Apex unit tests should System.assert()');
				expect(rows.find(r => r.indexOf("SomeTestClass.cls:19") > 0)).to.contain('Apex unit tests should System.assert()');
			});

			it('When no violations are detected, an empty table is logged to the console', () => {
				const output = runCommand(`scanner run --target ${pathToYetAnotherTestClass} --ruleset ApexUnit --format table`);
				// Split the output by newline characters and throw away the first two rows, which are the column names and a separator.
				// That will leave us with just the rows.
				const rows = output.shellOutput.stdout.trim().split('\n');

				// Expect to find no violations listing this class.
				expect(rows.find(r => r.indexOf("SomeTestClass.cls") > 0)).to.equal(undefined, "more rows??");
			});
		});

		describe('--json flag', () => {
			const testout = 'testout.xml';
			afterEach(() => {
				if (fs.existsSync(testout)) {
					fs.unlinkSync(testout);
				}
			});

			it('--json flag uses default format of JSON', () => {
				const commandOutput = runCommand(`scanner run --target ${pathToSomeTestClass} --ruleset ApexUnit --json`)
				const output = JSON.parse(commandOutput.shellOutput.stdout);
				expect(output.status).to.equal(0, 'Should have finished properly');
				const result = output.result;
				// Only PMD rules should have run.
				expect(result.length).to.equal(1, 'Should only be violations from one engine');
				expect(result[0].engine).to.equal('pmd', 'Engine should be PMD');

				expect(result[0].violations.length).to.equal(2, 'Should be 2 violations');
				expect(result[0].violations[0].line).to.equal(11, 'Violation #1 should occur on the expected line');
				expect(result[0].violations[1].line).to.equal(19, 'Violation #2 should occur on the expected line');
			});

			it('--json flag wraps other formats in a string', () => {
				const commandOutput = runCommand(`scanner run --target ${pathToSomeTestClass} --ruleset ApexUnit --format xml --json`);
				const output = JSON.parse(commandOutput.shellOutput.stdout);
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

			it('--json flag wraps message about writing to outfile', () => {
				const commandOutput = runCommand(`scanner run --target ${pathToSomeTestClass} --ruleset ApexUnit --outfile ${testout} --json`);
				const output = JSON.parse(commandOutput.shellOutput.stdout);
				expect(output.status).to.equal(0, 'Should finish properly');
				const result = output.result;
				expect(result).to.contain(processorMessages.getMessage('output.writtenToOutFile', [testout]));
				// Verify that the file we wanted was actually created.
				expect(fs.existsSync(testout)).to.equal(true, 'The command should have created the expected output file');
				const fileContents = fs.readFileSync(testout).toString();
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

			it('--json flag wraps message about no violations occuring', () => {
				const commandOutput = runCommand(`scanner run --target ${pathToYetAnotherTestClass} --ruleset ApexUnit --json`);
				const output = JSON.parse(commandOutput.shellOutput.stdout);
				expect(output.status).to.equal(0, 'Should have finished properly');
				expect(output.result).to.contain(processorMessages.getMessage('output.noViolationsDetected', ['pmd, retire-js']));
			})
		});

		describe('Dynamic Input', () => {

			describe('Test Case: Using ~/ shorthand in target', () => {
				const pathWithTilde = tildify(path.join(process.cwd(), 'test', 'code-fixtures', 'apex', 'SomeTestClass.cls'));

				it('Tilde is expanded to full directory', () => {
					const output = runCommand(`scanner run --target ${pathWithTilde} --ruleset ApexUnit --format xml`);
					// We'll split the output by the <violation> tag, so we can get individual violations.
					const violations = output.shellOutput.stdout.split('<violation');
					// The first list item is going to be the header, so we need to pull that off.
					violations.shift();
					expect(violations.length).to.equal(2, 'Should be two violations detected in the file');
					// We'll check each violation in enough depth to be confident that the expected violations were returned in the
					// expected order.
					expect(violations[0]).to.match(/line="11".+rule="ApexUnitTestClassShouldHaveAsserts"/);
					expect(violations[1]).to.match(/line="19".+rule="ApexUnitTestClassShouldHaveAsserts"/);
				})
			});
		});

		describe('Edge Cases', () => {
			describe('Test case: No output specified', () => {
				it('When no format is specified, we default to a TABLE', () => {
					const output = runCommand(`scanner run --target ${pathToSomeTestClass} --ruleset ApexUnit`);
					// Split the output by newline characters and throw away the first two rows, which are the column names and a separator.
					// That will leave us with just the rows.
					const rows = output.shellOutput.stdout.trim().split('\n');
					rows.shift();
					rows.shift();

					// Assert rows have the right error on the right line.
					expect(rows.find(r => r.indexOf("SomeTestClass.cls:11") > 0)).to.contain('Apex unit tests should System.assert()');
					expect(rows.find(r => r.indexOf("SomeTestClass.cls:19") > 0)).to.contain('Apex unit tests should System.assert()');
				})
			});

			describe('Test Case: No rules specified', () => {
				it('When no rules are explicitly specified, all rules are run', () => {
					const output = runCommand(`scanner run --target ${pathToAnotherTestClass} --format xml`);
					// We'll split the output by the <violation> tag, so we can get individual violations.
					const violations = output.shellOutput.stdout.split('<violation');
					// The first list item is going to be the header, so we need to pull that off.
					violations.shift();
					// ApexUnitTestClassShouldHaveAsserts, FieldNamingConventions, UnusedLocalVariable, and VariableNamingConventions
					// We'll just make sure that we have the right number of them.
					expect(violations.length).greaterThan(0);
				});
			});
		});

		describe('Error handling', () => {
			const notcsv = 'notcsv.xml';
			afterEach(() => {
				if (fs.existsSync(notcsv)) {
					fs.unlinkSync(notcsv);
				}
			});


			it('Error thrown when output file is malformed', () => {
				const output = runCommand(`scanner run --target path/that/does/notmatter --ruleset ApexUnit --outfile NotAValidFileName`);
				expect(output.shellOutput.stderr).to.contain(`Error (1): ${commonMessages.getMessage('validations.outfileMustBeValid')}`);
			});

			it('Error thrown when output file is unsupported type', () => {
				const output = runCommand(`scanner run --target path/that/does/not/matter --ruleset ApexUnit --outfile badtype.pdf`);
				expect(output.shellOutput.stderr).to.contain(`Error (1): ${commonMessages.getMessage('validations.outfileMustBeSupportedType')}`);
			})

			it('Warning logged when output file format does not match format', () => {
				const output = runCommand(`scanner run --target path/that/does/not/matter --format csv --outfile ${notcsv}`);
				expect(output.shellOutput.stdout).to.contain(commonMessages.getMessage('validations.outfileFormatMismatch', ['csv', 'xml']));
			});
		});
	});

	describe('MultiEngine', () => {
		describe('Project: JS', () => {
			it('Polyglot project triggers pmd and eslint rules', () => {
				const pathToApp = path.join('test', 'code-fixtures', 'projects', 'app');
				const allJsGlob = path.join(pathToApp, '**', '*.js');
				const allApexGlob = path.join(pathToApp, '**', '*.cls');
				const output = runCommand(`scanner run --target "${allJsGlob},${allApexGlob}" --format json`);
				const results = JSON.parse(output.shellOutput.stdout.substring(output.shellOutput.stdout.indexOf("[{"), output.shellOutput.stdout.lastIndexOf("}]") + 2));
				// Look through all of the results and gather a set of unique engines
				const uniqueEngines = new Set(results.map(r => { return r.engine }));
				expect(uniqueEngines).to.be.an("Set").that.has.length(2);
				expect(uniqueEngines).to.contain("eslint");
				expect(uniqueEngines).to.contain("pmd");
				// Validate that all of the results have an expected property
				for (const result of results) {
					expect(result.violations[0], `Message is ${result.violations[0].message}\n ${output.shellOutput.stdout}`).to.have.property("ruleName").that.is.not.null;
				}
			});
		});
	});

	describe('BaseConfig Environment Tests For Javascript', () => {
		it('The baseConfig enables the usage of default Javascript Types', () => {
			const output = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'projects', 'js', 'src', 'baseConfigEnv.js')} --format csv`);
			// There should be no violations.
			expect(output.shellOutput.stdout).to.contain(processorMessages.getMessage('output.noViolationsDetected', ['pmd, eslint, retire-js']));
		});

		// TODO: THIS TEST WAS IMPLEMENTED FOR W-7791882. THE FIX FOR THAT BUG WAS SUB-OPTIMAL, AND WE NEED TO CHANGE IT IN 4.0.
		//       DON'T BE AFRAID TO CHANGE/DELETE THIS TEST AT THAT POINT.
		it('By default, frameworks such as QUnit are not included in the baseConfig', () => {
			const output = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'projects', 'js', 'src', 'fileThatUsesQUnit.js')} --format json`);
			// We expect there to be 2 errors about qunit-related syntax being undefined.
			const stdout = output.shellOutput.stdout;
			const parsedCtx = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
			expect(parsedCtx[0].violations.length).to.equal(2, `Should be 2 violations ${JSON.stringify(parsedCtx[0].violations)}`);
			expect(parsedCtx[0].violations[0].message).to.contain("'QUnit' is not defined.");
		});

		// TODO: THIS TEST WAS IMPLEMENTED FOR W-7791882. THE FIX FOR THAT BUG WAS SUB-OPTIMAL AND WE NEED TO REDO IT IN 4.0.
		//       DON'T BE AFRAID TO CHANGE/DELETE THIS TEST AT THAT POINT.
		it('Providing qunit in the --env override should resolve errors about that framework', () => {
			const output = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'projects', 'js', 'src', 'fileThatUsesQUnit.js')} --format json --env "{\\"qunit\\": true}"`);
			expect(output.shellOutput.stdout).to.contain(processorMessages.getMessage('output.noViolationsDetected', ['pmd, eslint, retire-js']));
		});
	});

	describe('run with format --json', () => {
		it('provides only json in stdout', () => {
			const output = runCommand(`scanner run --target ${pathToAnotherTestClass} --format json`);
			try {
				JSON.parse(output.shellOutput.stdout);
			} catch (error) {
				expect.fail("Invalid JSON output from --format json: " + output.shellOutput.stdout, error);
			}

		});
	});

	describe('Validation on custom config flags', () => {

		it('Handle --tsconfig and --eslintconfig as mutially exclusive flags and throw an informative error message', () => {
			const output = runCommand(`scanner run --target /some/path --tsconfig /some/path/tsconfig.json --eslintconfig /some/path/.eslintrc.json`);
			expect(output.shellOutput.stderr).to.contain(runMessages.getMessage('validations.tsConfigEslintConfigExclusive'));
		});

		it('Display informative message when rule filters are provided along with custom config - pmdconfig', () => {
			const output = runCommand(`scanner run --target /some/path --pmdconfig /somepath/ruleref.xml --category Security`);
			expect(output.shellOutput.stdout).to.contain(runMessages.getMessage('output.filtersIgnoredCustom'));
		});
	});

	// Any commands that specify the --verbose cause subsequent commands to execute as if --verbose was specified.
	// Put all --verbose commands at the end of this file.
	describe('Verbose tests must come last. Verbose does not reset', () => {
		it('When the --verbose flag is supplied, info about implicitly run rules is logged', () => {
			const output = runCommand(`scanner run --target ${pathToYetAnotherTestClass} --format xml --verbose`);
			// We'll split the output by the <violation> tag, so we can get individual violations.
			const violations = output.shellOutput.stdout.split('<violation');
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
			// TODO: revisit test, should be improved because of issue above
			expect(implicitMessages || []).to.have.lengthOf(15, `Entries for implicitly added categories from all engines:\n ${JSON.stringify(implicitMessages)}`);

		});
	});
});
