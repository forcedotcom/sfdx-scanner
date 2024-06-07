import {RuleDetailViewer} from '../../../src/lib/viewers/RuleViewer';
import {DisplayEventType, StubDisplay} from '../../stubs/StubDisplay';
import * as StubRules from '../../stubs/StubRules';

describe('RuleDetailViewer', () => {
	it('When given no rules, outputs summary and nothing else', () => {
		const display = new StubDisplay();
		const viewer = new RuleDetailViewer(display);

		viewer.view([]);

		display.expectDisplayEvents([{
			type: DisplayEventType.INFO,
			data: 'Found 0 rules.'
		}]);
	});

	it('When given one rule, outputs correct summary and rule data', () => {
		const display = new StubDisplay();
		const viewer = new RuleDetailViewer(display);
		const rule = new StubRules.StubRule1();

		viewer.view([
			rule
		]);

		display.expectDisplayEvents([{
			type: DisplayEventType.INFO,
			data: 'Found 1 rules:'
		}, {
			type: DisplayEventType.STYLED_HEADER,
			data: `1. ${rule.getName()}`
		}, {
			type: DisplayEventType.STYLED_OBJECT_IN_ORDER,
			data: JSON.stringify({
				obj: {
					engine: rule.getEngineName(),
					severity: rule.getFormattedSeverity(),
					type: rule.getFormattedType(),
					tags: rule.getFormattedTags(),
					url: rule.getFormattedResourceUrls(),
					description: rule.getDescription()
				},
				keys: ['engine', 'severity', 'type', 'tags', 'url', 'description']
			})
		}]);
	});

	it('When given multiple rules, outputs correct summary and rule data', () => {
		const display = new StubDisplay();
		const viewer = new RuleDetailViewer(display);
		const rule1 = new StubRules.StubRule1();
		const rule2 = new StubRules.StubRule2();

		viewer.view([
			rule1,
			rule2
		]);

		display.expectDisplayEvents([{
			type: DisplayEventType.INFO,
			data: 'Found 2 rules:'
		}, {
			type: DisplayEventType.STYLED_HEADER,
			data: `1. ${rule1.getName()}`
		}, {
			type: DisplayEventType.STYLED_OBJECT_IN_ORDER,
			data: JSON.stringify({
				obj: {
					engine: rule1.getEngineName(),
					severity: rule1.getFormattedSeverity(),
					type: rule1.getFormattedType(),
					tags: rule1.getFormattedTags(),
					url: rule1.getFormattedResourceUrls(),
					description: rule1.getDescription()
				},
				keys: ['engine', 'severity', 'type', 'tags', 'url', 'description']
			})
		}, {
			type: DisplayEventType.STYLED_HEADER,
			data: `2. ${rule2.getName()}`
		}, {
			type: DisplayEventType.STYLED_OBJECT_IN_ORDER,
			data: JSON.stringify({
				obj: {
					engine: rule2.getEngineName(),
					severity: rule2.getFormattedSeverity(),
					type: rule2.getFormattedType(),
					tags: rule2.getFormattedTags(),
					url: rule2.getFormattedResourceUrls(),
					description: rule2.getDescription()
				},
				keys: ['engine', 'severity', 'type', 'tags', 'url', 'description']
			})
		}]);
	});
});

describe('RuleTableViewer', () => {
	it('When given no rules, outputs summary and nothing else', () => {

	});

	it('When given one rule, outputs correct summary and rule data', () => {

	});

	it('When given multiple rules, outputs correct summary and rule data', () => {

	});
});
