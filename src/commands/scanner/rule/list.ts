import {Flags} from '@salesforce/sf-plugins-core';
import {Action, ScannerCommand} from '../../../lib/ScannerCommand';
import {AllowedEngineFilters} from '../../../Constants';
import {BundleName, getMessage} from "../../../MessageCatalog";
import {Logger} from "@salesforce/core";
import {Display} from "../../../lib/Display";
import {RuleFilterFactoryImpl} from "../../../lib/RuleFilterFactory";
import {RuleListAction} from "../../../lib/actions/RuleListAction";

/**
 * Defines the "rule list" command for the "scanner" cli.
 */
export default class List extends ScannerCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static summary = getMessage(BundleName.List, 'commandSummary');
	public static description = getMessage(BundleName.List, 'commandDescription');

	public static examples = [
		getMessage(BundleName.List, 'examples')
	];

	// This defines the flags accepted by this command. The key is the longname, the char property is the shortname,
	// and summary and description is what's printed when the -h/--help flag is supplied.
	public static readonly flags = {
		verbose: Flags.boolean({
			summary: getMessage(BundleName.Common, 'flags.verboseSummary'),
			description: getMessage(BundleName.Common, 'flags.verboseDescription')
		}),
		category: Flags.custom<string[]>({
			char: 'c',
			summary: getMessage(BundleName.List, 'flags.categorySummary'),
			delimiter: ',',
			multiple: true
		})(),
		ruleset: Flags.custom<string[]>({
			char: 'r',
			deprecated: {
				message: getMessage(BundleName.List, 'rulesetDeprecation')
			},
			summary: getMessage(BundleName.List, 'flags.rulesetSummary'),
			delimiter: ',',
			multiple: true
		})(),
		language: Flags.custom<string[]>({
			char: 'l',
			summary: getMessage(BundleName.List, 'flags.languageSummary'),
			description: getMessage(BundleName.List, 'flags.languageDescription'),
			delimiter: ',',
			multiple: true
		})(),
		engine: Flags.custom<string[]>({
			char: 'e',
			summary: getMessage(BundleName.List, 'flags.engineSummary'),
			options: [...AllowedEngineFilters],
			delimiter: ',',
			multiple: true
		})(),
	};

	protected createAction(_logger: Logger, display: Display): Action {
		return new RuleListAction(display, new RuleFilterFactoryImpl())
	}
}
