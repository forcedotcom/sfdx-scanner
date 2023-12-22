import {Flags} from '@salesforce/sf-plugins-core';
import {Action, ScannerCommand} from '../../../lib/ScannerCommand';
import {RuleFilterFactoryImpl} from "../../../lib/RuleFilterFactory";
import {Bundle, getMessage} from "../../../MessageCatalog";
import {Logger} from "@salesforce/core";
import {Display} from "../../../lib/Display";
import {RuleDescribeAction} from "../../../lib/actions/RuleDescribeAction";

/**
 * Defines the "rule describe" command for the "scanner" cli.
 */
export default class Describe extends ScannerCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static summary = getMessage(Bundle.Describe, 'commandSummary');
	public static description = getMessage(Bundle.Describe, 'commandDescription');
	public static examples = [
		getMessage(Bundle.Describe, 'examples.normalExample')
	];

	// This defines the flags accepted by this command. The key is the longname, the char property is the shortname,
	// and summary and description is what's printed when the -h/--help flag is supplied.
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

	protected createAction(_logger: Logger, display: Display): Action {
		return new RuleDescribeAction(display, new RuleFilterFactoryImpl())
	}
}
