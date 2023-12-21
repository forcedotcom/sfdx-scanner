import {Flags, Ux} from '@salesforce/sf-plugins-core';
import {AnyJson} from '@salesforce/ts-types';
import {Controller} from '../../../Controller';
import {Inputs, Rule} from '../../../types';
import {ScannerCommand} from '../../../lib/ScannerCommand';
import {deepCopy} from '../../../lib/util/Utils';
import Run from '../run';
import Dfa from '../run/dfa';
import {RuleFilterFactory, RuleFilterFactoryImpl} from "../../../lib/RuleFilterFactory";
import {RuleFilter} from "../../../lib/RuleFilter";
import {Bundle, getMessage} from "../../../MessageCatalog";

type DescribeStyledRule = Rule & {
	runWith: string;
	enabled: boolean;
};

export default class Describe extends ScannerCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static summary = getMessage(Bundle.Describe, 'commandSummary');
	public static description = getMessage(Bundle.Describe, 'commandDescription');

	public static examples = [
		getMessage(Bundle.Describe, 'examples.normalExample')
	];

	// This defines the flags accepted by this command. The key is the longname, the char property is the shortname, and description
	// is what's printed when the -h/--help flag is supplied.
	public static readonly flags = {
		rulename: Flags.string({
			char: 'n',
			summary: getMessage(Bundle.Describe, 'flags.rulenameSummary'),
			description: getMessage(Bundle.Describe, 'flags.rulenameDescription'),
			required: true
		}),
		verbose: Flags.boolean({
			summary: getMessage(Bundle.Common, 'flags.verboseSummary')
		})
	};

	async runInternal(inputs: Inputs): Promise<AnyJson> {
		const ruleFilterFactory: RuleFilterFactory = new RuleFilterFactoryImpl();
		const ruleFilters: RuleFilter[] = ruleFilterFactory.createRuleFilters(inputs);

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
				this.styledHeader(`Rule #${idx + 1}`);
				this.logStyledRule(rule);
			});
		} else {
			// If there's exactly one rule, we don't need to do anything special, and can just log the rule.
			this.logStyledRule(rules[0]);
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

	private logStyledRule(rule: DescribeStyledRule): void {
		new Ux({jsonEnabled: this.jsonEnabled()})
			.styledObject(rule, ['name', 'engine', 'runWith', 'isPilot', 'enabled', 'categories', 'rulesets', 'languages', 'description', 'message']);
	}
}
