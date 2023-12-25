import {expect} from 'chai';

import {RunOutputOptions, RunOutputProcessor} from '../../../src/lib/util/RunOutputProcessor';
import {OUTPUT_FORMAT} from '../../../src/lib/RuleManager';
import {EngineExecutionSummary, RecombinedRuleResults} from '../../../src/types';
import {AnyJson} from '@salesforce/ts-types';
import Sinon = require('sinon');
import fs = require('fs');
import {BundleName, getMessage} from "../../../src/MessageCatalog";
import {FakeDisplay} from "../FakeDisplay";
import {PATHLESS_COLUMNS} from "../../../lib/lib/output/TableOutputFormatter";

const FAKE_SUMMARY_MAP: Map<string, EngineExecutionSummary> = new Map();
FAKE_SUMMARY_MAP.set('pmd', {fileCount: 1, violationCount: 1});
FAKE_SUMMARY_MAP.set('eslint-typescript', {fileCount: 1, violationCount: 2});

const FAKE_TABLE_OUTPUT = {
	"columns": PATHLESS_COLUMNS,
	"rows": [
		{
			location: "src/file-with-problems.ts:3",
			description: "  'UNUSED' is assigned a value but never used.",
			url: "https://eslint.org/docs/rules/no-unused-vars",
			category: "Variables",
		},
		{
			location: "src/file-with-problems.ts:3",
			description: "  'UNUSED' is assigned a value but never used.",
			url: "https://github.com/typescript-eslint/typescript-eslint/blob/v2.33.0/packages/eslint-plugin/docs/rules/no-unused-vars.md",
			category: "Variables",
		},
		{
			location: "src/some-apex-file.cls:15",
			description: "  Avoid empty 'if' statements",
			url: "Who cares",
			category: "Error Prone",
		}
	]
};

const FAKE_CSV_OUTPUT = `"Problem","File","Severity","Line","Column","Rule","Description","URL","Category","Engine"
"1","/Users/jfeingold/ts-sample-project/src/file-with-problems.ts","2","3","7","no-unused-vars","'UNUSED' is assigned a value but never used.","https://eslint.org/docs/rules/no-unused-vars","Variables","eslint-typescript"
"2","/Users/jfeingold/ts-sample-project/src/file-with-problems.ts","2","3","7","@typescript-eslint/no-unused-vars","'UNUSED' is assigned a value but never used.","https://github.com/typescript-eslint/typescript-eslint/blob/v2.33.0/packages/eslint-plugin/docs/rules/no-unused-vars.md","Variables","eslint-typescript"
"3","/Users/jfeingold/ts-sample-project/src/some-apex-file.cls","1","15","3","EmptyIfStmt","Avoid empty 'if' statements","Who cares","Error Prone","pmd"`;

const FAKE_JSON_OUTPUT = `[{
	"engine": "eslint-typescript",
	"fileName": "/Users/jfeingold/ts-sample-project/src/file-with-problems.ts",
	"violations": [{
		"line": 3,
		"column": 7,
		"severity": 2,
		"message": "'UNUSED' is assigned a value but never used.",
		"ruleName": "no-unused-vars",
		"category": "Variables",
		"url": "https://eslint.org/docs/rules/no-unused-vars"
	}, {
		"line": 3,
		"column": 7,
		"severity": 2,
		"message": "'UNUSED' is assigned a value but never used.",
		"ruleName": "@typescript-eslint/no-unused-vars",
		"category": "Variables",
		"url": "https://github.com/typescript-eslint/typescript-eslint/blob/v2.33.0/packages/eslint-plugin/docs/rules/no-unused-vars.md"
	}]
}, {
	"engine": "pmd",
	"fileName": "/Users/jfeingold/ts-sample-project/src/some-apex-file.cls",
	"violations": [{
		"line": 15,
		"column": 3,
		"severity": 1,
		"message": "Avoid empty 'if' statements",
		"ruleName": "EmptyIfStmt",
		"category": "Error Prone",
		"url": "Who cares"
	}]
}]`;


describe('RunOutputProcessor', () => {
	let fakeFiles: {path; data}[] = [];
	let display: FakeDisplay;

	beforeEach(() => {
		display = new FakeDisplay();

		Sinon.createSandbox();

		// Stub out fs.writeFileSync() so we can check what it's trying to do without letting it actually write anything.
		fakeFiles = [];
		Sinon.stub(fs, 'writeFileSync').callsFake((path, data) => {
			fakeFiles.push({path, data});
		});
	});

	afterEach(() => {
		Sinon.restore();
	});

	describe('#processRunOutput()', () => {
		describe('Writing to console', () => {
			it('Empty results yield expected message', async () => {
				const opts: RunOutputOptions = {
					format: OUTPUT_FORMAT.TABLE
				};
				const rop = new RunOutputProcessor(display, opts);
				const summaryMap: Map<string, EngineExecutionSummary> = new Map();
				summaryMap.set('pmd', {fileCount: 0, violationCount: 0});
				summaryMap.set('eslint', {fileCount: 0, violationCount: 0});
				const fakeRes: RecombinedRuleResults = {minSev: 0, summaryMap, results: ''};

				// THIS IS THE PART BEING TESTED.
				const output: AnyJson = rop.processRunOutput(fakeRes);

				// We expect that the message logged to the console and the message returned should both be the default
				const expectedMsg = getMessage(BundleName.RunOutputProcessor, 'output.noViolationsDetected', ['pmd, eslint']);
				expect(display.getOutputText()).to.equal("[Info]: " + expectedMsg, 'Should have displayed expected message');
				expect(output).to.equal(expectedMsg, 'Should have returned expected message');
			});

			describe('Test Case: Table', () => {
				const fakeTableResults: RecombinedRuleResults = {minSev: 1, results: FAKE_TABLE_OUTPUT, summaryMap: FAKE_SUMMARY_MAP};

				it('Table-type output should be followed by summary', async () => {
					const opts: RunOutputOptions = {
						format: OUTPUT_FORMAT.TABLE
					};
					const rop = new RunOutputProcessor(display, opts);

					// THIS IS THE PART BEING TESTED.
					const output: AnyJson = rop.processRunOutput(fakeTableResults);

					const expectedTableSummary = `${getMessage(BundleName.RunOutputProcessor, 'output.engineSummaryTemplate', ['pmd', 1, 1])}
${getMessage(BundleName.RunOutputProcessor, 'output.engineSummaryTemplate', ['eslint-typescript', 2, 1])}
${getMessage(BundleName.RunOutputProcessor, 'output.writtenToConsole')}`;

					expect(display.getOutputArray()).length(2);
					expect(display.getOutputArray()[0]).to.satisfy(msg => msg.startsWith("[Table]"));
					expect(display.getLastTableColumns()).to.equal(FAKE_TABLE_OUTPUT.columns);
					expect(display.getLastTableData()).to.equal(FAKE_TABLE_OUTPUT.rows);
					expect(display.getOutputArray()[1]).to.equal("[Info]: " + expectedTableSummary);
					expect(output).to.deep.equal(FAKE_TABLE_OUTPUT.rows, 'Should have returned the rows');
				});

				it('Throws severity-based exception on request', async () => {
					const opts: RunOutputOptions = {
						format: OUTPUT_FORMAT.TABLE,
						severityForError: 1
					};
					const rop = new RunOutputProcessor(display, opts);

					// THIS IS THE PART BEING TESTED.
					try {
						const output: AnyJson = rop.processRunOutput(fakeTableResults);
						expect(true).to.equal(false, `Unexpectedly returned ${output} instead of throwing error`);
					} catch (e) {
						expect(display.getOutputText()).to.satisfy(msg => msg.startsWith("[Table]"));
						expect(display.getLastTableColumns()).to.equal(FAKE_TABLE_OUTPUT.columns);
						expect(display.getLastTableData()).to.equal(FAKE_TABLE_OUTPUT.rows);
						const expectedTableSummary = `${getMessage(BundleName.RunOutputProcessor, 'output.engineSummaryTemplate', ['pmd', 1, 1])}
${getMessage(BundleName.RunOutputProcessor, 'output.engineSummaryTemplate', ['eslint-typescript', 2, 1])}
${getMessage(BundleName.RunOutputProcessor, 'output.sevThresholdSummary', [1])}
${getMessage(BundleName.RunOutputProcessor, 'output.writtenToConsole')}`;
						expect(e.message).to.equal(expectedTableSummary, 'Exception message incorrectly formed');
					}
				});
			});

			describe('Test Case: CSV', () => {
				const fakeCsvResults: RecombinedRuleResults = {minSev: 1, summaryMap: FAKE_SUMMARY_MAP, results: FAKE_CSV_OUTPUT};

				// NOTE: This next test is based on the implementation of run-summary messages in W-8388246, which was known
				// to be incomplete. When we flesh out that implementation with summaries for other formats, this test might
				// need to change.
				it('CSV-type output should NOT be followed by summary', async () => {
					const opts: RunOutputOptions = {
						format: OUTPUT_FORMAT.CSV
					};

					const rop = new RunOutputProcessor(display, opts);

					// THIS IS THE PART BEING TESTED.
					const output: AnyJson = rop.processRunOutput(fakeCsvResults);
					expect(display.getOutputText()).to.equal("[Info]: " + FAKE_CSV_OUTPUT);
					expect(output).to.equal(FAKE_CSV_OUTPUT, 'CSV should be returned as a string');
				});

				it('Throws severity-based exception on request', async () => {
					const opts: RunOutputOptions = {
						format: OUTPUT_FORMAT.CSV,
						severityForError: 2
					};

					const rop = new RunOutputProcessor(display, opts);

					// THIS IS THE PART BEING TESTED.
					try {
						const output: AnyJson = rop.processRunOutput(fakeCsvResults);
						expect(true).to.equal(false, `Unexpectedly returned ${output} instead of throwing error`);
					} catch (e) {
						expect(display.getOutputText()).to.equal("[Info]: " + FAKE_CSV_OUTPUT);
						expect(e.message).to.equal(getMessage(BundleName.RunOutputProcessor, 'output.sevThresholdSummary', [2]), 'Exception message incorrectly formed');
					}
				});

			});

			describe('Test Case: JSON', () => {
				const fakeJsonResults: RecombinedRuleResults = {minSev: 1, summaryMap: FAKE_SUMMARY_MAP, results: FAKE_JSON_OUTPUT};

				// NOTE: This next test is based on the implementation of run-summary messages in W-8388246, which was known
				// to be incomplete. When we flesh out that implementation with summaries for other formats, this test might
				// need to change.
				it('JSON-type output with no violations should output be an empty violation set', async () => {
					const opts: RunOutputOptions = {
						format: OUTPUT_FORMAT.JSON
					};

					const rop = new RunOutputProcessor(display, opts);

					// THIS IS THE PART BEING TESTED
					const output: AnyJson = rop.processRunOutput(fakeJsonResults);

					expect(display.getOutputText()).to.equal("[Info]: " + FAKE_JSON_OUTPUT);
					expect(output).to.deep.equal(JSON.parse(FAKE_JSON_OUTPUT), 'JSON should be returned as a parsed object');
				});

				it('Throws severity-based exception on request', async () => {
					const opts: RunOutputOptions = {
						format: OUTPUT_FORMAT.JSON,
						severityForError: 1
					};

					const rop = new RunOutputProcessor(display, opts);

					// THIS IS THE PART BEING TESTED
					try {
						const output: AnyJson = rop.processRunOutput(fakeJsonResults);
						expect(true).to.equal(false, `Unexpectedly returned ${output} instead of throwing error`);
					} catch (e) {
						expect(display.getOutputText()).to.equal("[Info]: " + FAKE_JSON_OUTPUT);
						expect(e.message).to.equal(getMessage(BundleName.RunOutputProcessor, 'output.sevThresholdSummary', [1]), 'Exception message incorrectly formed');
					}
				});
			});
		});

		describe('Writing to file', () => {
			const fakeFilePath = './some/path/to/a/file.csv';
			it('Empty results yield expected message', async () => {
				const opts: RunOutputOptions = {
					format: OUTPUT_FORMAT.CSV,
					outfile: fakeFilePath
				};
				const rop = new RunOutputProcessor(display, opts);
				const summaryMap: Map<string, EngineExecutionSummary> = new Map();
				summaryMap.set('pmd', {fileCount: 0, violationCount: 0});
				summaryMap.set('eslint', {fileCount: 0, violationCount: 0});
				const fakeRes: RecombinedRuleResults = {minSev: 0, summaryMap, results: '"Problem","Severity","File","Line","Column","Rule","Description","URL","Category","Engine"'};

				// THIS IS THE PART BEING TESTED.
				const output: AnyJson = rop.processRunOutput(fakeRes);

				// We expect the empty CSV output followed by the default engine summary and written-to-file messages are logged to the console
				const expectedMsg = `${getMessage(BundleName.RunOutputProcessor, 'output.engineSummaryTemplate', ['pmd', 0, 0])}
${getMessage(BundleName.RunOutputProcessor, 'output.engineSummaryTemplate', ['eslint', 0, 0])}
${getMessage(BundleName.RunOutputProcessor, 'output.writtenToOutFile', [fakeFilePath])}`;

				expect(display.getOutputText()).to.equal("[Info]: " + expectedMsg);
				expect(fakeFiles.length).to.equal(1, 'A CSV file with only a header should be created');
				expect(output).to.equal(expectedMsg, 'Should have returned expected message');
			});

			describe('Test Case: CSV', () => {
				const fakeCsvResults: RecombinedRuleResults = {minSev: 1, summaryMap: FAKE_SUMMARY_MAP, results: FAKE_CSV_OUTPUT};

				it('Results are properly written to file', async () => {
					const opts: RunOutputOptions = {
						format: OUTPUT_FORMAT.CSV,
						outfile: fakeFilePath
					};

					const rop = new RunOutputProcessor(display, opts);

					// THIS IS THE PART BEING TESTED.
					const output: AnyJson = rop.processRunOutput(fakeCsvResults);

					const expectedCsvSummary = `${getMessage(BundleName.RunOutputProcessor, 'output.engineSummaryTemplate', ['pmd', 1, 1])}
${getMessage(BundleName.RunOutputProcessor, 'output.engineSummaryTemplate', ['eslint-typescript', 2, 1])}
${getMessage(BundleName.RunOutputProcessor, 'output.writtenToOutFile', [fakeFilePath])}`;
					expect(display.getOutputText()).to.equal("[Info]: " + expectedCsvSummary);
					expect(output).to.equal(expectedCsvSummary, 'Summary should be returned as final message');
					expect(fakeFiles.length).to.equal(1, 'Should have tried to create one file');
					expect(fakeFiles[0]).to.deep.equal({path: fakeFilePath, data: FAKE_CSV_OUTPUT}, 'File-write expectations defied');
				});

				it('Throws severity-based exception on request', async () => {
					const opts: RunOutputOptions = {
						format: OUTPUT_FORMAT.CSV,
						severityForError: 1,
						outfile: fakeFilePath
					};

					const rop = new RunOutputProcessor(display, opts);

					// THIS IS THE PART BEING TESTED.
					try {
						const output: AnyJson = rop.processRunOutput(fakeCsvResults);
						expect(true).to.equal(false, `Unexpectedly returned ${output} instead of throwing error`);
					} catch (e) {
						expect(display.getOutputText()).to.equal("");
						expect(fakeFiles.length).to.equal(1, 'Should have tried to create one file');
						expect(fakeFiles[0]).to.deep.equal({path: fakeFilePath, data: FAKE_CSV_OUTPUT}, 'File-write expectations defied');
						const expectedCsvSummary = `${getMessage(BundleName.RunOutputProcessor, 'output.engineSummaryTemplate', ['pmd', 1, 1])}
${getMessage(BundleName.RunOutputProcessor, 'output.engineSummaryTemplate', ['eslint-typescript', 2, 1])}
${getMessage(BundleName.RunOutputProcessor, 'output.sevThresholdSummary', [1])}
${getMessage(BundleName.RunOutputProcessor, 'output.writtenToOutFile', [fakeFilePath])}`;
						expect(e.message).to.equal(expectedCsvSummary, 'Summary was wrong');
					}
				});
			});
		});
	});
});
