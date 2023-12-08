import {Flags, Ux} from '@salesforce/sf-plugins-core';
import {Messages} from '@salesforce/core';
import {Controller} from '../../../Controller';
import {Rule} from '../../../types';
import {ScannerCommand} from '../../../lib/ScannerCommand';
import {AllowedEngineFilters} from '../../../Constants';


// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'list');
const commonMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'common');

const columns: Ux.Table.Columns<Rule> = {
	name: {
		header: messages.getMessage('columnNames.name')
	},
	languages: {
		header: messages.getMessage('columnNames.languages'),
		get: (rule: Rule): string => rule.languages.join(',')
	},
	categories: {
		header: messages.getMessage('columnNames.categories'),
		get: (rule: Rule): string => rule.categories.join(',')
	},
	rulesets: {
		header: messages.getMessage('columnNames.rulesets'),
		get: (rule: Rule): string => rule.rulesets
			.map(ruleset => ruleset.length >= 20 ? ruleset.slice(0, 15) + '...' : ruleset)
			.join(',')
	},
	engine: {
		header: messages.getMessage('columnNames.engine')
	},
	isDfa: {
		header: messages.getMessage('columnNames.is-dfa'),
		get: (rule: Rule): string => rule.isDfa ? MSG_YES : MSG_NO
	},
	isPilot: {
		header: messages.getMessage('columnNames.is-pilot'),
		get: (rule: Rule): string => rule.isPilot ? MSG_YES : MSG_NO
	}
};
const MSG_YES = messages.getMessage('yes');
const MSG_NO = messages.getMessage('no');

export default class List extends ScannerCommand {
	// These determine what's displayed when the --help/-h flag is supplied.
	public static summary = messages.getMessage('commandSummary');
	public static description = messages.getMessage('commandDescription');

	public static examples = [
		messages.getMessage('examples')
	];

	// This defines the flags accepted by this command. The key is the longname, the char property is the shortname, and description
	// is what's printed when the -h/--help flag is supplied.
	public static readonly flags = {
		verbose: Flags.boolean({
			summary: commonMessages.getMessage('flags.verboseSummary')
		}),
		// BEGIN: Flags consumed by ScannerCommand#buildRuleFilters
		// These flags are how you choose which rules are listed.
		category: Flags.custom<string[]>({
			char: 'c',
			summary: messages.getMessage('flags.categorySummary'),
			description: messages.getMessage('flags.categoryDescription'),
			delimiter: ',',
			multiple: true
		})(),
		ruleset: Flags.custom<string[]>({
			char: 'r',
			deprecated: {
				message: messages.getMessage('rulesetDeprecation')
			},
			summary: messages.getMessage('flags.rulesetSummary'),
			description: messages.getMessage('flags.rulesetDescription'),
			delimiter: ',',
			multiple: true
		})(),
		language: Flags.custom<string[]>({
			char: 'l',
			summary: messages.getMessage('flags.languageSummary'),
			description: messages.getMessage('flags.languageDescription'),
			delimiter: ',',
			multiple: true
		})(),
		engine: Flags.custom<string[]>({
			char: 'e',
			summary: messages.getMessage('flags.engineSummary'),
			description: messages.getMessage('flags.engineDescription'),
			options: [...AllowedEngineFilters],
			delimiter: ',',
			multiple: true
		})()
		// END: Flags consumed by ScannerCommand#buildRuleFilters
	};

	async runInternal(): Promise<Rule[]> {
		const ruleFilters = this.buildRuleFilters();
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
