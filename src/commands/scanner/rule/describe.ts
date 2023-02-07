import {flags} from '@salesforce/command';
import {Messages} from '@salesforce/core';
import {AnyJson} from '@salesforce/ts-types';
import {Controller} from '../../../Controller';
import {Rule} from '../../../types';
import {ScannerCommand} from '../../../lib/ScannerCommand';
import {deepCopy} from '../../../lib/util/Utils';
import Run from '../run';
import Dfa from '../run/dfa';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'describe');

type DescribeStyledRule = Rule & {
	runWith: string;
	enabled: boolean;
};

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

	async runInternal(): Promise<AnyJson> {
		const ruleFilters = this.buildRuleFilters();
		// It's possible for this line to throw an error, but that's fine because the error will be an SfdxError that we can
		// allow to boil over.
		const ruleManager = await Controller.createRuleManager();
		const rules: DescribeStyledRule[] = await this.styleRules(ruleManager.getRulesMatchingOnlyExplicitCriteria(ruleFilters));
		if (rules.length === 0) {
			// If we couldn't find any rules that fit the criteria, we'll let the user know. We'll use .warn() instead of .log()
			// so it's immediately obvious.
			this.ux.warn(messages.getMessage('output.noMatchingRules', [this.flags.rulename as string]));
		} else if (rules.length > 1) {
			// If there was more than one matching rule, we'll let the user know, but we'll still output all the rules.
			const msg = messages.getMessage('output.multipleMatchingRules', [rules.length.toString(), this.flags.rulename as string]);
			this.ux.warn(msg);
			rules.forEach((rule, idx) => {
				this.ux.styledHeader(`Rule #${idx + 1}`);
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
				enabled: enabledEngineNames.has(r.engine)
			};
			// Strip any whitespace off of the description.
			styledRule.description = styledRule.description.trim();
			return styledRule;
		});
	}

	private logStyledRule(rule: DescribeStyledRule): void {
		this.ux.styledObject(rule, ['name', 'engine', 'runWith', 'enabled', 'categories', 'rulesets', 'languages', 'description', 'message']);
	}
}
