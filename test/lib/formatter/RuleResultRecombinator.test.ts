import {expect} from 'chai';
import {RuleResult, RuleViolation} from '../../../src/types';
import {RuleResultRecombinator} from '../../../src/lib/formatter/RuleResultRecombinator';
import {OUTPUT_FORMAT} from '../../../src/lib/RuleManager';
import path = require('path');
import * as csvParse from 'csv-parse';
import {parseString} from 'xml2js';
import * as TestOverrides from '../../test-related-lib/TestOverrides';
import { ENGINE, PMD_VERSION } from '../../../src/Constants';
import { fail } from 'assert';

const sampleFile1 = path.join('Users', 'SomeUser', 'samples', 'sample-file1.js');
const sampleFile2 = path.join('Users', 'SomeUser', 'samples', 'sample-file2.js');
const sampleFile3 = path.join('Users', 'SomeUser', 'samples', 'sample-file3.java');
const sampleFile4 = path.join('Users', 'SomeUser', 'samples', 'file-with-&.js');

const edgeCaseResults: RuleResult[] = [
	{
		engine: 'eslint',
		fileName: sampleFile4,
		violations: [{
			"line": 2,
			"column": 11,
			"severity": 2,
			"message": "message with newline \n",
			"ruleName": "Rulename with newline \n",
			"category": "Category with newline \n",
			"url": "https://eslint.org/docs/rules/no-unused-vars"
		},
		{
			"line": 2,
			"column": 11,
			"severity": 2,
			"message": "messsage with comma ,",
			"ruleName": "Rulename with comma ,",
			"category": "Category with comma ,",
			"url": "https://eslint.org/docs/rules/no-unused-vars?val=one,two"
		},
		{
			"line": 2,
			"column": 11,
			"severity": 2,
			"message": "message with quote \"",
			"ruleName": "Rulename with quote \"",
			"category": "Category with quote \"",
			"url": "https://eslint.org/docs/rules/no-unused-vars"
		},
		{
			"line": 2,
			"column": 11,
			"severity": 2,
			"message": "message with ampersand &",
			"ruleName": "Rulename with ampersand &",
			"category": "Category with ampersand &",
			"url": "https://eslint.org/docs/rules/no-unused-vars?foo1=bar1&foo2=bar2"
		},
		{
			"line": 2,
			"column": 11,
			"severity": 2,
			"message": "message with >",
			"ruleName": "Rulename with >",
			"category": "Category with >",
			"url": "https://eslint.org/docs/rules/no-unused-vars"
		}]
	}
];

const allFakeRuleResults: RuleResult[] = [
	{
		engine: 'eslint',
		fileName: sampleFile1,
		violations: [{
			"line": 2,
			"column": 11,
			"severity": 2,
			"message": "'unusedParam1' is defined but never used.",
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
			"message": "'unusedParam2' is defined but never used.",
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
			"url": "https://eslint.org/docs/rules/no-unused-vars",
			"exception": true
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
			"ruleName": "ApexAssertionsShouldIncludeMessage",
			"category": "Best Practices",
			"url": "https://pmd.github.io/pmd-6.22.0/pmd_rules_java_bestpractices.html#apexassertionsshouldincludemessage",
			"message": "\nAvoid unused imports such as 'sfdc.sfdx.scanner.messaging.SfdxMessager'\n"
		}, {
			"line": 4,
			"column": 8,
			"endLine": 56,
			"endColumn": 1,
			"severity": 3,
			"ruleName": "ApexDoc",
			"category": "Documentation",
			"url": "https://pmd.github.io/pmd-6.22.0/pmd_rules_java_documentation.html#apexdoc",
			"message": "\nEnum comments are required\n"
		}, {
			"line": 5,
			"column": 1,
			"endLine": 5,
			"endColumn": 2,
			"severity": 3,
			"ruleName": "ApexUnitTestClassShouldHaveAsserts",
			"category": "Best Practices",
			"url": "https://pmd.github.io/pmd-6.22.0/pmd_rules_java_documentation.html#commentsize",
			"message": "\nComment is too large: Line too long\n"
		}]
	}
];

const allFakeRuleResultsNormalized: RuleResult[] = [
	{
		engine: 'eslint',
		fileName: sampleFile1,
		violations: [{
			"line": 2,
			"column": 11,
			"severity": 2,
			"normalizedSeverity": 1,
			"message": "'unusedParam1' is defined but never used.",
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
			"normalizedSeverity": 2,
			"message": "'unusedParam2' is defined but never used.",
			"ruleName": "no-unused-vars",
			"category": "Variables",
			"url": "https://eslint.org/docs/rules/no-unused-vars"
		}, {
			"line": 6,
			"column": 9,
			"severity": 2,
			"normalizedSeverity": 1,
			"message": "'unusedVar' is assigned a value but never used.",
			"ruleName": "no-unused-vars",
			"category": "Variables",
			"url": "https://eslint.org/docs/rules/no-unused-vars",
			"exception": true
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
			"normalizedSeverity": 3,
			"ruleName": "ApexAssertionsShouldIncludeMessage",
			"category": "Best Practices",
			"url": "https://pmd.github.io/pmd-6.22.0/pmd_rules_java_bestpractices.html#apexassertionsshouldincludemessage",
			"message": "\nAvoid unused imports such as 'sfdc.sfdx.scanner.messaging.SfdxMessager'\n"
		}, {
			"line": 4,
			"column": 8,
			"endLine": 56,
			"endColumn": 1,
			"severity": 3,
			"normalizedSeverity": 3,
			"ruleName": "ApexDoc",
			"category": "Documentation",
			"url": "https://pmd.github.io/pmd-6.22.0/pmd_rules_java_documentation.html#apexdoc",
			"message": "\nEnum comments are required\n"
		}, {
			"line": 5,
			"column": 1,
			"endLine": 5,
			"endColumn": 2,
			"severity": 3,
			"normalizedSeverity": 3,
			"ruleName": "ApexUnitTestClassShouldHaveAsserts",
			"category": "Best Practices",
			"url": "https://pmd.github.io/pmd-6.22.0/pmd_rules_java_documentation.html#commentsize",
			"message": "\nComment is too large: Line too long\n"
		}]
	}
];

function isString(x: string | {columns; rows}): x is string {
	return typeof x === 'string';
}

function validateJson(ruleResult: RuleResult, expectedResults: RuleResult[], expectedRuleResultIndex: number, expectedViolationIndex: number, trimMessage: boolean, normalizeSeverity: boolean): void {
	const expectedRuleResult = expectedResults[expectedRuleResultIndex];
	const expectedViolation: RuleViolation = expectedRuleResult.violations[expectedViolationIndex];
	const violation: RuleViolation = ruleResult.violations[expectedViolationIndex];
	expect(ruleResult.fileName).to.equal(expectedRuleResult.fileName, 'Filename');
	expect(ruleResult.engine).to.equal(expectedRuleResult.engine, 'Engine');
	expect(violation.severity).to.equal(expectedViolation.severity, 'Severity');
	if (normalizeSeverity) {
		expect(violation.normalizedSeverity).to.equal(expectedViolation.normalizedSeverity, 'Normalized Severity');
	} else {
		expect(violation.normalizedSeverity).to.equal(undefined);
	}
	expect(violation.line).to.equal(expectedViolation.line, 'Line');
	expect(violation.column).to.equal(expectedViolation.column, 'Column');
	expect(violation.ruleName).to.equal(expectedViolation.ruleName, 'Rule Name');
	const expectedMessage = trimMessage ? expectedViolation.message.trim() : expectedViolation.message;
	expect(violation.message).to.equal(expectedMessage);
	expect(violation.url).to.equal(expectedViolation.url, 'Url');
	expect(violation.category).to.equal(expectedViolation.category, 'Category');
}

describe('RuleResultRecombinator', () => {
	beforeEach(() => TestOverrides.initializeTestSetup());

	describe('#recombineAndReformatResults()', () => {
		describe('Output Format: JUnit', () => {
			// This is a function for validating JUnit-formatted XMLs.
			function validateJUnitFormatting(lines: string[], fileNames: string[], violationCounts: number[]): void {
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
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(someFakeResults, {format: OUTPUT_FORMAT.JUNIT, normalizeSeverity: false}, new Set(['eslint', 'pmd']));
				// Split the results by newline character so we can make some interesting assertions.
				if (!isString(results)) {
					expect(false).to.equal(true, 'Results should have been string');
				} else {
					const resultLines = results.split('\n').map(x => x.trim());
					validateJUnitFormatting(resultLines, [sampleFile1], [1]);
					expect(minSev).to.equal(2, 'Most severe problem should have been level 2');
					expect(summaryMap.size).to.equal(2, 'All engines supposedly executed should be present in the summary map');
					expect(summaryMap.get('pmd')).to.deep.equal({fileCount: 0, violationCount: 0}, 'Since no PMD violations were provided, none should be summarized');
					expect(summaryMap.get('eslint')).to.deep.equal({fileCount: 1, violationCount: 1}, 'Since ESLint violations were provided, they should be summarized');
				}
			});

			it('Properly handles one file with multiple violations', async () => {
				// Create a subset of the fake results containing one file with multiple violations.
				const someFakeResults = [allFakeRuleResults[1]];

				// Create our reformatted results.
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(someFakeResults, {format: OUTPUT_FORMAT.JUNIT, normalizeSeverity: false}, new Set(['eslint', 'pmd']));
				// Split the results by newline character so we can make some interesting assertions.
				if (!isString(results)) {
					expect(false).to.equal(true, 'Results should have been string');
				} else {
					const resultLines = results.split('\n').map(x => x.trim());
					validateJUnitFormatting(resultLines, [sampleFile2], [2]);
					expect(minSev).to.equal(1, 'Most severe problem should have been level 1');
					expect(summaryMap.size).to.equal(2, 'Each supposedly executed engine needs a summary');
					expect(summaryMap.get('pmd')).to.deep.equal({fileCount: 0, violationCount: 0}, 'PMD summary should be correct');
					expect(summaryMap.get('eslint')).to.deep.equal({fileCount: 1, violationCount: 2}, 'ESLint summary should be correct');
				}
			});

			it('Properly handles multiple files with multiple violations', async () => {
				// Create our reformatted results from the entire sample.
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeRuleResults, {format: OUTPUT_FORMAT.JUNIT, normalizeSeverity: false}, new Set(['eslint', 'pmd']));
				// Split the results by newline character so we can make some interesting assertions.
				if (!isString(results)) {
					expect(false).to.equal(true, 'Results should have been string');
				} else {
					const resultLines = results.split('\n').map(x => x.trim());
					validateJUnitFormatting(resultLines, [sampleFile1, sampleFile2, sampleFile3], [1, 2, 3]);
					expect(minSev).to.equal(1, 'Most severe problem should have been level 1');
					expect(summaryMap.size).to.equal(2, 'Each supposedly executed engine needs a summary');
					expect(summaryMap.get('pmd')).to.deep.equal({fileCount: 1, violationCount: 3}, 'PMD summary should be correct');
					expect(summaryMap.get('eslint')).to.deep.equal({fileCount: 2, violationCount: 3}, 'ESLint summary should be correct');
				}
			});
		});

		describe('Output Format: Sarif', () => {
			function validateEslintSarif(run: unknown, normalizeSeverity: boolean): void {
				const driver = run['tool']['driver'];
				expect(driver.name).to.equal('eslint');
				expect(driver.version).to.equal('6.8.0');
				expect(driver.informationUri).to.equal('https://eslint.org');

				// tool.driver.rules
				expect(driver['rules']).to.have.lengthOf(1, 'Rules');
				const rule = driver['rules'][0];
				expect(rule.id).to.equal('no-unused-vars');
				expect(rule.shortDescription.text).to.equal('disallow unused variables');
				expect(rule.properties.category).to.equal('Variables');
				expect(rule.properties.severity).to.equal(2);
				if (normalizeSeverity) {
					expect(rule.properties.normalizedSeverity).to.equal(1);
				} else {
					expect(rule.properties.normalizedSeverity).to.equal(undefined);
				}
				expect(rule.helpUri).to.equal('https://eslint.org/docs/rules/no-unused-vars');

				// one of the violations has 'exception=2'. It will end up in the toolExecutionNotifications node
				expect(run['results']).to.have.lengthOf(2, 'Results');
				const results = run['results'];
				for (let i=0; i<results.length; i++) {
					const result = results[i];
					expect(result.ruleId).to.equal('no-unused-vars');
					expect(result.ruleIndex).to.equal(0);
					expect(result.locations[0].physicalLocation.artifactLocation.uri).to.satisfy(l => l.startsWith('file://'));
				}

				let result = results[0];
				expect(result.level).to.equal('error');
				expect(result.message.text).to.equal(`'unusedParam1' is defined but never used.`);
				expect(result.locations).to.have.lengthOf(1, 'Locations');
				expect(result.locations[0].physicalLocation.artifactLocation.uri).to.satisfy(l => l.endsWith('Users/SomeUser/samples/sample-file1.js'));
				expect(result.locations[0].physicalLocation.region.startLine).to.equal(2);
				expect(result.locations[0].physicalLocation.region.startColumn).to.equal(11);

				result = results[1];
				expect(result.level).to.equal('warning');
				expect(result.message.text).to.equal(`'unusedParam2' is defined but never used.`);
				expect(result.locations).to.have.lengthOf(1, 'Locations');
				expect(result.locations[0].physicalLocation.artifactLocation.uri).to.satisfy(l => l.endsWith('Users/SomeUser/samples/sample-file2.js'));
				expect(result.locations[0].physicalLocation.region.startLine).to.equal(4);
				expect(result.locations[0].physicalLocation.region.startColumn).to.equal(11);

				// invocations. any violations with exception=true will show up here
				expect(run['invocations']).to.have.lengthOf(1, 'Invocations');
				const invocation = run['invocations'][0];
				expect(invocation.executionSuccessful).to.be.false;
				expect(invocation.toolExecutionNotifications).to.have.lengthOf(1, 'Tool Execution');
				const toolExecution = invocation.toolExecutionNotifications[0];
				expect(toolExecution.message.text).to.equal(`'unusedVar' is assigned a value but never used.`);
				expect(toolExecution.locations).to.have.lengthOf(1, 'Locations');
				expect(toolExecution.locations[0].physicalLocation.artifactLocation.uri).to.satisfy(l => l.startsWith('file://'));
				expect(toolExecution.locations[0].physicalLocation.artifactLocation.uri).to.satisfy(l => l.endsWith('Users/SomeUser/samples/sample-file2.js'));
			}

			function validatePMDSarif(run: unknown, normalizeSeverity: boolean): void {
				const driver = run['tool']['driver'];
				expect(driver.name).to.equal('pmd');
				expect(driver.version).to.equal(PMD_VERSION);
				expect(driver.informationUri).to.equal('https://pmd.github.io/pmd');

				// tool.driver.rules
				expect(driver['rules']).to.have.lengthOf(3, 'Rules');
				expect(run['results']).to.have.lengthOf(3, 'Results');
				expect(run['tool']['driver']['rules']).to.have.lengthOf(3, 'Rules');

				let rule = driver['rules'][0];
				expect(rule.id).to.equal('ApexAssertionsShouldIncludeMessage');
				expect(rule.shortDescription.text).to.equal(`The second parameter of System.assert/third parameter of System.assertEquals/System.assertNotEquals is a message.\nHaving a second/third parameter provides more information and makes it easier to debug the test failure and\nimproves the readability of test output.`);
				expect(rule.properties.category).to.equal('Best Practices');
				expect(rule.properties.severity).to.equal(4);
				if (normalizeSeverity) {
					expect(rule.properties.normalizedSeverity).to.equal(3);
				} else {
					expect(rule.properties.normalizedSeverity).to.equal(undefined);
				}
				expect(rule.helpUri).to.equal('https://pmd.github.io/pmd-6.22.0/pmd_rules_java_bestpractices.html#apexassertionsshouldincludemessage');

				rule = driver['rules'][1];
				expect(rule.id).to.equal('ApexDoc');
				expect(rule.shortDescription.text).to.satisfy(r => r.indexOf('ApexDoc comments are present for classes') >= 0);
				expect(rule.properties.category).to.equal('Documentation');
				expect(rule.properties.severity).to.equal(3);
				expect(rule.helpUri).to.equal('https://pmd.github.io/pmd-6.22.0/pmd_rules_java_documentation.html#apexdoc');

				rule = driver['rules'][2];
				expect(rule.id).to.equal('ApexUnitTestClassShouldHaveAsserts');
				expect(rule.shortDescription.text).to.satisfy(r => r.startsWith(`Apex unit tests should include at least one assertion.`));
				expect(rule.properties.category).to.equal('Best Practices');
				expect(rule.properties.severity).to.equal(3);
				expect(rule.helpUri).to.equal('https://pmd.github.io/pmd-6.22.0/pmd_rules_java_documentation.html#commentsize');

				// Deep validation of results and rules is skipped, the only thing different from the eslint results
				// is that the ruleIndex should be different for each violation
				expect(run['results']).to.have.lengthOf(3, 'Results');
				const results = run['results'];

				let result = results[0];
				expect(result.ruleIndex).to.equal(0);

				result = results[1];
				expect(result.ruleIndex).to.equal(1);

				result = results[2];
				expect(result.ruleIndex).to.equal(2);

				// invocations
				expect(run['invocations']).to.have.lengthOf(1, 'Invocations');
				const invocation = run['invocations'][0];
				expect(invocation.executionSuccessful).to.be.true;
				expect(invocation.toolExecutionNotifications).to.have.lengthOf(0, 'Tool Execution');
			}

			it ('Happy Path', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeRuleResults, {format: OUTPUT_FORMAT.SARIF, normalizeSeverity: false}, new Set(['eslint', 'pmd']));
				const sarifResults: unknown[] = JSON.parse(results as string);
				expect(sarifResults['runs']).to.have.lengthOf(2, 'Runs');
				const runs = sarifResults['runs'];

				for (let i=0; i<runs.length; i++) {
					const run = runs[i];
					const engine = run['tool']['driver']['name'];
					if (engine === ENGINE.ESLINT) {
						validateEslintSarif(run, false);
					} else if (engine === ENGINE.PMD) {
						validatePMDSarif(run, false);
					} else {
						fail(`Unexpected engine: ${engine}`);
					}
				}

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(2, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('pmd')).to.deep.equal({fileCount: 1, violationCount: 3}, 'PMD summary should be correct');
				expect(summaryMap.get('eslint')).to.deep.equal({fileCount: 2, violationCount: 3}, 'ESLint summary should be correct');
			});

			it ('Happy Path - normalized', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeRuleResultsNormalized, {format: OUTPUT_FORMAT.SARIF, normalizeSeverity: true}, new Set(['eslint', 'pmd']));
				const sarifResults: unknown[] = JSON.parse(results as string);
				expect(sarifResults['runs']).to.have.lengthOf(2, 'Runs');
				const runs = sarifResults['runs'];

				for (let i=0; i<runs.length; i++) {
					const run = runs[i];
					const engine = run['tool']['driver']['name'];
					if (engine === ENGINE.ESLINT) {
						validateEslintSarif(run, true);
					} else if (engine === ENGINE.PMD) {
						validatePMDSarif(run, true);
					} else {
						fail(`Unexpected engine: ${engine}`);
					}
				}

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(2, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('pmd')).to.deep.equal({fileCount: 1, violationCount: 3}, 'PMD summary should be correct');
				expect(summaryMap.get('eslint')).to.deep.equal({fileCount: 2, violationCount: 3}, 'ESLint summary should be correct');
			});

			it ('Run with no violations returns engines that were run', async () => {
				const results = await (await RuleResultRecombinator.recombineAndReformatResults([], {format: OUTPUT_FORMAT.SARIF, normalizeSeverity: false}, new Set(['eslint', 'pmd']))).results;
				const sarifResults: unknown[] = JSON.parse(results as string);
				expect(sarifResults['runs']).to.have.lengthOf(2, 'Runs');
				const runs = sarifResults['runs'];

				for (let i=0; i<runs.length; i++) {
					const run = runs[i];
					const engine = run['tool']['driver']['name'];
					if (engine === ENGINE.ESLINT || engine === ENGINE.PMD) {
						expect(run['results']).to.have.lengthOf(0, 'Results');

						expect(run['invocations']).to.have.lengthOf(1, 'Invocations');
						const invocation = run['invocations'][0];
						expect(invocation.executionSuccessful).to.be.true;
						expect(invocation.toolExecutionNotifications).to.have.lengthOf(0, 'Tool Execution');
					} else {
						fail(`Unexpected engine: ${engine}`);
					}
				}
			});
		});

		describe('Output Format: JSON', () => {
			const messageIsTrimmed = false;
			it ('Happy Path', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeRuleResults, {format: OUTPUT_FORMAT.JSON, normalizeSeverity: false}, new Set(['eslint', 'pmd']));
				const ruleResults: RuleResult[] = JSON.parse(results as string);
				expect(ruleResults).to.have.lengthOf(3, 'Rule Results');

				// Validate each of the problem rows
				let rrIndex = 0;
				allFakeRuleResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateJson(ruleResults[rrIndex], allFakeRuleResults, rrIndex, vIndex, messageIsTrimmed, false);
						vIndex++;
					});
					rrIndex++
				});

				// Make sure we have iterated through all of the results.
				// It's one more than the number of RuleResults because of the post increment.
				expect(rrIndex).to.equal(3, 'Rule Result Index');

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(2, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('pmd')).to.deep.equal({fileCount: 1, violationCount: 3}, 'PMD summary should be correct');
				expect(summaryMap.get('eslint')).to.deep.equal({fileCount: 2, violationCount: 3}, 'ESLint summary should be correct');
			});

			it ('Happy Path - normalized', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeRuleResultsNormalized, {format: OUTPUT_FORMAT.JSON, normalizeSeverity: true}, new Set(['eslint', 'pmd']));
				const ruleResults: RuleResult[] = JSON.parse(results as string);
				expect(ruleResults).to.have.lengthOf(3, 'Rule Results');

				// Validate each of the problem rows
				let rrIndex = 0;
				allFakeRuleResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateJson(ruleResults[rrIndex], allFakeRuleResultsNormalized, rrIndex, vIndex, messageIsTrimmed, true);
						vIndex++;
					});
					rrIndex++
				});

				// Make sure we have iterated through all of the results.
				// It's one more than the number of RuleResults because of the post increment.
				expect(rrIndex).to.equal(3, 'Rule Result Index');

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(2, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('pmd')).to.deep.equal({fileCount: 1, violationCount: 3}, 'PMD summary should be correct');
				expect(summaryMap.get('eslint')).to.deep.equal({fileCount: 2, violationCount: 3}, 'ESLint summary should be correct');
			});

			it ('Edge Cases', async () => {
				const results = await (await RuleResultRecombinator.recombineAndReformatResults(edgeCaseResults, {format: OUTPUT_FORMAT.JSON, normalizeSeverity: false}, new Set(['eslint']))).results;
				const ruleResults: RuleResult[] = JSON.parse(results as string);
				expect(ruleResults).to.have.lengthOf(1, 'Rule Results');

				// Validate each of the problem rows
				let rrIndex = 0;
				edgeCaseResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateJson(ruleResults[rrIndex], edgeCaseResults, rrIndex, vIndex, messageIsTrimmed, false);
						vIndex++;
					});
					rrIndex++
				});

				// Make sure we have iterated through all of the results.
				// It's one more than the number of RuleResults because of the post increment.
				expect(rrIndex).to.equal(1, 'RuleResult Index');
			});
		});

		describe('Output Format: XML', () => {
			const messageIsTrimmed = true;
			async function convertXmlToJson(results: string, normalizeSeverity: boolean): Promise<RuleResult[]> {
				const parsedXml = await new Promise((resolve, reject) => {
					parseString(results as string, (err, output) => {
						if (err) {
							reject(err);
						}
						resolve(output);
					});
				});
				const records = parsedXml['results']['result'];

				const ruleResults: RuleResult[] = [];
				records.forEach(record => {
					const violations = [];

					record['violation'].forEach(v => {
						violations.push({
							severity: parseInt(v['$']['severity']),
							normalizedSeverity: (normalizeSeverity ? parseInt(v['$']['normalizedSeverity']) : undefined),
							line: parseInt(v['$']['line']),
							column: parseInt(v['$']['column']),
							ruleName: v['$']['rule'],
							message: v['_'],
							url: v['$']['url'],
							category: v['$']['category']
						})
					});

					const ruleResult: RuleResult = {
						engine: record['$']['engine'],
						fileName: record['$']['file'],
						violations: violations
					};
					ruleResults.push(ruleResult);
				});

				return ruleResults;
			}

			it ('Happy Path', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeRuleResults, {format: OUTPUT_FORMAT.XML, normalizeSeverity: false}, new Set(['eslint', 'pmd']));
				const ruleResults: RuleResult[] = await convertXmlToJson(results as string, false);
				expect(ruleResults).to.have.lengthOf(3, 'Rule Results');

				// Validate each of the problem rows
				let rrIndex = 0;
				allFakeRuleResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateJson(ruleResults[rrIndex], allFakeRuleResults, rrIndex, vIndex, messageIsTrimmed, false);
						vIndex++;
					});
					rrIndex++
				});

				// Make sure we have iterated through all of the results.
				// It's one more than the number of RuleResults because of the post increment.
				expect(rrIndex).to.equal(3, 'Rule Result Index');

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(2, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('pmd')).to.deep.equal({fileCount: 1, violationCount: 3}, 'PMD summary should be correct');
				expect(summaryMap.get('eslint')).to.deep.equal({fileCount: 2, violationCount: 3}, 'ESLint summary should be correct');
			});

			it ('Happy Path - normalized', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeRuleResultsNormalized, {format: OUTPUT_FORMAT.XML, normalizeSeverity: true}, new Set(['eslint', 'pmd']));
				const ruleResults: RuleResult[] = await convertXmlToJson(results as string, true);
				expect(ruleResults).to.have.lengthOf(3, 'Rule Results');

				// Validate each of the problem rows
				let rrIndex = 0;
				allFakeRuleResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateJson(ruleResults[rrIndex], allFakeRuleResultsNormalized, rrIndex, vIndex, messageIsTrimmed, true);
						vIndex++;
					});
					rrIndex++
				});

				// Make sure we have iterated through all of the results.
				// It's one more than the number of RuleResults because of the post increment.
				expect(rrIndex).to.equal(3, 'Rule Result Index');

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(2, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('pmd')).to.deep.equal({fileCount: 1, violationCount: 3}, 'PMD summary should be correct');
				expect(summaryMap.get('eslint')).to.deep.equal({fileCount: 2, violationCount: 3}, 'ESLint summary should be correct');
			});

			it ('Edge Cases', async () => {
				const results =  (await RuleResultRecombinator.recombineAndReformatResults(edgeCaseResults, {format: OUTPUT_FORMAT.XML, normalizeSeverity: false}, new Set(['eslint']))).results;
				const ruleResults: RuleResult[] = await convertXmlToJson(results as string, false);
				expect(ruleResults).to.have.lengthOf(1, 'Rule Results');

				// Validate each of the problem rows
				let rrIndex = 0;
				edgeCaseResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateJson(ruleResults[rrIndex], edgeCaseResults, rrIndex, vIndex, messageIsTrimmed, false);
						vIndex++;
					});
					rrIndex++
				});

				// Make sure we have iterated through all of the results.
				// It's one more than the number of RuleResults because of the post increment.
				expect(rrIndex).to.equal(1, 'RuleResult Index');
			});
		});

		describe('Output Format: Table', () => {
			// TODO: IMPLEMENT THESE TESTS.
		});

		describe('Output Format: CSV', () => {
			function validateCsvRow(violation, expectedResults: RuleResult[], expectedRuleResultIndex: number, expectedViolationIndex: number, expectedProblemNumber: number, normalizeSeverity: boolean): void {
				const expectedRuleResult = expectedResults[expectedRuleResultIndex];
				const expectedViolation = expectedRuleResult.violations[expectedViolationIndex];
				expect(violation[0]).to.equal(`${expectedProblemNumber}`, 'Problem number');
				expect(violation[1]).to.equal(expectedRuleResult.fileName, 'Filename');
				expect(violation[2]).to.equal(`${expectedViolation.severity}`, 'Severity');
				if (normalizeSeverity){
					expect(violation[3]).to.equal(`${expectedViolation.normalizedSeverity}`, 'Normalized Severity');
					expect(violation[4]).to.equal(`${expectedViolation.line}`, 'Line');
					expect(violation[5]).to.equal(`${expectedViolation.column}`, 'Column');
					expect(violation[6]).to.equal(expectedViolation.ruleName, 'Rule Name');
					// The message is trimmed before converting to CSV
					expect(violation[7]).to.equal(expectedViolation.message.trim(), 'Message');
					expect(violation[8]).to.equal(expectedViolation.url, 'Url');
					expect(violation[9]).to.equal(expectedViolation.category, 'Category');
					expect(violation[10]).to.equal(expectedRuleResult.engine, 'Engine');
				} else {
					expect(violation[3]).to.equal(`${expectedViolation.line}`, 'Line');
					expect(violation[4]).to.equal(`${expectedViolation.column}`, 'Column');
					expect(violation[5]).to.equal(expectedViolation.ruleName, 'Rule Name');
					// The message is trimmed before converting to CSV
					expect(violation[6]).to.equal(expectedViolation.message.trim(), 'Message');
					expect(violation[7]).to.equal(expectedViolation.url, 'Url');
					expect(violation[8]).to.equal(expectedViolation.category, 'Category');
					expect(violation[9]).to.equal(expectedRuleResult.engine, 'Engine');
				}
			}

			it ('Happy Path', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeRuleResults, {format: OUTPUT_FORMAT.CSV, normalizeSeverity: false}, new Set(['eslint', 'pmd']));
				const records = await new Promise((resolve, reject) => {
					csvParse(results as string, (err, output) => {
						if (err) {
							reject(err);
						}
						resolve(output);
					});
				});
				expect(records).to.have.lengthOf(7, 'Expected allFakeRuleResults violations plus the header');

				// Validate the header
				const header = records[0];
				// TODO: More validation
				expect(header[0]).to.equal('Problem');

				// Validate each of the problem rows
				let rrIndex = 0;
				let problemNumber = 1;
				allFakeRuleResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateCsvRow(records[problemNumber], allFakeRuleResults, rrIndex, vIndex, problemNumber, false);
						vIndex++;
						problemNumber++;
					});
					rrIndex++
				});

				// Make sure we have iterated through all of the results.
				// It's one more than the number of problems because of the post increment.
				expect(problemNumber).to.equal(7, 'Problem Number Index');

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(2, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('pmd')).to.deep.equal({fileCount: 1, violationCount: 3}, 'PMD summary should be correct');
				expect(summaryMap.get('eslint')).to.deep.equal({fileCount: 2, violationCount: 3}, 'ESLint summary should be correct');
			});

			it ('Happy Path - normalized', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeRuleResultsNormalized, {format: OUTPUT_FORMAT.CSV, normalizeSeverity: true}, new Set(['eslint', 'pmd']));
				const records = await new Promise((resolve, reject) => {
					csvParse(results as string, (err, output) => {
						if (err) {
							reject(err);
						}
						resolve(output);
					});
				});
				expect(records).to.have.lengthOf(7, 'Expected allFakeRuleResults violations plus the header');

				// Validate the header
				const header = records[0];
				// TODO: More validation
				expect(header[0]).to.equal('Problem');

				// Validate each of the problem rows
				let rrIndex = 0;
				let problemNumber = 1;
				allFakeRuleResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateCsvRow(records[problemNumber], allFakeRuleResultsNormalized, rrIndex, vIndex, problemNumber, true);
						vIndex++;
						problemNumber++;
					});
					rrIndex++
				});

				// Make sure we have iterated through all of the results.
				// It's one more than the number of problems because of the post increment.
				expect(problemNumber).to.equal(7, 'Problem Number Index');

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(2, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('pmd')).to.deep.equal({fileCount: 1, violationCount: 3}, 'PMD summary should be correct');
				expect(summaryMap.get('eslint')).to.deep.equal({fileCount: 2, violationCount: 3}, 'ESLint summary should be correct');
			});

			it ('Edge Cases', async () => {
				const results = (await RuleResultRecombinator.recombineAndReformatResults(edgeCaseResults, {format: OUTPUT_FORMAT.CSV, normalizeSeverity: false}, new Set(['eslint']))).results;
				const records = await new Promise((resolve, reject) => {
					csvParse(results as string, (err, output) => {
						if (err) {
							reject(err);
						}
						resolve(output);
					});
				});
				expect(records).to.have.lengthOf(6, 'Expected edgeCaseResults violations plus the header');

				// Validate the header
				const header = records[0];
				expect(header[0]).to.equal('Problem');

				// Validate each of the problem rows
				let rrIndex = 0;
				let problemNumber = 1;
				edgeCaseResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateCsvRow(records[problemNumber], edgeCaseResults, rrIndex, vIndex, problemNumber, false);
						vIndex++;
						problemNumber++;
					});
					rrIndex++
				});

				// Make sure we have iterated through all of the results.
				// It's one more than the number of problems because of the post increment.
				expect(problemNumber).to.equal(6, 'Problem Number Index');
			});
		});
	});
});
