import {flags} from '@salesforce/command';
import {Messages} from '@salesforce/core';
import {Controller} from '../../../Controller';
import {Rule} from '../../../types';
import {ScannerCommand} from '../../../lib/ScannerCommand';
import {AllowedEngineFilters, PILOT_AVAILABILITY_BANNER} from '../../../Constants';


// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'list');
const columns = [messages.getMessage('columnNames.name'),
				messages.getMessage('columnNames.languages'),
				messages.getMessage('columnNames.categories'),
				messages.getMessage('columnNames.rulesets'),
				messages.getMessage('columnNames.engine')];

export default class List extends ScannerCommand {
	// These determine what's displayed when the --help/-h flag is supplied.
	public static description = messages.getMessage('commandDescription');
	public static longDescription = messages.getMessage('commandDescriptionLong');

	public static examples = [
		messages.getMessage('examples')
	];

	public static args = [{name: 'file'}];

	// This defines the flags accepted by this command. The key is the longname, the char property is the shortname, and description
	// is what's printed when the -h/--help flag is supplied.
	protected static flagsConfig = {
		verbose: flags.builtin(),
		// BEGIN: Flags consumed by ScannerCommand#buildRuleFilters
		// These flags are how you choose which rules are listed.
		category: flags.array({
			char: 'c',
			description: messages.getMessage('flags.categoryDescription'),
			longDescription: messages.getMessage('flags.categoryDescriptionLong')
		}),
		ruleset: flags.array({
			char: 'r',
			deprecated: {
				messageOverride: messages.getMessage('rulesetDeprecation')
			},
			description: messages.getMessage('flags.rulesetDescription'),
			longDescription: messages.getMessage('flags.rulesetDescriptionLong')
		}),
		language: flags.array({
			char: 'l',
			description: messages.getMessage('flags.languageDescription'),
			longDescription: messages.getMessage('flags.languageDescriptionLong')
		}),
		engine: flags.array({
			char: 'e',
			description: messages.getMessage('flags.engineDescription'),
			longDescription: messages.getMessage('flags.engineDescriptionLong'),
			options: [...AllowedEngineFilters]
		}),
		// END: Flags consumed by ScannerCommand#buildRuleFilters
		// TODO: After implementing this flag, unhide it.
		severity: flags.string({
			char: 's',
			description: messages.getMessage('flags.severityDescription'),
			hidden: true
		}),
		// TODO: After implementing this flag, unhide it.
		standard: flags.boolean({
			description: messages.getMessage('flags.standardDescription'),
			exclusive: ['custom'],
			hidden: true
		}),
		// TODO: After implementing this flag, unhide it.
		custom: flags.boolean({
			description: messages.getMessage('flags.customDescription'),
			exclusive: ['standard'],
			hidden: true
		})
	};

	public async run(): Promise<Rule[]> {
		this.ux.warn(PILOT_AVAILABILITY_BANNER);
		const ruleFilters = this.buildRuleFilters();
		// It's possible for this line to throw an error, but that's fine because the error will be an SfdxError that we can
		// allow to boil over.
		const ruleManager = await Controller.createRuleManager();
		const rules = await ruleManager.getRulesMatchingCriteria(ruleFilters);
		const formattedRules = this.formatRulesForDisplay(rules);
		this.ux.table(formattedRules, columns);
		// If the --json flag was used, we need to return a JSON. Since we don't have to worry about displayability, we can
		// just return the filtered list instead of the formatted list.
		return rules;
	}

	/* eslint-disable @typescript-eslint/no-explicit-any */
	private formatRulesForDisplay(rules: Rule[]): Record<string, any>[] {
		// Truncate ruleset values
		const rulesetTruncatedRules = this.truncateRulesetValues(rules);

		// Transform column names to match display
		const transformedRules = [];
		rulesetTruncatedRules.forEach(rule => transformedRules.push(this.transformKeysToMatchColumns(rule)));

		return transformedRules;
	}

	/* eslint-disable @typescript-eslint/no-explicit-any */
	private transformKeysToMatchColumns(rule: Rule): Record<string, any> {
		// Map rule fields to the matching column
		const transformedRule = {};
		transformedRule[columns[0]] = rule.name;
		transformedRule[columns[1]] = rule.languages;
		transformedRule[columns[2]] = rule.categories;
		transformedRule[columns[3]] = rule.rulesets;
		transformedRule[columns[4]] = rule.engine;
		return transformedRule;
	}

	private truncateRulesetValues(rules: Rule[]): Rule[] {
		return rules.map(rule => {
			const clonedRule = JSON.parse(JSON.stringify(rule));

			// If any of the rule's rulesets have a name longer than 20 characters, we'll truncate it to 15 and append ellipses,
			// so it doesn't overflow horizontally.
			clonedRule.rulesets = rule['rulesets'].map(ruleset =>
				ruleset.length >= 20 ? ruleset.slice(0, 15) + '...' : ruleset);
			return clonedRule;
		});
	}
}
