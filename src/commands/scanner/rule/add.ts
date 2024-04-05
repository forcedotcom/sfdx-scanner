import {Flags} from '@salesforce/sf-plugins-core';
import {Action, ScannerCommand} from '../../../lib/ScannerCommand';
import {BundleName, getMessage} from "../../../MessageCatalog";
import {InputProcessorImpl} from "../../../lib/InputProcessor";
import {Display} from "../../../lib/Display";
import {RuleAddAction} from "../../../lib/actions/RuleAddAction";
import {Logger} from "@salesforce/core";

/**
 * Defines the "rule add" command for the "scanner" cli.
 */
export default class Add extends ScannerCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static summary = getMessage(BundleName.Add, 'commandSummary');
	public static description = getMessage(BundleName.Add, 'commandDescription');
	public static examples = [
		getMessage(BundleName.Add, 'examples')
	];

	// This defines the flags accepted by this command. The key is the longname, the char property is the shortname,
	// and summary and description is what's printed when the -h/--help flag is supplied.
	public static readonly flags = {
		language: Flags.string({
			char: 'l',
			summary: getMessage(BundleName.Add, 'flags.languageSummary'),
			required: true
		}),
		path: Flags.custom<string[]>({
			char: 'p',
			summary: getMessage(BundleName.Add, 'flags.pathSummary'),
			description: getMessage(BundleName.Add, 'flags.pathDescription'),
			multiple: true,
			delimiter: ',',
			required: true
		})()
	};

	protected createAction(logger: Logger, display: Display): Action {
		return new RuleAddAction(logger, display, new InputProcessorImpl(this.config.version, display));
	}
}
