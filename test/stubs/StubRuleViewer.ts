import {Rule} from '@salesforce/code-analyzer-core';
import {RuleViewer} from '../../src/lib/viewers/RuleViewer';


export class StubRuleViewer implements RuleViewer {
	private viewedRules: Rule[] = [];

	public view(rules: Rule[]): void {
		this.viewedRules.push(...rules);
	}

	public expectViewedRules(ruleNames: string[]): void {
		expect(this.viewedRules).toHaveLength(ruleNames.length);

		for (let i = 0; i < ruleNames.length; i++) {
			expect(this.viewedRules[i].getName()).toEqual(ruleNames[i]);
		}
	}
}
