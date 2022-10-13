import {expect} from 'chai';
import {ESLint} from 'eslint';
import {isPathlessViolation} from '../../../src/lib/util/Utils';
import {RuleResult, RuleViolation} from '../../../src/types';
import {RuleResultRecombinator} from '../../../src/lib/formatter/RuleResultRecombinator';
import {OUTPUT_FORMAT} from '../../../src/lib/RuleManager';
import path = require('path');
import * as csvParse from 'csv-parse';
import {parseString} from 'xml2js';
import * as TestOverrides from '../../test-related-lib/TestOverrides';
import { PathlessEngineFilters, ENGINE, PMD_VERSION, SFGE_VERSION } from '../../../src/Constants';
import { fail } from 'assert';

const sampleFile1 = path.join('Users', 'SomeUser', 'samples', 'sample-file1.js');
const sampleFile2 = path.join('Users', 'SomeUser', 'samples', 'sample-file2.js');
const sampleFile3 = path.join('Users', 'SomeUser', 'samples', 'sample-file3.java');
const sampleFile4 = path.join('Users', 'SomeUser', 'samples', 'file-with-&.js');
const sampleFile5 = path.join('Users', 'SomeUser', 'samples', 'sample-apex-file.cls');

const ESLINT_VERSION = ESLint.version;

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

const allFakePathlessRuleResults: RuleResult[] = [
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

const allFakePathlessRuleResultsNormalized: RuleResult[] = [
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

const allFakeDfaRuleResults: RuleResult[] = [
	{
		engine: 'sfge',
		fileName: sampleFile5,
		violations: [{
			"sourceLine": 15,
			"sourceColumn": 8,
			"sourceType": "BearController",
			"sourceMethodName": "getAllBears",
			"sinkFileName": '/Users/path/to/offendingFile.cls',
			"sinkLine": 3,
			"sinkColumn": 8,
			"ruleName": "ApexFlsViolationRule",
			"severity": 1,
			"message": "FLS validation is missing for [READ] operation on [Bear__c] with field(s) [Age__c,Height__c]",
			"url": "https://thisurldoesnotmatter.com",
			"category": "Security"
		}]
	}
];

const fakeDfaWithError: RuleResult[] = [
	{
		engine: 'sfge',
		fileName: sampleFile5,
		violations: [{
			"sourceLine": 15,
			"sourceColumn": 8,
			"sourceType": "BearController",
			"sourceMethodName": "getAllBears",
			"sinkFileName": "",
			"sinkLine": null,
			"sinkColumn": null,
			"ruleName": "InternalExecutionError",
			"severity": 3,
			"message": "Path evaluation timed out after 40 ms",
			"url": "https://thisurldoesnotmatter.com",
			"category": "InternalExecutionError"
		}]
	}
]


const allFakeDfaRuleResultsNormalized: RuleResult[] = [
	{
		engine: 'sfge',
		fileName: sampleFile5,
		violations: [{
			"sourceLine": 15,
			"sourceColumn": 8,
			"sourceType": "BearController",
			"sourceMethodName": "getAllBears",
			"sinkFileName": '/Users/path/to/offendingFile.cls',
			"sinkLine": 3,
			"sinkColumn": 8,
			"ruleName": "ApexFlsViolationRule",
			"normalizedSeverity": 1,
			"severity": 1,
			"message": "FLS validation is missing for [READ] operation on [Bear__c] with field(s) [Age__c,Height__c]",
			"url": "https://thisurldoesnotmatter.com",
			"category": "Security"
		}]
	}
];

const retireJsVerboseViolations: RuleResult[] = [
	{
		engine: 'retire-js',
		fileName: sampleFile1,
		violations: [{
			"line": 1,
			"column": 1,
			"severity": 2,
			"message": "jquery 3.1.0 has known vulnerabilities:\nseverity: medium; summary: jQuery before 3.4.0, as used in Drupal, Backdrop CMS, and other products, mishandles jQuery.extend(true, {}, ...) because of Object.prototype pollution; CVE: CVE-2019-11358; https://blog.jquery.com/2019/04/10/jquery-3-4-0-released/ https://nvd.nist.gov/vuln/detail/CVE-2019-11358 https://github.com/jquery/jquery/commit/753d591aea698e57d6db58c9f722cd0808619b1b\nseverity: medium; summary: Regex in its jQuery.htmlPrefilter sometimes may introduce XSS; CVE: CVE-2020-11022; https://blog.jquery.com/2020/04/10/jquery-3-5-0-released/\nseverity: medium; summary: Regex in its jQuery.htmlPrefilter sometimes may introduce XSS; CVE: CVE-2020-11023; https://blog.jquery.com/2020/04/10/jquery-3-5-0-released/",
			"ruleName": "insecure-bundled-dependencies",
			"category": "Insecure Dependencies",
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
	expect(violation.ruleName).to.equal(expectedViolation.ruleName, 'Rule Name');
	const expectedMessage = trimMessage ? expectedViolation.message.trim() : expectedViolation.message;
	expect(violation.message).to.equal(expectedMessage);
	expect(violation.url).to.equal(expectedViolation.url, 'Url');
	expect(violation.category).to.equal(expectedViolation.category, 'Category');
	const isActuallyPathless = isPathlessViolation(violation);
	const isExpectedPathless = isPathlessViolation(expectedViolation);
	expect(isActuallyPathless).to.equal(isExpectedPathless, 'Wrong violation type');
	if (isActuallyPathless && isExpectedPathless) {
		expect(violation.line).to.equal(expectedViolation.line, 'Line');
		expect(violation.column).to.equal(expectedViolation.column, 'Column');
	} else if (!isActuallyPathless && !isExpectedPathless) {
		expect(violation.sourceLine).to.equal(expectedViolation.sourceLine, 'Line');
		expect(violation.sourceColumn).to.equal(expectedViolation.sourceColumn, 'Column');
	}
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
				const someFakeResults = [allFakePathlessRuleResults[0]];

				// Create our reformatted results.
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(someFakeResults, OUTPUT_FORMAT.JUNIT, new Set(['eslint', 'pmd']));
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
				const someFakeResults = [allFakePathlessRuleResults[1]];

				// Create our reformatted results.
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(someFakeResults, OUTPUT_FORMAT.JUNIT, new Set(['eslint', 'pmd']));
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
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakePathlessRuleResults, OUTPUT_FORMAT.JUNIT, new Set(['eslint', 'pmd']));
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
				expect(driver.version).to.equal(ESLINT_VERSION);
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

			function validateSfgeSarif(run: unknown, normalizeSeverity: boolean, expectingErrors: boolean): void {
				const driver = run['tool']['driver'];
				expect(driver.name).to.equal(ENGINE.SFGE);
				expect(driver.version).to.equal(SFGE_VERSION);
				expect(driver.informationUri).to.equal('https://forcedotcom.github.io/sfdx-scanner/en/v3.x/salesforce-graph-engine/introduction/');

				// tool.driver.rules
				expect(driver['rules']).to.have.lengthOf(1, 'Incorrect rule count');
				if (expectingErrors) {
					expect(driver['rules'][0].id).to.equal('InternalExecutionError');
				} else {
					expect(driver['rules'][0].id).to.equal('ApexFlsViolationRule');
				}
				if (normalizeSeverity) {
					expect(driver['rules'][0].properties.normalizedSeverity).to.equal(1);
				} else {
					expect(driver['rules'][0].properties.normalizedSeverity).to.equal(undefined);
				}

				// Deep validation of results and rules is skipped. The only thing distinguishing SFCA violations from
				// other engines is that path-based rules should also have a `relatedLocations` node.
				expect(run['results']).to.have.lengthOf(1, 'Incorrect result count');
				const results = run['results'];
				expect(results[0].ruleIndex).to.equal(0);
				if (expectingErrors) {
					expect(results[0].relatedLocations).to.not.exist;
				} else {
					expect(results[0].relatedLocations).to.exist;
				}

				// Invocations
				expect(run['invocations']).to.have.lengthOf(1, 'Invocations');
				const invocation = run['invocations'][0];
				expect(invocation.executionSuccessful).to.be.true;
				expect(invocation.toolExecutionNotifications).to.have.lengthOf(0, 'Tool execution');
			}

			it ('Happy Path - pathless rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakePathlessRuleResults, OUTPUT_FORMAT.SARIF, new Set(['eslint', 'pmd']));
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

			it ('Happy Path - normalized pathless rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakePathlessRuleResultsNormalized, OUTPUT_FORMAT.SARIF, new Set(['eslint', 'pmd']));
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

			it ('Happy Path - DFA rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeDfaRuleResults, OUTPUT_FORMAT.SARIF, new Set(['sfge']));
				const sarifResults: unknown[] = JSON.parse(results as string);
				expect(sarifResults['runs']).to.have.lengthOf(1, 'Runs');
				const runs = sarifResults['runs'];

				for (let i=0; i<runs.length; i++) {
					const run = runs[i];
					const engine = run['tool']['driver']['name'];
					if (engine === ENGINE.SFGE) {
						validateSfgeSarif(run, false, false);
					} else {
						fail(`Unexpected engine: ${engine}`);
					}
				}

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(1, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('sfge')).to.deep.equal({fileCount: 1, violationCount: 1}, 'Sfge summary should be correct');
			});

			it ('Happy Path - Normalized DFA rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeDfaRuleResultsNormalized, OUTPUT_FORMAT.SARIF, new Set(['sfge']));
				const sarifResults: unknown[] = JSON.parse(results as string);
				expect(sarifResults['runs']).to.have.lengthOf(1, 'Runs');
				const runs = sarifResults['runs'];

				for (let i=0; i<runs.length; i++) {
					const run = runs[i];
					const engine = run['tool']['driver']['name'];
					if (engine === ENGINE.SFGE) {
						validateSfgeSarif(run, true, false);
					} else {
						fail(`Unexpected engine: ${engine}`);
					}
				}

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(1, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('sfge')).to.deep.equal({fileCount: 1, violationCount: 1}, 'Sfge summary should be correct');
			});

			it ('DFA with timeout error', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(fakeDfaWithError, OUTPUT_FORMAT.SARIF, new Set(['sfge']));
				const sarifResults: unknown[] = JSON.parse(results as string);
				expect(sarifResults['runs']).to.have.lengthOf(1, 'Runs');
				const runs = sarifResults['runs'];

				for (let i = 0; i < runs.length; i++) {
					const run = runs[i];
					const engine = run['tool']['driver']['name'];
					if (engine === ENGINE.SFGE) {
						validateSfgeSarif(run, false, true);
					} else {
						fail(`Unexpected engine: ${engine}`);
					}
				}

				expect(minSev).to.equal(3, 'Most severe problem');
				expect(summaryMap.size).to.equal(1, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('sfge')).to.deep.equal({fileCount: 1, violationCount: 1}, 'Sfge summary should be correct');
			});

			it('Handles all pathless engines', async () => {
				const allEngines = PathlessEngineFilters.map(engine => engine.valueOf());
				for (const engine of allEngines) {
					const ruleResults: RuleResult[] = [{
						engine: engine,
						fileName: sampleFile1,
						violations: [{
							"line": 2,
							"column": 11,
							"severity": 2,
							"message": "A generic message",
							"ruleName": "rule-name",
							"category": "category-name",
							"url": "https://some/url.org"
						}]
					}];
					await RuleResultRecombinator.recombineAndReformatResults(ruleResults, OUTPUT_FORMAT.SARIF, new Set([engine]));
					// should throw an error if the engine was not handled
				}
			});

			it ('Run with no violations returns engines that were run', async () => {
				const results = await (await RuleResultRecombinator.recombineAndReformatResults([], OUTPUT_FORMAT.SARIF, new Set(['eslint', 'pmd']))).results;
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
			it ('Happy Path - pathless rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakePathlessRuleResults, OUTPUT_FORMAT.JSON, new Set(['eslint', 'pmd']));
				const ruleResults: RuleResult[] = JSON.parse(results as string);
				expect(ruleResults).to.have.lengthOf(3, 'Rule Results');

				// Validate each of the problem rows
				let rrIndex = 0;
				allFakePathlessRuleResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateJson(ruleResults[rrIndex], allFakePathlessRuleResults, rrIndex, vIndex, messageIsTrimmed, false);
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

			it ('Happy Path - normalized pathless rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakePathlessRuleResultsNormalized, OUTPUT_FORMAT.JSON, new Set(['eslint', 'pmd']));
				const ruleResults: RuleResult[] = JSON.parse(results as string);
				expect(ruleResults).to.have.lengthOf(3, 'Rule Results');

				// Validate each of the problem rows
				let rrIndex = 0;
				allFakePathlessRuleResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateJson(ruleResults[rrIndex], allFakePathlessRuleResultsNormalized, rrIndex, vIndex, messageIsTrimmed, true);
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

			it ('Happy path - DFA rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeDfaRuleResults, OUTPUT_FORMAT.JSON, new Set(['sfge']));
				const ruleResults: RuleResult[] = JSON.parse(results as string);
				expect(ruleResults).to.have.lengthOf(1, 'Rule Results');

				// Validate each of the problem rows
				let rrIndex = 0;
				allFakeDfaRuleResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateJson(ruleResults[rrIndex], allFakeDfaRuleResults, rrIndex, vIndex, messageIsTrimmed, false);
						vIndex++;
					});
					rrIndex++
				});

				// Make sure we have iterated through all of the results.
				expect(rrIndex).to.equal(1, 'Rule Result Index');

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(1, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('sfge')).to.deep.equal({fileCount: 1, violationCount: 1}, 'SFGE summary should be correct');
			});

			it ('Happy path - normalized DFA rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeDfaRuleResultsNormalized, OUTPUT_FORMAT.JSON, new Set(['sfge']));
				const ruleResults: RuleResult[] = JSON.parse(results as string);
				expect(ruleResults).to.have.lengthOf(1, 'Rule Results');

				// Validate each of the problem rows
				let rrIndex = 0;
				allFakeDfaRuleResultsNormalized.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateJson(ruleResults[rrIndex], allFakeDfaRuleResultsNormalized, rrIndex, vIndex, messageIsTrimmed, true);
						vIndex++;
					});
					rrIndex++
				});

				// Make sure we have iterated through all of the results.
				expect(rrIndex).to.equal(1, 'Rule Result Index');

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(1, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('sfge')).to.deep.equal({fileCount: 1, violationCount: 1}, 'SFGE summary should be correct');
			});

			it ('Using --verbose-violations', async () => {
				const results = (await RuleResultRecombinator.recombineAndReformatResults(retireJsVerboseViolations, OUTPUT_FORMAT.JSON, new Set(['retire-js']), true)).results;
				const ruleResults: RuleResult[] = JSON.parse(results as string);
				expect(ruleResults[0].violations[0].message).to.equal("jquery 3.1.0 has known vulnerabilities: severity: medium; summary: jQuery before 3.4.0, as used in Drupal, Backdrop CMS, and other products, mishandles jQuery.extend(true, {}, ...) because of Object.prototype pollution; CVE: CVE-2019-11358; https://blog.jquery.com/2019/04/10/jquery-3-4-0-released/ https://nvd.nist.gov/vuln/detail/CVE-2019-11358 https://github.com/jquery/jquery/commit/753d591aea698e57d6db58c9f722cd0808619b1b; severity: medium; summary: Regex in its jQuery.htmlPrefilter sometimes may introduce XSS; CVE: CVE-2020-11022; https://blog.jquery.com/2020/04/10/jquery-3-5-0-released/; severity: medium; summary: Regex in its jQuery.htmlPrefilter sometimes may introduce XSS; CVE: CVE-2020-11023; https://blog.jquery.com/2020/04/10/jquery-3-5-0-released/");
			});

			it ('Edge Cases', async () => {
				const results = await (await RuleResultRecombinator.recombineAndReformatResults(edgeCaseResults, OUTPUT_FORMAT.JSON, new Set(['eslint']))).results;
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
						if (v.$.line != null) {
							violations.push({
								severity: parseInt(v['$']['severity']),
								normalizedSeverity: (normalizeSeverity ? parseInt(v['$']['normalizedSeverity']) : undefined),
								line: parseInt(v['$']['line']),
								column: parseInt(v['$']['column']),
								ruleName: v['$']['rule'],
								message: v['_'],
								url: v['$']['url'],
								category: v['$']['category']
							});
						} else {
							violations.push({
								severity: parseInt(v['$']['severity']),
								normalizedSeverity: (normalizeSeverity ? parseInt(v['$']['normalizedSeverity']) : undefined),
								sourceLine: parseInt(v['$']['sourceLine']),
								sourceColumn: parseInt(v['$']['sourceColumn']),
								ruleName: v['$']['rule'],
								message: v['_'],
								url: v['$']['url'],
								category: v['$']['category']
							})
						}
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

			it ('Happy Path - pathless rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakePathlessRuleResults, OUTPUT_FORMAT.XML, new Set(['eslint', 'pmd']));
				const ruleResults: RuleResult[] = await convertXmlToJson(results as string, false);
				expect(ruleResults).to.have.lengthOf(3, 'Rule Results');

				// Validate each of the problem rows
				let rrIndex = 0;
				allFakePathlessRuleResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateJson(ruleResults[rrIndex], allFakePathlessRuleResults, rrIndex, vIndex, messageIsTrimmed, false);
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

			it ('Happy Path - normalized pathless rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakePathlessRuleResultsNormalized, OUTPUT_FORMAT.XML, new Set(['eslint', 'pmd']));
				const ruleResults: RuleResult[] = await convertXmlToJson(results as string, true);
				expect(ruleResults).to.have.lengthOf(3, 'Rule Results');

				// Validate each of the problem rows
				let rrIndex = 0;
				allFakePathlessRuleResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateJson(ruleResults[rrIndex], allFakePathlessRuleResultsNormalized, rrIndex, vIndex, messageIsTrimmed, true);
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

			it('Happy path - DFA rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeDfaRuleResults, OUTPUT_FORMAT.XML, new Set(['sfge']));
				const ruleResults: RuleResult[] = await convertXmlToJson(results as string, false);
				expect(ruleResults).to.have.lengthOf(1, 'Rule Results');

				// Validate each of the problem rows
				let rrIndex = 0;
				allFakeDfaRuleResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateJson(ruleResults[rrIndex], allFakeDfaRuleResults, rrIndex, vIndex, messageIsTrimmed, false);
						vIndex++;
					});
					rrIndex++
				});

				// Make sure we have iterated through all of the results.
				expect(rrIndex).to.equal(1, 'Rule Result Index');

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(1, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('sfge')).to.deep.equal({fileCount: 1, violationCount: 1}, 'SFGE summary should be correct');
			});

			it('Happy path - normalized DFA rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeDfaRuleResultsNormalized, OUTPUT_FORMAT.XML, new Set(['sfge']));
				const ruleResults: RuleResult[] = await convertXmlToJson(results as string, true);
				expect(ruleResults).to.have.lengthOf(1, 'Rule Results');

				// Validate each of the problem rows
				let rrIndex = 0;
				allFakeDfaRuleResultsNormalized.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateJson(ruleResults[rrIndex], allFakeDfaRuleResultsNormalized, rrIndex, vIndex, messageIsTrimmed, true);
						vIndex++;
					});
					rrIndex++
				});

				// Make sure we have iterated through all of the results.
				expect(rrIndex).to.equal(1, 'Rule Result Index');

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(1, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('sfge')).to.deep.equal({fileCount: 1, violationCount: 1}, 'SFGE summary should be correct');
			});

			it ('Edge Cases', async () => {
				const results =  (await RuleResultRecombinator.recombineAndReformatResults(edgeCaseResults, OUTPUT_FORMAT.XML, new Set(['eslint']))).results;
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
				const expectationArray: {expectation: any; message: string}[] = [
					{expectation: `${expectedProblemNumber}`, message: 'Problem number'},
					{expectation: `${expectedViolation.severity}`, message: 'Severity'}
				];
				if (normalizeSeverity) {
					expectationArray.push({expectation: `${expectedViolation.normalizedSeverity}`, message: 'Normalized severity'});
				}
				const expectedPathless = isPathlessViolation(expectedViolation);
				if (expectedPathless) {
					expectationArray.push({expectation: expectedRuleResult.fileName, message: 'File'});
					expectationArray.push({expectation: `${expectedViolation.line}`, message: 'Line'});
					expectationArray.push({expectation: `${expectedViolation.column}`, message: 'Column'});
				} else {
					expectationArray.push({expectation: expectedRuleResult.fileName, message: 'Source File'});
					expectationArray.push({expectation: `${expectedViolation.sourceLine}`, message: 'Source Line'});
					expectationArray.push({expectation: `${expectedViolation.sourceColumn}`, message: 'Source Column'});
					expectationArray.push({expectation: expectedViolation.sourceType, message: 'Source Type'});
					expectationArray.push({expectation: expectedViolation.sourceMethodName, message: 'Source Method'});
					expectationArray.push({expectation: expectedViolation.sinkFileName, message: 'Sink File'});
					expectationArray.push({expectation: `${expectedViolation.sinkLine}`, message: 'Sink Line'});
					expectationArray.push({expectation: `${expectedViolation.sinkColumn}`, message: 'Sink Column'});
				}
				expectationArray.push({expectation: expectedViolation.ruleName, message: 'Rule'});
				// The message is trimmed before converting to CSV.
				expectationArray.push({expectation: expectedViolation.message.trim(), message: 'Description'});
				expectationArray.push({expectation: expectedViolation.url || '', message: 'URL'});
				expectationArray.push({expectation: expectedViolation.category, message: 'Category'});
				expectationArray.push({expectation: expectedRuleResult.engine, message: 'Engine'});
				for (let i = 0; i < expectationArray.length; i++) {
					expect(violation[i]).to.equal(expectationArray[i].expectation, expectationArray[i].message);
				}
			}

			it ('Happy Path - pathless rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakePathlessRuleResults, OUTPUT_FORMAT.CSV, new Set(['eslint', 'pmd']));
				const records = await new Promise((resolve, reject) => {
					csvParse(results as string, (err, output) => {
						if (err) {
							reject(err);
						}
						resolve(output);
					});
				});
				expect(records).to.have.lengthOf(7, 'Expected allFakePathlessRuleResults violations plus the header');

				// Validate the header
				const header = records[0];
				// TODO: More validation
				expect(header[0]).to.equal('Problem');

				// Validate each of the problem rows
				let rrIndex = 0;
				let problemNumber = 1;
				allFakePathlessRuleResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateCsvRow(records[problemNumber], allFakePathlessRuleResults, rrIndex, vIndex, problemNumber, false);
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

			it ('Happy Path - normalized pathless rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakePathlessRuleResultsNormalized, OUTPUT_FORMAT.CSV, new Set(['eslint', 'pmd']));
				const records = await new Promise((resolve, reject) => {
					csvParse(results as string, (err, output) => {
						if (err) {
							reject(err);
						}
						resolve(output);
					});
				});
				expect(records).to.have.lengthOf(7, 'Expected allFakePathlessRuleResults violations plus the header');

				// Validate the header
				const header = records[0];
				// TODO: More validation
				expect(header[0]).to.equal('Problem');

				// Validate each of the problem rows
				let rrIndex = 0;
				let problemNumber = 1;
				allFakePathlessRuleResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateCsvRow(records[problemNumber], allFakePathlessRuleResultsNormalized, rrIndex, vIndex, problemNumber, true);
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

			it ('Happy path - DFA rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeDfaRuleResults, OUTPUT_FORMAT.CSV, new Set(['sfge']));
				const records = await new Promise((resolve, reject) => {
					csvParse(results as string, (err, output) => {
						if (err) {
							reject(err);
						}
						resolve(output);
					});
				});
				expect(records).to.have.lengthOf(2, 'Expected allFakeDfaRuleResults violations plus the header');

				// Validate the header
				const header = records[0];
				// TODO: More validation
				expect(header[0]).to.equal('Problem');

				// Validate each of the problem rows
				let rrIndex = 0;
				let problemNumber = 1;
				allFakeDfaRuleResults.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateCsvRow(records[problemNumber], allFakeDfaRuleResults, rrIndex, vIndex, problemNumber, false);
						vIndex++;
						problemNumber++;
					});
					rrIndex++
				});

				// Make sure we have iterated through all of the results.
				expect(problemNumber).to.equal(2, 'Problem Number Index');

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(1, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('sfge')).to.deep.equal({fileCount: 1, violationCount: 1}, 'SFGE summary should be correct');
			});

			it('Happy path - Normalized DFA rules', async () => {
				const {minSev, results, summaryMap} = await RuleResultRecombinator.recombineAndReformatResults(allFakeDfaRuleResultsNormalized, OUTPUT_FORMAT.CSV, new Set(['sfge']));
				const records = await new Promise((resolve, reject) => {
					csvParse(results as string, (err, output) => {
						if (err) {
							reject(err);
						}
						resolve(output);
					});
				});
				expect(records).to.have.lengthOf(2, 'Expected allFakeDfaRuleResultsNormalized violations plus the header');

				// Validate the header
				const header = records[0];
				// TODO: More validation
				expect(header[0]).to.equal('Problem');

				// Validate each of the problem rows
				let rrIndex = 0;
				let problemNumber = 1;
				allFakeDfaRuleResultsNormalized.forEach(rr => {
					let vIndex = 0;
					rr.violations.forEach(() => {
						validateCsvRow(records[problemNumber], allFakeDfaRuleResultsNormalized, rrIndex, vIndex, problemNumber, true);
						vIndex++;
						problemNumber++;
					});
					rrIndex++
				});

				// Make sure we have iterated through all of the results.
				expect(problemNumber).to.equal(2, 'Problem Number Index');

				expect(minSev).to.equal(1, 'Most severe problem');
				expect(summaryMap.size).to.equal(1, 'Each supposedly executed engine needs a summary');
				expect(summaryMap.get('sfge')).to.deep.equal({fileCount: 1, violationCount: 1}, 'SFGE summary should be correct');
			});

			it ('Edge Cases', async () => {
				const results = (await RuleResultRecombinator.recombineAndReformatResults(edgeCaseResults, OUTPUT_FORMAT.CSV, new Set(['eslint']))).results;
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

		describe('Output Format: HTML', () => {
			it ('Using --verbose-violations', async () => {
				const results = (await RuleResultRecombinator.recombineAndReformatResults(retireJsVerboseViolations, OUTPUT_FORMAT.HTML, new Set(['retire-js']), true)).results as string;
				const violationString = results.split("const violations = [")[1].split("];\n")[0];
				console.log(`===START=====\n results is ${results}\n=BREAK=\nviolations is ${violationString}\n===END====\n`);
				const violation: RuleViolation = JSON.parse(violationString as string);
				expect(violation.message).to.equal("jquery 3.1.0 has known vulnerabilities:<br>severity: medium; summary: jQuery before 3.4.0, as used in Drupal, Backdrop CMS, and other products, mishandles jQuery.extend(true, {}, ...) because of Object.prototype pollution; CVE: CVE-2019-11358; https://blog.jquery.com/2019/04/10/jquery-3-4-0-released/ https://nvd.nist.gov/vuln/detail/CVE-2019-11358 https://github.com/jquery/jquery/commit/753d591aea698e57d6db58c9f722cd0808619b1b<br>severity: medium; summary: Regex in its jQuery.htmlPrefilter sometimes may introduce XSS; CVE: CVE-2020-11022; https://blog.jquery.com/2020/04/10/jquery-3-5-0-released/<br>severity: medium; summary: Regex in its jQuery.htmlPrefilter sometimes may introduce XSS; CVE: CVE-2020-11023; https://blog.jquery.com/2020/04/10/jquery-3-5-0-released/");
			});
		});
	});
});
