import {RuleDetailViewer, RuleTableViewer} from '../../../src/lib/viewers/RuleViewer';
import {toStyledHeaderAndBody} from '../../../src/lib/utils/StylingUtil';
import {DisplayEventType, SpyDisplay} from '../../stubs/SpyDisplay';
import * as StubRules from '../../stubs/StubRules';

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

			const displayEvents = display.getDisplayEvents();
			expect(displayEvents).toHaveLength(2);
			expect(displayEvents).toEqual([{
				type: DisplayEventType.LOG,
				data: '*DRAFT*: Found 1 rules:'
			}, {
				type: DisplayEventType.LOG,
				data: toStyledHeaderAndBody(`*DRAFT*: 1. ${rule.getName()}`, {
					engine: rule.getEngineName(),
					severity: rule.getFormattedSeverity(),
					type: rule.getFormattedType(),
					tags: rule.getFormattedTags(),
					resources: rule.getFormattedResourceUrls(),
					description: rule.getDescription()
				}, ['engine', 'severity', 'type', 'tags', 'resources', 'description'])
			}]);
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

			const displayEvents = display.getDisplayEvents();
			expect(displayEvents).toHaveLength(2);
			expect(displayEvents).toEqual([{
				type: DisplayEventType.LOG,
				data: '*DRAFT*: Found 2 rules:'
			}, {
				type: DisplayEventType.LOG,
				data: toStyledHeaderAndBody(`*DRAFT*: 1. ${rule1.getName()}`, {
					engine: rule1.getEngineName(),
					severity: rule1.getFormattedSeverity(),
					type: rule1.getFormattedType(),
					tags: rule1.getFormattedTags(),
					resources: rule1.getFormattedResourceUrls(),
					description: rule1.getDescription()
				}, ['engine', 'severity', 'type', 'tags', 'resources', 'description'])
				+ '\n\n'
				+ toStyledHeaderAndBody(`*DRAFT*: 2. ${rule2.getName()}`, {
					engine: rule2.getEngineName(),
					severity: rule2.getFormattedSeverity(),
					type: rule2.getFormattedType(),
					tags: rule2.getFormattedTags(),
					resources: rule2.getFormattedResourceUrls(),
					description: rule2.getDescription()
				}, ['engine', 'severity', 'type', 'tags', 'resources', 'description'])
			}]);
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
