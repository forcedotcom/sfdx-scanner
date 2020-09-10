import {expect} from 'chai';
import {RuleResult} from '../../src/types';
import {RuleResultRecombinator} from '../../src/lib/RuleResultRecombinator';
import {OUTPUT_FORMAT} from '../../src/lib/RuleManager';
import path = require('path');

const sampleFile1 = path.join('Users', 'SomeUser', 'samples', 'sample-file1.js');
const sampleFile2 = path.join('Users', 'SomeUser', 'samples', 'sample-file2.js');
const sampleFile3 = path.join('Users', 'SomeUser', 'samples', 'sample-file3.java');

const allFakeRuleResults: RuleResult[] = [
	{
		engine: 'eslint',
		fileName: sampleFile1,
		violations: [{
			"line": 2,
			"column": 11,
			"severity": 2,
			"message": "'unusedParam' is defined but never used.",
			"ruleName": "no-unused-vars",
			"category": "Variables",
			"url": "https://eslint.org/docs/rules/no-unused-vars"
		}]
	},
	{
		engine: 'eslint',
		fileName: sampleFile2,
		violations: [{
			"line": 4,
			"column": 11,
			"severity": 1,
			"message": "'unusedParam' is defined but never used.",
			"ruleName": "no-unused-vars",
			"category": "Variables",
			"url": "https://eslint.org/docs/rules/no-unused-vars"
		}, {
			"line": 6,
			"column": 9,
			"severity": 2,
			"message": "'unusedVar' is assigned a value but never used.",
			"ruleName": "no-unused-vars",
			"category": "Variables",
			"url": "https://eslint.org/docs/rules/no-unused-vars"
		}]
	},
	{
		engine: "pmd",
		fileName: sampleFile3,
		violations: [{
			"line": 2,
			"column": 1,
			"endLine": 2,
			"endColumn": 57,
			"severity": 4,
			"ruleName": "UnusedImports",
			"category": "Best Practices",
			"url": "https://pmd.github.io/pmd-6.22.0/pmd_rules_java_bestpractices.html#unusedimports",
			"message": "\nAvoid unused imports such as 'sfdc.sfdx.scanner.messaging.SfdxMessager'\n"
		}, {
			"line": 4,
			"column": 8,
			"endLine": 56,
			"endColumn": 1,
			"severity": 3,
			"ruleName": "CommentRequired",
			"category": "Documentation",
			"url": "https://pmd.github.io/pmd-6.22.0/pmd_rules_java_documentation.html#commentrequired",
			"message": "\nEnum comments are required\n"
		}, {
			"line": 5,
			"column": 1,
			"endLine": 5,
			"endColumn": 2,
			"severity": 3,
			"ruleName": "CommentSize",
			"category": "Documentation",
			"url": "https://pmd.github.io/pmd-6.22.0/pmd_rules_java_documentation.html#commentsize",
			"message": "\nComment is too large: Line too long\n"
		}]
	}
];

function isString(x: string | {columns; rows}): x is string {
	return typeof x === 'string';
}


describe('RuleResultRecombinator', () => {
	describe('#recombineAndReformatResults()', () => {
		describe('Output Format: JUnit', () => {
			// This is a function for validating JUnit-formatted XMLs.
			function validateJUnitFormatting(lines: string[], fileNames: string[], violationCounts: number[]) {
				expect(fileNames.length).to.equal(violationCounts.length,
					`Improperly constructed rule. Supplied ${fileNames.length} names and ${violationCounts.length} violation counts`
				);
				// We expect the first line to be the <testsuites> opening tag.
				expect(lines[0]).to.equal('<testsuites>', 'First line should have been the testsuites opening tag');
				// After that, we expect to see a pattern of <testsuite> tags wrapping at least one <testcase> tag,
				// each of which wraps exactly one <failure> tag.
				let lineCounter = 1;
				let idx = 0;
				while (idx < fileNames.length) {
					const fileName = fileNames[idx];
					const expectedErrCt = violationCounts[idx];
					// Make sure the <testsuite> tag is well-formed.
					expect(lines[lineCounter]).to.equal(`<testsuite name="${fileName}" tests="${expectedErrCt}" errors="${expectedErrCt}">`,
						`Malformed testsuite opening tag @line ${lineCounter}`
					);
					lineCounter += 1;

					// Make sure we have the right number of <testcase> and <failure> tags.
					let observedErrCt = 0;
					while (observedErrCt < expectedErrCt) {
						// Pattern starts with a <testcase> tag.
						expect(lines[lineCounter]).to.match(/^<testcase name=".+">$/, `Malformed/absent testcase tag @line ${lineCounter}.`);
						lineCounter += 1;
						// Then there should be a <failure> tag.
						expect(lines[lineCounter]).to.match(/^<failure message=".+" type="\d+">$/, `Malformed/absent failure tag @line ${lineCounter}`);
						// There should be 6 lines of detail
						lineCounter += 7;
						// There should be a closing </failure> tag.
						expect(lines[lineCounter]).to.equal('</failure>', `Malformed/absent /failure tag @line ${lineCounter}`);
						lineCounter += 1;
						// There should be a </testcase> tag.
						expect(lines[lineCounter]).to.equal('</testcase>', `Malformed/absent /testcase tag @line ${lineCounter}`);
						lineCounter += 1;
						observedErrCt += 1;
					}
					// Make sure the </testsuite> tag is present.
					expect(lines[lineCounter]).to.equal("</testsuite>", `Expected testsuite closing tag @line ${lineCounter}`);
					lineCounter += 1;
					idx += 1;
				}
				// We expect the final line to be the </testsuites> closing tag.
				expect(lines[lineCounter]).to.equal('</testsuites>');
				lineCounter += 1;
				expect(lineCounter).to.equal(lines.length, `Reached unexpected end after ${lineCounter} lines instead of ${lines.length}`);
			}

			it('Properly handles one file with one violation', async () => {
				// Create a subset of the fake results containing only one file and one violation.
				const someFakeResults = [allFakeRuleResults[0]];

				// Create our reformatted results.
				const {minSev, results} = await RuleResultRecombinator.recombineAndReformatResults(someFakeResults, OUTPUT_FORMAT.JUNIT);
				// Split the results by newline character so we can make some interesting assertions.
				if (!isString(results)) {
					expect(false).to.equal(true, 'Results should have been string');
				} else {
					const resultLines = results.split('\n').map(x => x.trim());
					validateJUnitFormatting(resultLines, [sampleFile1], [1]);
					expect(minSev).to.equal(2, 'Most severe problem should have been level 2');
				}
			});

			it('Properly handles one file with multiple violations', async () => {
				// Create a subset of the fake results containing one file with multiple violations.
				const someFakeResults = [allFakeRuleResults[1]];

				// Create our reformatted results.
				const {minSev, results} = await RuleResultRecombinator.recombineAndReformatResults(someFakeResults, OUTPUT_FORMAT.JUNIT);
				// Split the results by newline character so we can make some interesting assertions.
				if (!isString(results)) {
					expect(false).to.equal(true, 'Results should have been string');
				} else {
					const resultLines = results.split('\n').map(x => x.trim());
					validateJUnitFormatting(resultLines, [sampleFile2], [2]);
					expect(minSev).to.equal(1, 'Most severe problem should have been level 1');
				}
			});

			it('Properly handles multiple files with multiple violations', async () => {
				// Create our reformatted results from the entire sample.
				const {minSev, results} = await RuleResultRecombinator.recombineAndReformatResults(allFakeRuleResults, OUTPUT_FORMAT.JUNIT);
				// Split the results by newline character so we can make some interesting assertions.
				if (!isString(results)) {
					expect(false).to.equal(true, 'Results should have been string');
				} else {
					const resultLines = results.split('\n').map(x => x.trim());
					validateJUnitFormatting(resultLines, [sampleFile1, sampleFile2, sampleFile3], [1, 2, 3]);
					expect(minSev).to.equal(1, 'Most severe problem should have been level 1');
				}
			});
		});

		describe('Output Format: JSON', () => {
			// TODO: IMPLEMENT THESE TESTS
		});

		describe('Output Format: XML', () => {
			// TODO: IMPLEMENT THESE TESTS
		});

		describe('Output Format: Table', () => {
			// TODO: IMPLEMENT THESE TESTS.
		});

		describe('Output Format: CSV', () => {

		});
	});
});
