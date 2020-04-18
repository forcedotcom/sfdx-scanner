import {flags} from '@salesforce/command';
import {Messages} from '@salesforce/core';
import {AnyJson} from '@salesforce/ts-types';
import {Controller} from '../../../ioc.config';
import {Rule} from '../../../types';
import {ScannerCommand} from '../scannerCommand';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'describe');

export default class Describe extends ScannerCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static description = messages.getMessage('commandDescription');
	public static longDescription = messages.getMessage('commandDescriptionLong');

	public static examples = [
		messages.getMessage('examples.normalExample')
	];

	public static args = [{name: 'file'}];

	// This defines the flags accepted by this command. The key is the longname, the char property is the shortname, and description
	// is what's printed when the -h/--help flag is supplied.
	protected static flagsConfig = {
		rulename: flags.string({
			char: 'n',
			description: messages.getMessage('flags.rulenameDescription'),
			longDescription: messages.getMessage('flags.rulenameDescriptionLong'),
			required: true
		}),
		verbose: flags.builtin()
	};

	public async run(): Promise<AnyJson> {
		const ruleFilters = this.buildRuleFilters();
		// It's possible for this line to throw an error, but that's fine because the error will be an SfdxError that we can
		// allow to boil over.
		const ruleManager = await Controller.createManager();
		const rules = await ruleManager.getRulesMatchingCriteria(ruleFilters);
		if (rules.length === 0) {
			// If we couldn't find any rules that fit the criteria, we'll let the user know. We'll use .warn() instead of .log()
			// so it's immediately obvious.
			this.ux.warn(messages.getMessage('output.noMatchingRules').replace('{0}', this.flags.rulename));
		} else if (rules.length > 1) {
			// If there was more than one matching rule, we'll let the user know, but we'll still output all the rules.
			const msg = messages.getMessage('output.multipleMatchingRules')
				.replace('{0}', rules.length.toString())
				.replace('{1}', this.flags.rulename);
			this.ux.warn(msg);
			rules.forEach((rule, idx) => {
				this.ux.styledHeader('Rule #' + (idx + 1));
				this.logStyledRule(rule);
			});
		} else {
			this.logStyledRule(rules[0]);
		}
		// We need to return something for when the --json flag is used, so we'll just return the list of rules.
		return JSON.parse(JSON.stringify(rules));
	}

	private static formatRuleForDescribe(rule: Rule): object {
		// Strip any whitespace off of the description.
		rule.description = rule.description.trim();
		return rule;
	}

	private logStyledRule(rule: Rule): void {
		this.ux.styledObject(Describe.formatRuleForDescribe(rule), ['name', 'categories', 'rulesets', 'languages', 'description', 'message']);
	}
}
