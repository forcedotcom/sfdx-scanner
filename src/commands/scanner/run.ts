import {Flags} from '@salesforce/sf-plugins-core';
import {PathlessEngineFilters, PMD6_VERSION, PMD7_VERSION} from '../../Constants';
import {ScannerRunCommand} from '../../lib/ScannerRunCommand';
import {EngineOptionsFactory, RunEngineOptionsFactory} from "../../lib/EngineOptionsFactory";
import {InputProcessor, InputProcessorImpl} from "../../lib/InputProcessor";
import {BundleName, getMessage} from "../../MessageCatalog";
import {Logger} from "@salesforce/core";
import {Action} from "../../lib/ScannerCommand";
import {Display} from "../../lib/Display";
import {RunAction} from "../../lib/actions/RunAction";
import {RuleFilterFactory, RuleFilterFactoryImpl} from "../../lib/RuleFilterFactory";
import {ResultsProcessorFactory, ResultsProcessorFactoryImpl} from "../../lib/output/ResultsProcessorFactory";

/**
 * Defines the "run" command for the "scanner" cli.
 */
export default class Run extends ScannerRunCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static summary = getMessage(BundleName.Run, 'commandSummary');
	public static description = getMessage(BundleName.Run, 'commandDescription');
	public static examples = [
		getMessage(BundleName.Run, 'examples')
	];
	public static readonly invocation = 'scanner run';

	// This defines the flags accepted by this command. The key is the longname, the char property is the shortname,
	// and summary and description is what's printed when the -h/--help flag is supplied.
	public static readonly flags= {
		// Include all common flags from the super class.
		...ScannerRunCommand.flags,
		// BEGIN: Filter-related flags.
		ruleset: Flags.custom<string[]>({
			char: 'r',
			deprecated: {
				message: getMessage(BundleName.Run, 'rulesetDeprecation')
			},
			summary: getMessage(BundleName.Run, 'flags.rulesetSummary'),
			description: getMessage(BundleName.Run, 'flags.rulesetDescription'),
			delimiter: ',',
			multiple: true
		})(),
		engine: Flags.custom<string[]>({
			char: 'e',
			summary: getMessage(BundleName.Run, 'flags.engineSummary'),
			description: getMessage(BundleName.Run, 'flags.engineDescription'),
			options: [...PathlessEngineFilters],
			delimiter: ',',
			multiple: true
		})(),
		// END: Filter-related flags.
		// BEGIN: Targeting-related flags.
		target: Flags.custom<string[]>({
			char: 't',
			summary: getMessage(BundleName.Run, 'flags.targetSummary'),
			description: getMessage(BundleName.Run, 'flags.targetDescription'),
			delimiter: ',',
			multiple: true
		})(),
		// END: Targeting-related flags.
		// BEGIN: Engine config flags.
		tsconfig: Flags.string({
			summary: getMessage(BundleName.Run, 'flags.tsconfigSummary'),
			description: getMessage(BundleName.Run, 'flags.tsconfigDescription')
		}),
		eslintconfig: Flags.string({
			summary: getMessage(BundleName.Run, 'flags.eslintConfigSummary'),
			description: getMessage(BundleName.Run, 'flags.eslintConfigDescription')
		}),
		pmdconfig: Flags.string({
			summary: getMessage(BundleName.Run, 'flags.pmdConfigSummary'),
			description: getMessage(BundleName.Run, 'flags.pmdConfigDescription')
		}),
		"preview-pmd7": Flags.boolean({
			summary: getMessage(BundleName.Run, 'flags.previewPmd7Summary', [PMD7_VERSION]),
			description: getMessage(BundleName.Run, 'flags.previewPmd7Description', [PMD7_VERSION, PMD6_VERSION])
		}),

		// TODO: This flag was implemented for W-7791882, and it's suboptimal. It leaks the abstraction and pollutes the command.
		//   It should be replaced during the 3.0 release cycle.
		env: Flags.string({
			summary: getMessage(BundleName.Run, 'flags.envSummary'),
			description: getMessage(BundleName.Run, 'flags.envDescription'),
			deprecated: {
				message: getMessage(BundleName.Run, 'flags.envParamDeprecationWarning')
			}
		}),
		// END: Engine config flags.
		// BEGIN: Flags related to results processing.
		"verbose-violations": Flags.boolean({
			summary: getMessage(BundleName.Run, 'flags.verboseViolationsSummary'),
			description: getMessage(BundleName.Run, 'flags.verboseViolationsDescription')
		})
		// END: Flags related to results processing.
	};

	protected createAction(logger: Logger, display: Display): Action {
		const inputProcessor: InputProcessor = new InputProcessorImpl(this.config.version, display);
		const ruleFilterFactory: RuleFilterFactory = new RuleFilterFactoryImpl();
		const engineOptionsFactory: EngineOptionsFactory = new RunEngineOptionsFactory(inputProcessor);
		const resultsProcessorFactory: ResultsProcessorFactory = new ResultsProcessorFactoryImpl();
		return new RunAction(logger, display, inputProcessor, ruleFilterFactory, engineOptionsFactory,
			resultsProcessorFactory);
	}
}
