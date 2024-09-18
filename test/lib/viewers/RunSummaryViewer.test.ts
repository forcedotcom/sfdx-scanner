import fs from "node:fs/promises";
import path from "node:path";
import ansis from 'ansis';
import {CodeAnalyzer, CodeAnalyzerConfig, RunResults} from '@salesforce/code-analyzer-core';
import {Violation} from "@salesforce/code-analyzer-engine-api";

import {RunSummaryDisplayer} from '../../../src/lib/viewers/RunSummaryViewer';

import {DisplayEvent, DisplayEventType, SpyDisplay} from "../../stubs/SpyDisplay";
import {FunctionalStubEnginePlugin1, StubEngine1} from '../../stubs/StubEnginePlugins';

const PATH_TO_COMPARISON_FILES = path.resolve(__dirname, '..', '..', '..', 'test', 'fixtures', 'comparison-files', 'lib',
	'viewers', 'RunSummaryViewer.test.ts');
const PATH_TO_SAMPLE_CODE = path.resolve(__dirname, '..', '..', '..', 'test', 'sample-code');
const PATH_TO_OUTFILE1 = path.join('the', 'specifics', 'of', 'this', 'path', 'do', 'not', 'matter.csv');
const PATH_TO_OUTFILE2 = path.join('neither', 'do', 'the', 'specifics', 'of', 'this', 'one.json');
const PATH_TO_FILE_A = path.resolve(PATH_TO_SAMPLE_CODE, 'fileA.cls');
const PATH_TO_FILE_Z = path.resolve(PATH_TO_SAMPLE_CODE, 'fileZ.cls');

describe('RunSummaryViewer implementations', () => {
	// We need SpyDisplays for the cases where inputs are empty and non-empty.
	const emptyInputsSpyDisplay: SpyDisplay = new SpyDisplay();
	const nonEmptyInputsSpyDisplay: SpyDisplay = new SpyDisplay();

	// We need a config, core, and plugin.
	const config: CodeAnalyzerConfig = CodeAnalyzerConfig.withDefaults();
	const codeAnalyzerCore: CodeAnalyzer = new CodeAnalyzer(config);
	const stubEnginePlugin: FunctionalStubEnginePlugin1 = new FunctionalStubEnginePlugin1();

	let emptyResults: RunResults;
	let nonEmptyResults: RunResults;

	// The tests are similar enough that we can do all of the setup in the `beforeAll()` functions.
	beforeAll(async () => {
		// Add the stub plugin to the core.
		await codeAnalyzerCore.addEnginePlugin(stubEnginePlugin);

		// Run the core once without assigning any violations.
		const workspace = await codeAnalyzerCore.createWorkspace(['package.json']);
		const rules = await codeAnalyzerCore.selectRules(['all'], {workspace});
		emptyResults = await codeAnalyzerCore.run(rules, {workspace});

		// Assign some violations and then run the core again.
		const engine1 = stubEnginePlugin.getCreatedEngine(`stubEngine1`) as StubEngine1;
		const rule1 = (await engine1.describeRules())[0];
		const rule2 = (await engine1.describeRules())[1];
		const violations: Violation[] = [
			// A low-severity violation late in a high-alphabetical file.
			createViolation(rule1.name, PATH_TO_FILE_A, 20, 1),
			// A low-severity violation early in the same high-alphabetical file.
			createViolation(rule1.name, PATH_TO_FILE_A, 1, 1),
			// A low-severity violation early in a low-alphabetical file.
			createViolation(rule1.name, PATH_TO_FILE_Z, 1, 1),
			// A high-severity violation later in the same low-alphabetical file.
			createViolation(rule2.name, PATH_TO_FILE_Z, 20, 1)
		];
		engine1.resultsToReturn = {violations};

		nonEmptyResults = await codeAnalyzerCore.run(rules, {workspace});
	});

	describe('RunSummaryDisplayer', () => {
		// Create Displayers for the empty-input and non-empty-input cases.
		const emptyInputsDisplayer: RunSummaryDisplayer = new RunSummaryDisplayer(emptyInputsSpyDisplay);
		const nonEmptyInputsDisplayer: RunSummaryDisplayer = new RunSummaryDisplayer(nonEmptyInputsSpyDisplay);

		let emptyInputsDisplayEvents: DisplayEvent[];
		let nonEmptyInputsDisplayEvents: DisplayEvent[];

		beforeAll(() => {
			emptyInputsDisplayer.view(emptyResults, config, []);
			emptyInputsDisplayEvents = emptyInputsSpyDisplay.getDisplayEvents();
			nonEmptyInputsDisplayer.view(nonEmptyResults, config, [PATH_TO_OUTFILE1, PATH_TO_OUTFILE2])
			nonEmptyInputsDisplayEvents = nonEmptyInputsSpyDisplay.getDisplayEvents();
		});

		describe('Formatting', () => {
			it('Output has correct header', () => {
				expect(ansis.strip(emptyInputsDisplayEvents[0].data)).toEqual('=== Summary');
				expect(ansis.strip(nonEmptyInputsDisplayEvents[0].data)).toEqual('=== Summary');
			});

			it('Output has correct log level', () => {
				for (const event of emptyInputsDisplayEvents) {
					expect(event.type).toEqual(DisplayEventType.LOG);
				}
				for (const event of nonEmptyInputsDisplayEvents) {
					expect(event.type).toEqual(DisplayEventType.LOG);
				}
			});
		});

		describe('Results breakdown', () => {
			it('When no violations exist, correctly outputs this fact', () => {
				const contents = emptyInputsDisplayEvents.map(e => e.data).join('\n');
				expect(contents).toContain('Found 0 violations.\n');
			});

			it('When violations exist, they are broken down by severity', async () => {
				const contents = nonEmptyInputsDisplayEvents.map(e => e.data).join('\n');
				const expectedViolationSummary = await readComparisonFile('four-unique-violations-summary.txt');
				expect(contents).toContain(expectedViolationSummary);
			});
		});

		describe('Outfile breakdown', () => {
			it('When no outfiles were provided, correctly outputs this fact', () => {
				const contents = emptyInputsDisplayEvents.map(e => e.data).join('\n');
				expect(contents).toContain('No results files were specified.\n');
			});

			it('When outfiles were provided, they are properly listed', () => {
				const contents = nonEmptyInputsDisplayEvents.map(e => e.data).join('\n');
				const expectation = `Results written to:\n` +
					`    ${PATH_TO_OUTFILE1}\n` +
					`    ${PATH_TO_OUTFILE2}\n`;
				expect(contents).toContain(expectation);
			});
		});

		describe('Logging breakdown', () => {
			it('Logfile is correctly displayed', () => {
				const expectation = `Additional log information written to:\n` +
					`    ${config.getLogFolder()}`;
				const emptyContents = emptyInputsDisplayEvents.map(e => e.data).join('\n');
				const nonEmptyContents = nonEmptyInputsDisplayEvents.map(e => e.data).join('\n');
				expect(emptyContents).toContain(expectation);
				expect(nonEmptyContents).toContain(expectation);
			});
		});
	});
});

function createViolation(ruleName: string, file: string, startLine: number, startColumn: number): Violation {
	return {
		ruleName,
		message: 'This is a message',
		codeLocations: [{
			file,
			startLine,
			startColumn
		}],
		primaryLocationIndex: 0
	};
}

function readComparisonFile(fileName: string): Promise<string> {
	return fs.readFile(path.join(PATH_TO_COMPARISON_FILES, fileName), {encoding: 'utf-8'});
}
