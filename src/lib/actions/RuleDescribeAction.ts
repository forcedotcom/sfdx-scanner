import {Action} from "../ScannerCommand";
import {Inputs, Rule} from "../../types";
import {AnyJson} from "@salesforce/ts-types";
import {RuleFilter} from "../RuleFilter";
import {Controller} from "../../Controller";
import {BundleName, getMessage} from "../../MessageCatalog";
import {deepCopy} from "../util/Utils";
import Dfa from "../../commands/scanner/run/dfa";
import Run from "../../commands/scanner/run";
import {Display} from "../Display";
import {RuleFilterFactory} from "../RuleFilterFactory";
import {Pmd6CommandInfo, Pmd7CommandInfo} from "../pmd/PmdCommandInfo";

type DescribeStyledRule = Rule & {
	runWith: string;
	enabled: boolean;
};

/**
 * The Action behind the "rule describe" command
 */
export class RuleDescribeAction implements Action {
	private readonly display: Display;
	private readonly ruleFilterFactory: RuleFilterFactory;

	public constructor(display: Display, ruleFilterFactory: RuleFilterFactory) {
		this.display = display;
		this.ruleFilterFactory = ruleFilterFactory;
	}

	public validateInputs(_inputs: Inputs): Promise<void> { // eslint-disable-line @typescript-eslint/no-unused-vars
		// Currently there is nothing to validate
		return Promise.resolve();
	}

	public async run(inputs: Inputs): Promise<AnyJson> {
		Controller.setActivePmdCommandInfo(inputs['preview-pmd7'] ? new Pmd7CommandInfo() : new Pmd6CommandInfo());
		const ruleFilters: RuleFilter[] = this.ruleFilterFactory.createRuleFilters(inputs);

		// TODO: Inject RuleManager as a dependency to improve testability by removing coupling to runtime implementation
		const ruleManager = await Controller.createRuleManager();

		const rules: DescribeStyledRule[] = await this.styleRules(ruleManager.getRulesMatchingOnlyExplicitCriteria(ruleFilters));
		if (rules.length === 0) {
			// If we couldn't find any rules that fit the criteria, we'll let the user know. We'll use .displayWarning()
			// instead of .displayInfo() so it's immediately obvious.
			this.display.displayWarning(getMessage(BundleName.Describe, 'output.noMatchingRules', [inputs.rulename as string]));
		} else if (rules.length > 1) {
			// If there was more than one matching rule, we'll let the user know, but we'll still output all the rules.
			const msg = getMessage(BundleName.Describe, 'output.multipleMatchingRules', [rules.length.toString(), inputs.rulename as string]);
			this.display.displayWarning(msg);
			rules.forEach((rule, idx) => {
				this.display.displayStyledHeader(`Rule #${idx + 1}`);
				this.displayStyledRule(rule);
			});
		} else {
			// If there's exactly one rule, we don't need to do anything special, and can just log the rule.
			this.displayStyledRule(rules[0]);
		}
		// We need to return something for when the --json flag is used, so we'll just return the list of rules.
		return deepCopy(rules);
	}

	private async styleRules(rules: Rule[]): Promise<DescribeStyledRule[]> {
		// Opting to use .getAllEngines() instead of .getEnabledEngines() so we don't have to futz with the engineOptions param.

		// TODO: Inject this as a dependency (maybe a factory) to improve testability by removing coupling to runtime implementation
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

	private displayStyledRule(rule: DescribeStyledRule): void {
		this.display.displayStyledObject({
			name: rule.name,
			engine: rule.engine,
			runWith: rule.runWith,
			isPilot: rule.isPilot,
			enabled: rule.enabled,
			categories: rule.categories,
			rulesets: rule.rulesets,
			languages: rule.languages,
			description: rule.description,
			message: rule.message
		})
	}
}
