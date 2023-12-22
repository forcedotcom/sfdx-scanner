import {Action} from "../ScannerCommand";
import {Inputs, Rule} from "../../types";
import {AnyJson} from "@salesforce/ts-types";
import {RuleFilter} from "../RuleFilter";
import {Controller} from "../../Controller";
import {Bundle, getMessage} from "../../MessageCatalog";
import {deepCopy} from "../util/Utils";
import Dfa from "../../commands/scanner/run/dfa";
import Run from "../../commands/scanner/run";
import {Ux} from "@salesforce/sf-plugins-core";
import {Display} from "../Display";
import {RuleFilterFactory} from "../RuleFilterFactory";

type DescribeStyledRule = Rule & {
	runWith: string;
	enabled: boolean;
};

export class RuleDescribeAction implements Action {
	private readonly display: Display;
	private readonly ruleFilterFactory: RuleFilterFactory;

	public constructor(display: Display, ruleFilterFactory: RuleFilterFactory) {
		this.display = display;
		this.ruleFilterFactory = ruleFilterFactory;
	}

	public async validateInputs(inputs: Inputs): Promise<void> {
		// Currently there is nothing to validate
	}

	public async run(inputs: Inputs): Promise<AnyJson> {
		const jsonEnabled: boolean = inputs.json;

		const ruleFilters: RuleFilter[] = this.ruleFilterFactory.createRuleFilters(inputs);

		// It's possible for this line to throw an error, but that's fine because the error will be an SfError that we can
		// allow to boil over.
		const ruleManager = await Controller.createRuleManager();
		const rules: DescribeStyledRule[] = await this.styleRules(ruleManager.getRulesMatchingOnlyExplicitCriteria(ruleFilters));
		if (rules.length === 0) {
			// If we couldn't find any rules that fit the criteria, we'll let the user know. We'll use .warn() instead of .log()
			// so it's immediately obvious.
			this.display.displayWarning(getMessage(Bundle.Describe, 'output.noMatchingRules', [inputs.rulename as string]));
		} else if (rules.length > 1) {
			// If there was more than one matching rule, we'll let the user know, but we'll still output all the rules.
			const msg = getMessage(Bundle.Describe, 'output.multipleMatchingRules', [rules.length.toString(), inputs.rulename as string]);
			this.display.displayWarning(msg);
			rules.forEach((rule, idx) => {
				this.display.displayStyledHeader(`Rule #${idx + 1}`);
				this.displayStyledRule(rule, jsonEnabled);
			});
		} else {
			// If there's exactly one rule, we don't need to do anything special, and can just log the rule.
			this.displayStyledRule(rules[0], jsonEnabled);
		}
		// We need to return something for when the --json flag is used, so we'll just return the list of rules.
		return deepCopy(rules);
	}

	private async styleRules(rules: Rule[]): Promise<DescribeStyledRule[]> {
		// Opting to use .getAllEngines() instead of .getEnabledEngines() so we don't have to futz with the engineOptions param.
		const allEngines = await Controller.getAllEngines();
		const enabledEngineNames: Set<string> = new Set();
		for (const engine of allEngines) {
			if (await engine.isEnabled()) {
				enabledEngineNames.add(engine.getName());
			}
		}
		return rules.map(r => {
			const styledRule: DescribeStyledRule = {
				...r,
				runWith: r.isDfa ? Dfa.id : Run.id,
				isPilot: r.isPilot,
				enabled: enabledEngineNames.has(r.engine)
			};
			// Strip any whitespace off of the description.
			styledRule.description = styledRule.description.trim();
			return styledRule;
		});
	}

	private displayStyledRule(rule: DescribeStyledRule, jsonEnabled: boolean): void {
		new Ux({jsonEnabled: jsonEnabled})
			.styledObject(rule, ['name', 'engine', 'runWith', 'isPilot', 'enabled', 'categories', 'rulesets', 'languages', 'description', 'message']);
	}
}
