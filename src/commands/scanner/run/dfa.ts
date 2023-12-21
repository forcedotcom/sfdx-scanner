import {Flags} from '@salesforce/sf-plugins-core';
import {ScannerRunCommand} from '../../../lib/ScannerRunCommand';
import {Config} from "@oclif/core";
import {RunOptionsFactory, RunOptionsFactoryImpl} from "../../../lib/RunOptionsFactory";
import {PathResolver, PathResolverImpl} from "../../../lib/PathResolver";
import {EngineOptionsFactory, RunDfaEngineOptionsFactory} from "../../../lib/EngineOptionsFactory";
import {Bundle, getMessage} from "../../../MessageCatalog";
import {InputValidatorFactory, RunDfaCommandInputValidatorFactory} from "../../../lib/InputValidator";

export default class Dfa extends ScannerRunCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static summary = getMessage(Bundle.RunDfa, 'commandSummary');
	public static description = getMessage(Bundle.RunDfa, 'commandDescription');

	public static examples = [
		getMessage(Bundle.RunDfa, 'examples')
	];

	// This defines the flags accepted by this command.
	// NOTE: Unlike the other classes that extend ScannerCommand, this class has no flags for specifying rules. This is
	// because the command currently supports only a single engine with a single rule. So no such flags are currently
	// needed. If, at some point, we add additional rules or engines to this command, those flags will need to be added.
	public static readonly flags = {
		// Include all common flags from the super class.
		...ScannerRunCommand.flags,
		// BEGIN: Filter-related flags.
		'with-pilot': Flags.boolean({
			summary: getMessage(Bundle.RunDfa, 'flags.withpilotSummary'),
			description: getMessage(Bundle.RunDfa, 'flags.withpilotDescription')
		}),
		// END: Filter-related flags.
		// BEGIN: Flags for targeting files.
		// NOTE: All run commands have a `--target` flag, but they have differing functionalities,
		// and therefore different descriptions, so each command defines this flag separately.
		target: Flags.custom<string[]>({
			char: 't',
			summary: getMessage(Bundle.RunDfa, 'flags.targetSummary'),
			description: getMessage(Bundle.RunDfa, 'flags.targetDescription'),
			required: true,
			delimiter: ',',
			multiple: true
		})(),
		// END: Flags for targeting files.
		// BEGIN: Config-overrideable engine flags.
		'rule-thread-count': Flags.integer({
			summary: getMessage(Bundle.RunDfa, 'flags.rulethreadcountSummary'),
			description: getMessage(Bundle.RunDfa, 'flags.rulethreadcountDescription'),
			env: 'SFGE_RULE_THREAD_COUNT'
		}),
		'rule-thread-timeout': Flags.integer({
			summary: getMessage(Bundle.RunDfa, 'flags.rulethreadtimeoutSummary'),
			description: getMessage(Bundle.RunDfa, 'flags.rulethreadtimeoutDescription'),
			env: 'SFGE_RULE_THREAD_TIMEOUT'
		}),
		// NOTE: This flag can't use the `env` property to inherit a value automatically, because OCLIF boolean flags
		// don't support that. Instead, we check the env-var manually in a subsequent method.
		'rule-disable-warning-violation': Flags.boolean({
			summary: getMessage(Bundle.RunDfa, 'flags.ruledisablewarningviolationSummary'),
			description: getMessage(Bundle.RunDfa, 'flags.ruledisablewarningviolationDescription')
		}),
		'sfgejvmargs': Flags.string({
			summary: getMessage(Bundle.RunDfa, 'flags.sfgejvmargsSummary'),
			description: getMessage(Bundle.RunDfa, 'flags.sfgejvmargsDescription'),
			env: 'SFGE_JVM_ARGS'
		}),
		'pathexplimit': Flags.integer({
			summary: getMessage(Bundle.RunDfa, 'flags.pathexplimitSummary'),
			description: getMessage(Bundle.RunDfa, 'flags.pathexplimitDescription'),
			env: 'SFGE_PATH_EXPANSION_LIMIT'
		})
		// END: Config-overrideable engine flags.
	};

	public constructor(argv: string[], config: Config,
					   inputValidatorFactory?: InputValidatorFactory,
					   pathResolver?: PathResolver,
					   runOptionsFactory?: RunOptionsFactory,
					   engineOptionsFactory?: EngineOptionsFactory) {
		if (typeof inputValidatorFactory === 'undefined') {
			inputValidatorFactory = new RunDfaCommandInputValidatorFactory();
		}
		if (typeof pathResolver === 'undefined') {
			pathResolver = new PathResolverImpl();
		}
		if (typeof runOptionsFactory === 'undefined') {
			runOptionsFactory = new RunOptionsFactoryImpl(true, config.version);
		}
		if (typeof engineOptionsFactory === 'undefined') {
			engineOptionsFactory = new RunDfaEngineOptionsFactory(pathResolver);
		}
		super(argv, config, inputValidatorFactory, pathResolver, runOptionsFactory, engineOptionsFactory);
	}
}
