import {Flags, Ux} from '@salesforce/sf-plugins-core';
import {Controller} from '../../../Controller';
import {Inputs, Rule} from '../../../types';
import {ScannerCommand} from '../../../lib/ScannerCommand';
import {AllowedEngineFilters} from '../../../Constants';
import {RuleFilterFactory, RuleFilterFactoryImpl} from "../../../lib/RuleFilterFactory";
import {RuleFilter} from "../../../lib/RuleFilter";
import {Bundle, getMessage} from "../../../MessageCatalog";
import {Config} from "@oclif/core";

import {InputValidatorFactory, NoOpInputValidatorFactory} from "../../../lib/InputValidator";

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
const MSG_YES = getMessage(Bundle.List, 'yes');
const MSG_NO = getMessage(Bundle.List, 'no');

export default class List extends ScannerCommand {
	// These determine what's displayed when the --help/-h flag is supplied.
	public static summary = getMessage(Bundle.List, 'commandSummary');
	public static description = getMessage(Bundle.List, 'commandDescription');

	public static examples = [
		getMessage(Bundle.List, 'examples')
	];

	// This defines the flags accepted by this command. The key is the longname, the char property is the shortname, and description
	// is what's printed when the -h/--help flag is supplied.
	public static readonly flags = {
		verbose: Flags.boolean({
			summary: getMessage(Bundle.Common, 'flags.verboseSummary')
		}),
		// BEGIN: Flags consumed by ScannerCommand#buildRuleFilters
		// These flags are how you choose which rules are listed.
		category: Flags.custom<string[]>({
			char: 'c',
			summary: getMessage(Bundle.List, 'flags.categorySummary'),
			description: getMessage(Bundle.List, 'flags.categoryDescription'),
			delimiter: ',',
			multiple: true
		})(),
		ruleset: Flags.custom<string[]>({
			char: 'r',
			deprecated: {
				message: getMessage(Bundle.List, 'rulesetDeprecation')
			},
			summary: getMessage(Bundle.List, 'flags.rulesetSummary'),
			description: getMessage(Bundle.List, 'flags.rulesetDescription'),
			delimiter: ',',
			multiple: true
		})(),
		language: Flags.custom<string[]>({
			char: 'l',
			summary: getMessage(Bundle.List, 'flags.languageSummary'),
			description: getMessage(Bundle.List, 'flags.languageDescription'),
			delimiter: ',',
			multiple: true
		})(),
		engine: Flags.custom<string[]>({
			char: 'e',
			summary: getMessage(Bundle.List, 'flags.engineSummary'),
			description: getMessage(Bundle.List, 'flags.engineDescription'),
			options: [...AllowedEngineFilters],
			delimiter: ',',
			multiple: true
		})()
		// END: Flags consumed by ScannerCommand#buildRuleFilters
	};

	private readonly ruleFilterFactory: RuleFilterFactory;

	public constructor(argv: string[], config: Config,
					   inputValidatorFactory?: InputValidatorFactory,
					   ruleFilterFactory?: RuleFilterFactory) {
		if (typeof inputValidatorFactory === 'undefined') {
			inputValidatorFactory = new NoOpInputValidatorFactory();
		}
		if (typeof ruleFilterFactory === 'undefined') {
			ruleFilterFactory = new RuleFilterFactoryImpl();
		}
		super(argv, config, inputValidatorFactory);
		this.ruleFilterFactory = ruleFilterFactory;
	}

	async runInternal(inputs: Inputs): Promise<Rule[]> {
		const ruleFilters: RuleFilter[] = this.ruleFilterFactory.createRuleFilters(inputs);

		// It's possible for this line to throw an error, but that's fine because the error will be an SfError that we can
		// allow to boil over.
		const ruleManager = await Controller.createRuleManager();
		const rules = await ruleManager.getRulesMatchingCriteria(ruleFilters);
		this.table(rules, columns);
		// If the --json flag was used, we need to return a JSON. Since we don't have to worry about displayability, we can
		// just return the filtered list instead of the formatted list.
		return rules;
	}
}
