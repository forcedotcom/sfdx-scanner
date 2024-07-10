import fs from 'node:fs';
import path from 'path';
import ansis from 'ansis';
import {RuleDetailViewer, RuleTableViewer} from '../../../src/lib/viewers/RuleViewer';
import {DisplayEventType, SpyDisplay} from '../../stubs/SpyDisplay';
import * as StubRules from '../../stubs/StubRules';

const PATH_TO_COMPARISON_FILES = path.resolve(__dirname, '..', '..', '..', 'test', 'fixtures', 'comparison-files', 'lib',
	'viewers', 'RuleViewer.test.ts');

describe('RuleViewer implementations', () => {
	describe('RuleDetailViewer', () => {
		it('When given no rules, outputs summary and nothing else', () => {
			const display = new SpyDisplay();
			const viewer = new RuleDetailViewer(display);

			viewer.view([]);

			const displayEvents = display.getDisplayEvents();
			expect(displayEvents).toHaveLength(1);
			expect(displayEvents).toEqual([{
				type: DisplayEventType.LOG,
				data: '*DRAFT*: Found 0 rules.'
			}]);
		});

		it('When given one rule, outputs correct summary and correctly styled rule data', () => {
			const display = new SpyDisplay();
			const viewer = new RuleDetailViewer(display);
			const rule = new StubRules.StubRule1();

			viewer.view([
				rule
			]);

			const actualDisplayEvents = display.getDisplayEvents();
			expect(actualDisplayEvents).toHaveLength(2);
			for (const displayEvent of actualDisplayEvents) {
				expect(displayEvent.type).toEqual(DisplayEventType.LOG);
			}
			// Rip off all of ansis's styling, so we're just comparing plain text.
			const actualEventText = ansis.strip(actualDisplayEvents.map(e => e.data).join('\n'));

			const expectedRuleDetails = readComparisonFile('one-rule-details.txt');
			expect(actualEventText).toEqual(expectedRuleDetails);
		});

		it('When given multiple rules, outputs correct summary and correctly styled rule data', () => {
			const display = new SpyDisplay();
			const viewer = new RuleDetailViewer(display);
			const rule1 = new StubRules.StubRule1();
			const rule2 = new StubRules.StubRule2();

			viewer.view([
				rule1,
				rule2
			]);

			const actualDisplayEvents = display.getDisplayEvents();
			expect(actualDisplayEvents).toHaveLength(2);
			for (const displayEvent of actualDisplayEvents) {
				expect(displayEvent.type).toEqual(DisplayEventType.LOG);
			}
			// Rip off all of ansis's styling, so we're just comparing plain text.
			const actualEventText = ansis.strip(actualDisplayEvents.map(e => e.data).join('\n'));

			const expectedRuleDetails = readComparisonFile('two-rules-details.txt');
			expect(actualEventText).toEqual(expectedRuleDetails);
		});
	});

	describe('RuleTableViewer', () => {
		it('When given no rules, outputs summary and nothing else', () => {
			const display = new SpyDisplay();
			const viewer = new RuleTableViewer(display);

			viewer.view([]);

			const displayEvents = display.getDisplayEvents();
			expect(displayEvents).toHaveLength(1);
			expect(displayEvents).toEqual([{
				type: DisplayEventType.LOG,
				data: '*DRAFT*: Found 0 rules.'
			}]);
		});

		it('When given one rule, outputs correct summary and rule data', () => {
			const display = new SpyDisplay();
			const viewer = new RuleTableViewer(display);
			const rule = new StubRules.StubRule1();

			viewer.view([
				rule
			]);

			const displayEvents = display.getDisplayEvents();
			expect(displayEvents).toHaveLength(2);
			expect(displayEvents).toEqual([{
				type: DisplayEventType.LOG,
				data: '*DRAFT*: Found 1 rules:'
			}, {
				type: DisplayEventType.TABLE,
				data: JSON.stringify({
					columns: ['#', 'Name', 'Engine', 'Severity', 'Tag'],
					rows: [{
						num: 1,
						name: rule.getName(),
						engine: rule.getEngineName(),
						severity: rule.getFormattedSeverity(),
						tag: rule.getFormattedTags()
					}]
				})
			}])
		});

		it('When given multiple rules, outputs correct summary and rule data', () => {
			const display = new SpyDisplay();
			const viewer = new RuleTableViewer(display);
			const rule1 = new StubRules.StubRule1();
			const rule2 = new StubRules.StubRule2();

			viewer.view([
				rule1,
				rule2
			]);

			const displayEvents = display.getDisplayEvents();
			expect(displayEvents).toHaveLength(2);
			expect(displayEvents).toEqual([{
				type: DisplayEventType.LOG,
				data: '*DRAFT*: Found 2 rules:'
			}, {
				type: DisplayEventType.TABLE,
				data: JSON.stringify({
					columns: ['#', 'Name', 'Engine', 'Severity', 'Tag'],
					rows: [{
						num: 1,
						name: rule1.getName(),
						engine: rule1.getEngineName(),
						severity: rule1.getFormattedSeverity(),
						tag: rule1.getFormattedTags()
					}, {
						num: 2,
						name: rule2.getName(),
						engine: rule2.getEngineName(),
						severity: rule2.getFormattedSeverity(),
						tag: rule2.getFormattedTags()
					}]
				})
			}]);
		});
	});
});

function readComparisonFile(fileName: string): string {
	return fs.readFileSync(path.join(PATH_TO_COMPARISON_FILES, fileName), {encoding: 'utf-8'});
}
