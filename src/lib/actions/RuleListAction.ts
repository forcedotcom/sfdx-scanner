import {Action} from "../ScannerCommand";
import {Inputs, Rule} from "../../types";
import {AnyJson} from "@salesforce/ts-types";
import {Ux} from "@salesforce/sf-plugins-core";
import {Bundle, getMessage} from "../../MessageCatalog";
import {RuleFilter} from "../RuleFilter";
import {Controller} from "../../Controller";
import {Display} from "../Display";
import {RuleFilterFactory} from "../RuleFilterFactory";

/**
 * The Action behind the "rule list" command
 */
export class RuleListAction implements Action {
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
		const ruleFilters: RuleFilter[] = this.ruleFilterFactory.createRuleFilters(inputs);

		// TODO: Inject RuleManager as a dependency to improve testability by removing coupling to runtime implementation
		const ruleManager = await Controller.createRuleManager();

		const rules = await ruleManager.getRulesMatchingCriteria(ruleFilters);
		this.display.displayTable(rules, columns);
		// If the --json flag was used, we need to return a JSON. Since we don't have to worry about displayability, we can
		// just return the filtered list instead of the formatted list.
		return rules;
	}
}

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
