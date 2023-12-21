import {Flags} from '@salesforce/sf-plugins-core';
import {PathlessEngineFilters} from '../../Constants';
import {ScannerRunCommand} from '../../lib/ScannerRunCommand';
import {Config} from "@oclif/core";
import {RunOptionsFactory, RunOptionsFactoryImpl} from "../../lib/RunOptionsFactory";
import {EngineOptionsFactory, RunEngineOptionsFactory} from "../../lib/EngineOptionsFactory";
import {PathResolver, PathResolverImpl} from "../../lib/PathResolver";
import {Bundle, getMessage} from "../../MessageCatalog";
import {InputValidatorFactory, RunCommandInputValidatorFactory} from "../../lib/InputValidator";

export default class Run extends ScannerRunCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static summary = getMessage(Bundle.Run, 'commandSummary');
	public static description = getMessage(Bundle.Run, 'commandDescription');

	public static examples = [
		getMessage(Bundle.Run, 'examples')
	];

	// This defines the flags accepted by this command.
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

	public constructor(argv: string[], config: Config,
					   inputValidatorFactory?: InputValidatorFactory,
	                   pathResolver?: PathResolver,
					   runOptionsFactory?: RunOptionsFactory,
					   engineOptionsFactory?: EngineOptionsFactory) {
		if (typeof inputValidatorFactory === 'undefined') {
			inputValidatorFactory = new RunCommandInputValidatorFactory()
		}
		if (typeof pathResolver === 'undefined') {
			pathResolver = new PathResolverImpl();
		}
		if (typeof runOptionsFactory === 'undefined') {
			runOptionsFactory = new RunOptionsFactoryImpl(false, config.version);
		}
		if (typeof engineOptionsFactory === 'undefined') {
			engineOptionsFactory = new RunEngineOptionsFactory(pathResolver);
		}
		super(argv, config, inputValidatorFactory, pathResolver, runOptionsFactory, engineOptionsFactory);
	}
}
