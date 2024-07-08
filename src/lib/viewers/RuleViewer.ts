import {Ux} from '@salesforce/sf-plugins-core';
import {Rule, RuleType, SeverityLevel} from '@salesforce/code-analyzer-core';
import {Display} from '../Display';
import {BundleName, getMessage} from '../messages';

export interface RuleViewer {
	view(rules: Rule[]): void;
}


abstract class AbstractRuleViewer implements RuleViewer {
	protected display: Display;

	public constructor(display: Display) {
		this.display = display;
	}

	public view(rules: Rule[]): void {
		if (rules.length === 0) {
			this.display.displayLog(getMessage(BundleName.RuleViewer, 'summary.found-no-rules'));
		} else {
			this.display.displayLog(getMessage(BundleName.RuleViewer, 'summary.found-rules', [rules.length]));
			this._view(rules);
		}
	}

	protected abstract _view(rules: Rule[]): void;
}

export class RuleDetailViewer extends AbstractRuleViewer {
	protected _view(rules: Rule[]): void {
		for (let i = 0; i < rules.length; i++) {
			const rule = rules[i];
			this.display.displayStyledHeader(getMessage(BundleName.RuleViewer, 'summary.detail.header', [i + 1, rule.getName()]));
			const severity = rule.getSeverityLevel();
			this.display.displayStyledObject({
				engine: rule.getEngineName(),
				severity: `${severity.valueOf()} (${SeverityLevel[severity]})`,
				type: RuleType[rule.getType()],
				tags: rule.getTags().join(', '),
				url: rule.getResourceUrls().join(', '),
				description: rule.getDescription()
			}, ['engine', 'severity', 'type', 'tags', 'url', 'description']);
		}
	}
}

type RuleRow = {
	num: number;
	name: string;
	engine: string;
	severity: string;
	tag: string;
};

const TABLE_COLUMNS: Ux.Table.Columns<RuleRow> = {
	num: {
		header: getMessage(BundleName.RuleViewer, 'summary.table.num-column')
	},
	name: {
		header: getMessage(BundleName.RuleViewer, 'summary.table.name-column')
	},
	engine: {
		header: getMessage(BundleName.RuleViewer, 'summary.table.engine-column')
	},
	severity: {
		header: getMessage(BundleName.RuleViewer, 'summary.table.severity-column')
	},
	tag: {
		header: getMessage(BundleName.RuleViewer, 'summary.table.tag-column')
	}
};

export class RuleTableViewer extends AbstractRuleViewer {
	protected _view(rules: Rule[]): void {
		const ruleJsons: RuleRow[] = rules.map((rule, idx) => {
			const severity = rule.getSeverityLevel();
			return {
				num: idx + 1,
				name: rule.getName(),
				engine: rule.getEngineName(),
				severity: `${severity.valueOf()} (${SeverityLevel[severity]})`,
				tag: rule.getTags().join(', ')
			};
		});
		this.display.displayTable(ruleJsons, TABLE_COLUMNS);
	}
}

