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

	public abstract view(rules: Rule[]): void;
}

export class RuleDetailViewer extends AbstractRuleViewer {
	public view(rules: Rule[]): void {
		if (rules.length === 0) {
			this.display.displayInfo(getMessage(BundleName.RuleViewer, 'summary.found-no-rules'));
		} else {
			this.display.displayInfo(getMessage(BundleName.RuleViewer, 'summary.found-rules', [rules.length]));
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
}

export class RuleTableViewer extends AbstractRuleViewer {
	public view(_rules: Rule[]): void {

	}
}

