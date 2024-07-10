import path from 'node:path';
import * as fs from 'node:fs/promises';
import ansis from 'ansis';
import {CodeAnalyzer, CodeAnalyzerConfig, SeverityLevel} from '@salesforce/code-analyzer-core';
import {Engine, RuleDescription, Violation} from '@salesforce/code-analyzer-engine-api';

import {ResultsDetailViewer, ResultsTableViewer} from '../../../src/lib/viewers/ResultsViewer';
import {BundleName, getMessage} from '../../../src/lib/messages';
import {DisplayEvent, DisplayEventType, SpyDisplay} from '../../stubs/SpyDisplay';
import {FunctionalStubEnginePlugin1, StubEngine1} from '../../stubs/StubEnginePlugins';

const PATH_TO_COMPARISON_FILES = path.resolve(__dirname, '..', '..', '..', 'test', 'fixtures', 'comparison-files', 'lib',
	'viewers', 'ResultsViewer.test.ts');

const PATH_TO_SOME_FILE = path.resolve(__dirname, '..', '..', '..', 'test', 'sample-code', 'someFile.cls');
const PATH_TO_FILE_A = path.resolve(__dirname, '..', '..', '..', 'test', 'sample-code', 'fileA.cls');
const PATH_TO_FILE_Z = path.resolve(__dirname, '..', '..', '..', 'test', 'sample-code', 'fileZ.cls');

describe('ResultsViewer implementations', () => {

	let spyDisplay: SpyDisplay;
	let codeAnalyzerCore: CodeAnalyzer;
	let stubEnginePlugin: FunctionalStubEnginePlugin1;
	let engine1: StubEngine1;
	let rule1: RuleDescription;
	let rule2: RuleDescription;

	beforeEach(async () => {
		spyDisplay = new SpyDisplay();
		codeAnalyzerCore = new CodeAnalyzer(CodeAnalyzerConfig.withDefaults());
		stubEnginePlugin = new FunctionalStubEnginePlugin1();
		await codeAnalyzerCore.addEnginePlugin(stubEnginePlugin);
		engine1 = stubEnginePlugin.getCreatedEngine('stubEngine1') as StubEngine1;
		rule1 = (await engine1.describeRules())[0];
		rule2 = (await engine1.describeRules())[1];
	});

	describe('ResultsDetailViewer', () => {
		let viewer: ResultsDetailViewer;

		beforeEach(() => {
			viewer = new ResultsDetailViewer(spyDisplay);
		});

		it('When given no results, outputs top-level count and nothing else', async () => {
			// ==== TEST SETUP ====
			// "Run" the plugin without assigning any violations.
			const workspace = await codeAnalyzerCore.createWorkspace(['package.json']);
			const rules = await codeAnalyzerCore.selectRules(['all'], {workspace});
			const results = await codeAnalyzerCore.run(rules, {workspace});

			// ==== TESTED METHOD ====
			// Pass the empty result object into the viewer.
			viewer.view(results);

			// ==== ASSERTIONS ====
			// Assert against our messages.
			const displayEvents = spyDisplay.getDisplayEvents();
			expect(displayEvents).toHaveLength(1);
			expect(displayEvents).toEqual([{
				type: DisplayEventType.LOG,
				data: getMessage(BundleName.ResultsViewer, 'summary.found-no-results')
			}]);
		});

		it(`When there are violations, all are shown`, async () => {
			// ==== TEST SETUP ====
			// This test doesn't care about sorting, so just assign our engine several copies of the same violation.
			const violations: Violation[] = repeatViolation(
				createViolation(rule1.name, PATH_TO_SOME_FILE, 1, 1),
				4);
			engine1.resultsToReturn = {violations};
			const workspace = await codeAnalyzerCore.createWorkspace([PATH_TO_SOME_FILE]);
			const rules = await codeAnalyzerCore.selectRules(['all'], {workspace});
			// "Run" the plugin.
			const results = await codeAnalyzerCore.run(rules, {workspace});

			// ==== TESTED METHOD ====
			// Pass the result object into the viewer.
			viewer.view(results);

			// ==== ASSERTIONS ====
			// Compare the text in the events with the text in our comparison file.
			const actualDisplayEvents: DisplayEvent[] = spyDisplay.getDisplayEvents();
			for (const event of actualDisplayEvents) {
				expect(event.type).toEqual(DisplayEventType.LOG);
			}
			// Rip off all of ansis's styling, so we're just comparing plain text.
			const actualEventText = ansis.strip(actualDisplayEvents.map(e => e.data).join('\n'));
			const expectedViolationDetails = (await readComparisonFile('four-identical-violations-details.txt'))
				.replace(/__PATH_TO_SOME_FILE__/g, PATH_TO_SOME_FILE);
			expect(actualEventText).toEqual(expectedViolationDetails);
		});

		// The reasoning behind this sorting order is so that the Detail view can function as a "show me the N most
		// severe violations" option.
		it('Results are sorted by severity, then file, then location', async () => {
			// ==== TEST SETUP ====
			// Populate the engine with:
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
			// "Run" the plugin.
			const workspace = await codeAnalyzerCore.createWorkspace(['package.json']);
			const rules = await codeAnalyzerCore.selectRules(['all'], {workspace});
			const results = await codeAnalyzerCore.run(rules, {workspace});

			// ==== TESTED METHOD ====
			// Pass the result object into the viewer.
			viewer.view(results);

			// ==== ASSERTIONS ====
			// Compare the text in the events with the text in our comparison file.
			const actualDisplayEvents: DisplayEvent[] = spyDisplay.getDisplayEvents();
			for (const event of actualDisplayEvents) {
				expect(event.type).toEqual(DisplayEventType.LOG);
			}
			// Rip off all of ansis's styling, so we're just comparing plain text.
			const actualEventText = ansis.strip(actualDisplayEvents.map(e => e.data).join('\n'));
			const expectedViolationDetails = (await readComparisonFile('four-unique-violations-details.txt'))
				.replace(/__PATH_TO_FILE_A__/g, PATH_TO_FILE_A)
				.replace(/__PATH_TO_FILE_Z__/g, PATH_TO_FILE_Z);
			expect(actualEventText).toEqual(expectedViolationDetails);
		});
	});

	describe('ResultsTableViewer', () => {
		type TableRow = {
			num: number;
			location: string;
			rule: string;
			message: string;
			severity: string;
		};

		let viewer: ResultsTableViewer;

		beforeEach(() => {
			viewer = new ResultsTableViewer(spyDisplay);
		})

		it('When given no results, outputs top-level count and nothing else', async () => {
			// ==== SETUP ====
			const workspace = await codeAnalyzerCore.createWorkspace(['package.json']);
			const rules = await codeAnalyzerCore.selectRules(['all']);
			// Run without having assigned any violations.
			const results = await codeAnalyzerCore.run(rules, {workspace});

			// ==== TESTED BEHAVIOR ====
			// Pass the empty results object into the viewer.
			viewer.view(results);

			// ==== ASSERTIONS ====
			const displayEvents = spyDisplay.getDisplayEvents();
			expect(displayEvents).toHaveLength(1);
			expect(displayEvents).toEqual([{
				type: DisplayEventType.LOG,
				data: getMessage(BundleName.ResultsViewer, 'summary.found-no-results')
			}]);
		});

		it('When given violations, they are displayed as a table', async () => {
			// ==== SETUP ====
			// This test doesn't care about sorting, so just assign our engine several copies of the same violation.
			const violations: Violation[] = repeatViolation(
				createViolation(rule1.name, PATH_TO_SOME_FILE, 1, 1),
				10
			);
			engine1.resultsToReturn = {violations};
			const workspace = await codeAnalyzerCore.createWorkspace([PATH_TO_SOME_FILE]);
			const rules = await codeAnalyzerCore.selectRules(['all']);
			// "Run" the plugin.
			const results = await codeAnalyzerCore.run(rules, {workspace});

			// ==== TESTED BEHAVIOR ====
			viewer.view(results);

			// ==== ASSERTIONS ====
			const displayEvents = spyDisplay.getDisplayEvents();
			expect(displayEvents).toHaveLength(2);
			const expectedTableRows = violations.map((v, idx) => {
				return createTableRowExpectation(rule1, v, engine1, idx);
			});
			expect(displayEvents).toEqual([{
				type: DisplayEventType.LOG,
				data: getMessage(BundleName.ResultsViewer, 'summary.found-results', [10, 1])
			}, {
				type: DisplayEventType.TABLE,
				data: JSON.stringify({
					columns: ['#', 'Severity', 'Rule', 'Location', 'Message'],
					rows: expectedTableRows
				})
			}]);
		});

		// The reasoning behind this sorting order is so that the Table view can function as a "show me all the violations
		// in File X" option.
		it('Results are sorted by severity, then file, then location', async () => {
			// ==== SETUP ====
			// Populate the engine with:
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
			const workspace = await codeAnalyzerCore.createWorkspace(['package.json']);
			const rules = await codeAnalyzerCore.selectRules(['all'], {workspace});
			const results = await codeAnalyzerCore.run(rules, {workspace});

			// ==== TESTED BEHAVIOR ====
			viewer.view(results);

			// ==== ASSERTIONS ====
			const expectedRows: TableRow[] = [
				// Violation 4 has the highest severity, so it goes first.
				createTableRowExpectation(rule2, violations[3], engine1, 0),
				// 1, 2, and 3 are tied for severity and 1 and 2 are tied for highest file,
				// but 2 has the earliest location, so it goes next.
				createTableRowExpectation(rule1, violations[1], engine1, 1),
				// Violation 1 is later in the same high-alphabetical file, so it's next.
				createTableRowExpectation(rule1, violations[0], engine1, 2),
				// Violation 3 is last.
				createTableRowExpectation(rule1, violations[2], engine1, 3),
			]
			const displayEvents = spyDisplay.getDisplayEvents();
			expect(displayEvents).toHaveLength(2);
			expect(displayEvents).toEqual([{
				type: DisplayEventType.LOG,
				data: getMessage(BundleName.ResultsViewer, 'summary.found-results', [4, 2])
			}, {
				type: DisplayEventType.TABLE,
				data: JSON.stringify({
					columns: ['#', 'Severity', 'Rule', 'Location', 'Message'],
					rows: expectedRows
				})
			}])
		});

		function createTableRowExpectation(rule: RuleDescription, violation: Violation, engine: Engine, index: number): TableRow {
			const primaryLocation = violation.codeLocations[violation.primaryLocationIndex];
			return {
				num: index + 1,
				location: `${primaryLocation.file}:${primaryLocation.startLine}:${primaryLocation.startColumn}`,
				rule: `${engine.getName()}:${rule.name}`,
				severity: `${rule.severityLevel} (${SeverityLevel[rule.severityLevel]})`,
				message: violation.message
			};
		}
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

function repeatViolation(violation: Violation, times: number): Violation[] {
	return Array(times).fill(violation);
}

function readComparisonFile(fileName: string): Promise<string> {
	return fs.readFile(path.join(PATH_TO_COMPARISON_FILES, fileName), {encoding: 'utf-8'});
}
