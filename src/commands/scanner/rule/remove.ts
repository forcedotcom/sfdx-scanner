import {Flags} from '@salesforce/sf-plugins-core';
import {Action, ScannerCommand} from '../../../lib/ScannerCommand';
import {BundleName, getMessage} from "../../../MessageCatalog";
import {Logger} from "@salesforce/core";
import {Display} from "../../../lib/Display";
import {RuleRemoveAction} from "../../../lib/actions/RuleRemoveAction";
import {InputProcessorImpl} from "../../../lib/InputProcessor";

/**
 * Defines the "rule remove" command for the "scanner" cli.
 */
export default class Remove extends ScannerCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static summary = getMessage(BundleName.Remove, 'commandSummary');
	public static description = getMessage(BundleName.Remove, 'commandDescription');

	public static examples = [
		getMessage(BundleName.Remove, 'examples')
	];

	// This defines the flags accepted by this command. The key is the longname, the char property is the shortname,
	// and summary and description is what's printed when the -h/--help flag is supplied.
	public static readonly flags = {
		verbose: Flags.boolean({
			summary: getMessage(BundleName.Common, 'flags.verboseSummary'),
			description: getMessage(BundleName.Common, 'flags.verboseDescription')
		}),
		force: Flags.boolean({
			char: 'f',
			summary: getMessage(BundleName.Remove, 'flags.forceSummary')
		}),
		path: Flags.custom<string[]>({
			char: 'p',
			summary: getMessage(BundleName.Remove, 'flags.pathSummary'),
			description: getMessage(BundleName.Remove, 'flags.pathDescription'),
			delimiter: ',',
			multiple: true
		})()
	};

	protected createAction(logger: Logger, display: Display): Action {
		return new RuleRemoveAction(logger, display, new InputProcessorImpl(this.config.version, display));
	}
}
