import {Action} from "../ScannerCommand";
import {Inputs, Rule} from "../../types";
import {AnyJson} from "@salesforce/ts-types";
import {Ux} from "@salesforce/sf-plugins-core";
import {Bundle, getMessage} from "../../MessageCatalog";
import {RuleFilter} from "../RuleFilter";
import {Controller} from "../../Controller";
import {Display} from "../Display";
import {RuleFilterFactory} from "../RuleFilterFactory";

const MSG_YES: string = getMessage(Bundle.List, 'yes');
const MSG_NO: string = getMessage(Bundle.List, 'no');
const columns: Ux.Table.Columns<Rule> = {
	name: {
		header: getMessage(Bundle.List, 'columnNames.name')
	},
	languages: {
		header: getMessage(Bundle.List, 'columnNames.languages'),
		get: (rule: Rule): string => rule.languages.join(',')
	},
	categories: {
		header: getMessage(Bundle.List, 'columnNames.categories'),
		get: (rule: Rule): string => rule.categories.join(',')
	},
	rulesets: {
		header: getMessage(Bundle.List, 'columnNames.rulesets'),
		get: (rule: Rule): string => rule.rulesets
			.map(ruleset => ruleset.length >= 20 ? ruleset.slice(0, 15) + '...' : ruleset)
			.join(',')
	},
	engine: {
		header: getMessage(Bundle.List, 'columnNames.engine')
	},
	isDfa: {
		header: getMessage(Bundle.List, 'columnNames.is-dfa'),
		get: (rule: Rule): string => rule.isDfa ? MSG_YES : MSG_NO
	},
	isPilot: {
		header: getMessage(Bundle.List, 'columnNames.is-pilot'),
		get: (rule: Rule): string => rule.isPilot ? MSG_YES : MSG_NO
	}
};

export class RuleListAction implements Action {
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
		const ruleFilters: RuleFilter[] = this.ruleFilterFactory.createRuleFilters(inputs);

		// It's possible for this line to throw an error, but that's fine because the error will be an SfError that we can
		// allow to boil over.
		const ruleManager = await Controller.createRuleManager();
		const rules = await ruleManager.getRulesMatchingCriteria(ruleFilters);
		this.display.displayTable(rules, columns);
		// If the --json flag was used, we need to return a JSON. Since we don't have to worry about displayability, we can
		// just return the filtered list instead of the formatted list.
		return rules;
	}
}
