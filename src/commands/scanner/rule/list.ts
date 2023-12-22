import {Flags} from '@salesforce/sf-plugins-core';
import {Action, ScannerCommand} from '../../../lib/ScannerCommand';
import {AllowedEngineFilters} from '../../../Constants';
import {Bundle, getMessage} from "../../../MessageCatalog";
import {Logger} from "@salesforce/core";
import {Display} from "../../../lib/Display";
import {RuleFilterFactoryImpl} from "../../../lib/RuleFilterFactory";
import {RuleListAction} from "../../../lib/actions/RuleListAction";

/**
 * Defines the "rule list" command for the "scanner" cli.
 */
export default class List extends ScannerCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static summary = getMessage(Bundle.List, 'commandSummary');
	public static description = getMessage(Bundle.List, 'commandDescription');

	public static examples = [
		getMessage(Bundle.List, 'examples')
	];

	// This defines the flags accepted by this command. The key is the longname, the char property is the shortname,
	// and summary and description is what's printed when the -h/--help flag is supplied.
	public static readonly flags = {
		verbose: Flags.boolean({
			summary: getMessage(Bundle.Common, 'flags.verboseSummary')
		}),
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
	};

	protected createAction(_logger: Logger, display: Display): Action {
		return new RuleListAction(display, new RuleFilterFactoryImpl())
	}
}
