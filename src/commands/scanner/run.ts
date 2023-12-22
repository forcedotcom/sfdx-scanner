import {Flags} from '@salesforce/sf-plugins-core';
import {PathlessEngineFilters} from '../../Constants';
import {ScannerRunCommand} from '../../lib/ScannerRunCommand';
import {RunOptionsFactory, RunOptionsFactoryImpl} from "../../lib/RunOptionsFactory";
import {EngineOptionsFactory, RunEngineOptionsFactory} from "../../lib/EngineOptionsFactory";
import {InputsResolver, InputsResolverImpl} from "../../lib/InputsResolver";
import {Bundle, getMessage} from "../../MessageCatalog";
import {Logger} from "@salesforce/core";
import {Action} from "../../lib/ScannerCommand";
import {Display} from "../../lib/Display";
import {RunAction} from "../../lib/actions/RunAction";

/**
 * Defines the "run" command for the "scanner" cli.
 */
export default class Run extends ScannerRunCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static summary = getMessage(Bundle.Run, 'commandSummary');
	public static description = getMessage(Bundle.Run, 'commandDescription');
	public static examples = [
		getMessage(Bundle.Run, 'examples')
	];

	// This defines the flags accepted by this command. The key is the longname, the char property is the shortname,
	// and summary and description is what's printed when the -h/--help flag is supplied.
	public static readonly flags= {
		// Include all common flags from the super class.
		...ScannerRunCommand.flags,
		// BEGIN: Filter-related flags.
		ruleset: Flags.custom<string[]>({
			char: 'r',
			deprecated: {
				message: getMessage(Bundle.Run, 'rulesetDeprecation')
			},
			summary: getMessage(Bundle.Run, 'flags.rulesetSummary'),
			description: getMessage(Bundle.Run, 'flags.rulesetDescription'),
			delimiter: ',',
			multiple: true
		})(),
		engine: Flags.custom<string[]>({
			char: 'e',
			summary: getMessage(Bundle.Run, 'flags.engineSummary'),
			description: getMessage(Bundle.Run, 'flags.engineDescription'),
			options: [...PathlessEngineFilters],
			delimiter: ',',
			multiple: true
		})(),
		// END: Filter-related flags.
		// BEGIN: Targeting-related flags.
		target: Flags.custom<string[]>({
			char: 't',
			summary: getMessage(Bundle.Run, 'flags.targetSummary'),
			description: getMessage(Bundle.Run, 'flags.targetDescription'),
			delimiter: ',',
			multiple: true,
			required: true
		})(),
		// END: Targeting-related flags.
		// BEGIN: Engine config flags.
		tsconfig: Flags.string({
			summary: getMessage(Bundle.Run, 'flags.tsconfigSummary'),
			description: getMessage(Bundle.Run, 'flags.tsconfigDescription')
		}),
		eslintconfig: Flags.string({
			summary: getMessage(Bundle.Run, 'flags.eslintConfigSummary'),
			description: getMessage(Bundle.Run, 'flags.eslintConfigDescription')
		}),
		pmdconfig: Flags.string({
			summary: getMessage(Bundle.Run, 'flags.pmdConfigSummary'),
			description: getMessage(Bundle.Run, 'flags.pmdConfigDescription')
		}),
		// TODO: This flag was implemented for W-7791882, and it's suboptimal. It leaks the abstraction and pollutes the command.
		//   It should be replaced during the 3.0 release cycle.
		env: Flags.string({
			summary: getMessage(Bundle.Run, 'flags.envSummary'),
			description: getMessage(Bundle.Run, 'flags.envDescription'),
			deprecated: {
				message: getMessage(Bundle.Run, 'flags.envParamDeprecationWarning')
			}
		}),
		// END: Engine config flags.
		// BEGIN: Flags related to results processing.
		"verbose-violations": Flags.boolean({
			summary: getMessage(Bundle.Run, 'flags.verboseViolationsSummary'),
			description: getMessage(Bundle.Run, 'flags.verboseViolationsDescription')
		})
		// END: Flags related to results processing.
	};

	protected createAction(_logger: Logger, display: Display): Action {
		const inputsResolver: InputsResolver = new InputsResolverImpl()
		const runOptionsFactory: RunOptionsFactory = new RunOptionsFactoryImpl(false, this.config.version);
		const engineOptionsFactory: EngineOptionsFactory = new RunEngineOptionsFactory(inputsResolver);
		return new RunAction(display, inputsResolver, runOptionsFactory, engineOptionsFactory);
	}
}
