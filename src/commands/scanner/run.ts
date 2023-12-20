import {Flags} from '@salesforce/sf-plugins-core';
import {PathlessEngineFilters} from '../../Constants';
import {ScannerRunCommand} from '../../lib/ScannerRunCommand';
import {Config} from "@oclif/core";
import {RuleFilterFactoryImpl} from "../../lib/RuleFilterFactory";
import {RunOptionsFactoryImpl} from "../../lib/RunOptionsFactory";
import {RunCommandInputValidatorFactory} from "../../lib/InputValidatorFactory";
import {RunEngineOptionsFactory} from "../../lib/EngineOptionsFactory";
import {PathFactory, PathFactoryImpl} from "../../lib/PathFactory";
import {BUNDLE, getBundledMessage} from "../../MessageCatalog";

export default class Run extends ScannerRunCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static summary = getBundledMessage(BUNDLE.RUN, 'commandSummary');
	public static description = getBundledMessage(BUNDLE.RUN, 'commandDescription');

	public static examples = [
		getBundledMessage(BUNDLE.RUN, 'examples')
	];

	// This defines the flags accepted by this command.
	public static readonly flags= {
		// Include all common flags from the super class.
		...ScannerRunCommand.flags,
		// BEGIN: Filter-related flags.
		ruleset: Flags.custom<string[]>({
			char: 'r',
			deprecated: {
				message: getBundledMessage(BUNDLE.RUN, 'rulesetDeprecation')
			},
			summary: getBundledMessage(BUNDLE.RUN, 'flags.rulesetSummary'),
			description: getBundledMessage(BUNDLE.RUN, 'flags.rulesetDescription'),
			delimiter: ',',
			multiple: true
		})(),
		engine: Flags.custom<string[]>({
			char: 'e',
			summary: getBundledMessage(BUNDLE.RUN, 'flags.engineSummary'),
			description: getBundledMessage(BUNDLE.RUN, 'flags.engineDescription'),
			options: [...PathlessEngineFilters],
			delimiter: ',',
			multiple: true
		})(),
		// END: Filter-related flags.
		// BEGIN: Targeting-related flags.
		target: Flags.custom<string[]>({
			char: 't',
			summary: getBundledMessage(BUNDLE.RUN, 'flags.targetSummary'),
			description: getBundledMessage(BUNDLE.RUN, 'flags.targetDescription'),
			delimiter: ',',
			multiple: true,
			required: true
		})(),
		// END: Targeting-related flags.
		// BEGIN: Engine config flags.
		tsconfig: Flags.string({
			summary: getBundledMessage(BUNDLE.RUN, 'flags.tsconfigSummary'),
			description: getBundledMessage(BUNDLE.RUN, 'flags.tsconfigDescription')
		}),
		eslintconfig: Flags.string({
			summary: getBundledMessage(BUNDLE.RUN, 'flags.eslintConfigSummary'),
			description: getBundledMessage(BUNDLE.RUN, 'flags.eslintConfigDescription')
		}),
		pmdconfig: Flags.string({
			summary: getBundledMessage(BUNDLE.RUN, 'flags.pmdConfigSummary'),
			description: getBundledMessage(BUNDLE.RUN, 'flags.pmdConfigDescription')
		}),
		// TODO: This flag was implemented for W-7791882, and it's suboptimal. It leaks the abstraction and pollutes the command.
		//   It should be replaced during the 3.0 release cycle.
		env: Flags.string({
			summary: getBundledMessage(BUNDLE.RUN, 'flags.envSummary'),
			description: getBundledMessage(BUNDLE.RUN, 'flags.envDescription'),
			deprecated: {
				message: getBundledMessage(BUNDLE.RUN, 'flags.envParamDeprecationWarning')
			}
		}),
		// END: Engine config flags.
		// BEGIN: Flags related to results processing.
		"verbose-violations": Flags.boolean({
			summary: getBundledMessage(BUNDLE.RUN, 'flags.verboseViolationsSummary'),
			description: getBundledMessage(BUNDLE.RUN, 'flags.verboseViolationsDescription')
		})
		// END: Flags related to results processing.
	};

	public constructor(argv: string[], config: Config) {
		const pathFactory: PathFactory = new PathFactoryImpl();
		super(argv, config,
			new RunCommandInputValidatorFactory(),
			new RuleFilterFactoryImpl(),
			new RunOptionsFactoryImpl(false, config.version),
			pathFactory,
			new RunEngineOptionsFactory(pathFactory));
	}
}
