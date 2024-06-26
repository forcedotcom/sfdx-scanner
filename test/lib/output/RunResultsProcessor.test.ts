import {expect} from 'chai';

import {RunOutputOptions} from '../../../src/lib/output/RunResultsProcessor';
import {EngineExecutionSummary} from '../../../src/types';
import {AnyJson} from '@salesforce/ts-types';
import Sinon = require('sinon');
import fs = require('fs');
import {BundleName, getMessage} from "../../../src/MessageCatalog";
import {FakeDisplay} from "../FakeDisplay";
import {PATHLESS_COLUMNS} from "../../../src/lib/output/TableOutputFormatter";
import {OutputFormat} from "../../../src/lib/output/OutputFormat";
import {Results} from "../../../src/lib/output/Results";
import {FakeResults} from "./FakeResults";
import {JsonReturnValueHolder} from "../../../src/lib/output/JsonReturnValueHolder";
import {ResultsProcessor} from "../../../src/lib/output/ResultsProcessor";
import {ResultsProcessorFactoryImpl} from "../../../src/lib/output/ResultsProcessorFactory";
import {Display} from "../../../src/lib/Display";

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
					format: OutputFormat.TABLE,
					verboseViolations: false
				};
				const jsonHolder: JsonReturnValueHolder = new JsonReturnValueHolder();
				const rrp = createResultsProcessor(display, opts, jsonHolder);
				const summaryMap: Map<string, EngineExecutionSummary> = new Map();
				summaryMap.set('pmd', {fileCount: 0, violationCount: 0});
				summaryMap.set('eslint', {fileCount: 0, violationCount: 0});
				const fakeResults: Results = new FakeResults()
					.withMinSev(0).withSummaryMap(summaryMap).withFormattedOutput('');

				// THIS IS THE PART BEING TESTED.
				await rrp.processResults(fakeResults);
				const output: AnyJson = jsonHolder.get();

				// We expect that the message logged to the console and the message returned should both be the default
				const expectedMsg = getMessage(BundleName.RunOutputProcessor, 'output.noViolationsDetected', ['pmd, eslint']);
				expect(display.getOutputText()).to.equal("[Info]: " + expectedMsg, 'Should have displayed expected message');
				expect(output).to.equal(expectedMsg, 'Should have returned expected message');
			});

			describe('Test Case: Table', () => {
				const fakeTableResults: Results = new FakeResults()
					.withMinSev(1).withSummaryMap(FAKE_SUMMARY_MAP).withFormattedOutput(FAKE_TABLE_OUTPUT);

				it('Table-type output should be followed by summary', async () => {
					const opts: RunOutputOptions = {
						format: OutputFormat.TABLE,
						verboseViolations: false
					};
					const jsonHolder: JsonReturnValueHolder = new JsonReturnValueHolder();
					const rrp = createResultsProcessor(display, opts, jsonHolder);

					// THIS IS THE PART BEING TESTED.
					await rrp.processResults(fakeTableResults);
					const output: AnyJson = jsonHolder.get();

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
						format: OutputFormat.TABLE,
						verboseViolations: false,
						severityForError: 1
					};
					const jsonHolder: JsonReturnValueHolder = new JsonReturnValueHolder();
					const rrp = createResultsProcessor(display, opts, jsonHolder);

					// THIS IS THE PART BEING TESTED.
					try {
						await rrp.processResults(fakeTableResults);
						const output: AnyJson = jsonHolder.get();
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
				const fakeCsvResults: Results = new FakeResults()
					.withMinSev(1).withSummaryMap(FAKE_SUMMARY_MAP).withFormattedOutput(FAKE_CSV_OUTPUT);

				// NOTE: This next test is based on the implementation of run-summary messages in W-8388246, which was known
				// to be incomplete. When we flesh out that implementation with summaries for other formats, this test might
				// need to change.
				it('CSV-type output should NOT be followed by summary', async () => {
					const opts: RunOutputOptions = {
						format: OutputFormat.CSV,
						verboseViolations: false
					};

					const jsonHolder: JsonReturnValueHolder = new JsonReturnValueHolder();
					const rrp = createResultsProcessor(display, opts, jsonHolder);

					// THIS IS THE PART BEING TESTED.
					await rrp.processResults(fakeCsvResults);
					const output: AnyJson = jsonHolder.get();
					expect(display.getOutputText()).to.equal("[Info]: " + FAKE_CSV_OUTPUT);
					expect(output).to.equal(FAKE_CSV_OUTPUT, 'CSV should be returned as a string');
				});

				it('Throws severity-based exception on request', async () => {
					const opts: RunOutputOptions = {
						format: OutputFormat.CSV,
						verboseViolations: false,
						severityForError: 2
					};

					const jsonHolder: JsonReturnValueHolder = new JsonReturnValueHolder();
					const rrp = createResultsProcessor(display, opts, jsonHolder);

					// THIS IS THE PART BEING TESTED.
					try {
						await rrp.processResults(fakeCsvResults);
						const output: AnyJson = jsonHolder.get();
						expect(true).to.equal(false, `Unexpectedly returned ${output} instead of throwing error`);
					} catch (e) {
						expect(display.getOutputText()).to.equal("[Info]: " + FAKE_CSV_OUTPUT);
						expect(e.message).to.equal(getMessage(BundleName.RunOutputProcessor, 'output.sevThresholdSummary', [2]), 'Exception message incorrectly formed');
					}
				});

			});

			describe('Test Case: JSON', () => {
				const fakeJsonResults: Results = new FakeResults()
					.withMinSev(1).withSummaryMap(FAKE_SUMMARY_MAP).withFormattedOutput(FAKE_JSON_OUTPUT);

				// NOTE: This next test is based on the implementation of run-summary messages in W-8388246, which was known
				// to be incomplete. When we flesh out that implementation with summaries for other formats, this test might
				// need to change.
				it('JSON-type output with no violations should output be an empty violation set', async () => {
					const opts: RunOutputOptions = {
						format: OutputFormat.JSON,
						verboseViolations: false
					};

					const jsonHolder: JsonReturnValueHolder = new JsonReturnValueHolder();
					const rrp = createResultsProcessor(display, opts, jsonHolder);

					// THIS IS THE PART BEING TESTED
					await rrp.processResults(fakeJsonResults);
					const output: AnyJson = jsonHolder.get();

					expect(display.getOutputText()).to.equal("[Info]: " + FAKE_JSON_OUTPUT);
					expect(output).to.deep.equal(JSON.parse(FAKE_JSON_OUTPUT), 'JSON should be returned as a parsed object');
				});

				it('Throws severity-based exception on request', async () => {
					const opts: RunOutputOptions = {
						format: OutputFormat.JSON,
						verboseViolations: false,
						severityForError: 1
					};

					const jsonHolder: JsonReturnValueHolder = new JsonReturnValueHolder();
					const rrp = createResultsProcessor(display, opts, jsonHolder);

					// THIS IS THE PART BEING TESTED
					try {
						await rrp.processResults(fakeJsonResults);
						const output: AnyJson = jsonHolder.get();
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
					format: OutputFormat.CSV,
					verboseViolations: false,
					outfile: fakeFilePath
				};
				const jsonHolder: JsonReturnValueHolder = new JsonReturnValueHolder();
				const rrp = createResultsProcessor(display, opts, jsonHolder);
				const summaryMap: Map<string, EngineExecutionSummary> = new Map();
				summaryMap.set('pmd', {fileCount: 0, violationCount: 0});
				summaryMap.set('eslint', {fileCount: 0, violationCount: 0});
				const fakeResults: Results = new FakeResults()
					.withMinSev(0).withSummaryMap(summaryMap).withFormattedOutput('"Problem","Severity","File","Line","Column","Rule","Description","URL","Category","Engine"');

				// THIS IS THE PART BEING TESTED.
				await rrp.processResults(fakeResults);
				const output: AnyJson = jsonHolder.get();

				// We expect the empty CSV output followed by the default engine summary and written-to-file messages are logged to the console
				const expectedMsg = `${getMessage(BundleName.RunOutputProcessor, 'output.engineSummaryTemplate', ['pmd', 0, 0])}
${getMessage(BundleName.RunOutputProcessor, 'output.engineSummaryTemplate', ['eslint', 0, 0])}
${getMessage(BundleName.RunOutputProcessor, 'output.writtenToOutFile', [fakeFilePath])}`;

				expect(display.getOutputText()).to.equal("[Info]: " + expectedMsg);
				expect(fakeFiles.length).to.equal(1, 'A CSV file with only a header should be created');
				expect(output).to.equal(expectedMsg, 'Should have returned expected message');
			});

			describe('Test Case: CSV', () => {
				const fakeCsvResults: Results = new FakeResults()
					.withMinSev(1).withSummaryMap(FAKE_SUMMARY_MAP).withFormattedOutput(FAKE_CSV_OUTPUT)

				it('Results are properly written to file', async () => {
					const opts: RunOutputOptions = {
						format: OutputFormat.CSV,
						verboseViolations: false,
						outfile: fakeFilePath
					};
					const jsonHolder: JsonReturnValueHolder = new JsonReturnValueHolder();
					const rrp = createResultsProcessor(display, opts, jsonHolder);

					// THIS IS THE PART BEING TESTED.
					await rrp.processResults(fakeCsvResults);
					const output: AnyJson = jsonHolder.get();

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
						format: OutputFormat.CSV,
						verboseViolations: false,
						severityForError: 1,
						outfile: fakeFilePath
					};
					const jsonHolder: JsonReturnValueHolder = new JsonReturnValueHolder();
					const rrp = createResultsProcessor(display, opts, jsonHolder);

					// THIS IS THE PART BEING TESTED.
					try {
						await rrp.processResults(fakeCsvResults);
						const output: AnyJson = jsonHolder.get();
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

function createResultsProcessor(display: Display, runOutputOptions: RunOutputOptions,
								jsonReturnValueHolder: JsonReturnValueHolder): ResultsProcessor {
	return (new ResultsProcessorFactoryImpl()).createResultsProcessor(display, runOutputOptions, jsonReturnValueHolder);
}
