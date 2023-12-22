import {Flags} from '@salesforce/sf-plugins-core';
import {Action, ScannerCommand} from '../../../lib/ScannerCommand';
import {Bundle, getMessage} from "../../../MessageCatalog";
import {Logger} from "@salesforce/core";
import {Display} from "../../../lib/Display";
import {RuleRemoveAction} from "../../../lib/actions/RuleRemoveAction";
import {PathResolverImpl} from "../../../lib/PathResolver";

/**
 * Defines the "rule remove" command for the "scanner" cli.
 */
export default class Remove extends ScannerCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static summary = getMessage(Bundle.Remove, 'commandSummary');
	public static description = getMessage(Bundle.Remove, 'commandDescription');

	public static examples = [
		getMessage(Bundle.Remove, 'examples')
	];

	// This defines the flags accepted by this command. The key is the longname, the char property is the shortname,
	// and summary and description is what's printed when the -h/--help flag is supplied.
	public static readonly flags = {
		verbose: Flags.boolean({
			summary: getMessage(Bundle.Common, 'flags.verboseSummary')
		}),
		force: Flags.boolean({
			char: 'f',
			summary: getMessage(Bundle.Remove, 'flags.forceSummary'),
			description: getMessage(Bundle.Remove, 'flags.forceDescription')
		}),
		path: Flags.custom<string[]>({
			char: 'p',
			summary: getMessage(Bundle.Remove, 'flags.pathSummary'),
			description: getMessage(Bundle.Remove, 'flags.pathDescription'),
			delimiter: ',',
			multiple: true
		})()
	};

	protected createAction(logger: Logger, display: Display): Action {
		return new RuleRemoveAction(logger, display, new PathResolverImpl());
	}
}
