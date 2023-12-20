import {Flags} from '@salesforce/sf-plugins-core';
import {ScannerRunCommand} from '../../../lib/ScannerRunCommand';
import {Config} from "@oclif/core";
import {RuleFilterFactoryImpl} from "../../../lib/RuleFilterFactory";
import {RunOptionsFactoryImpl} from "../../../lib/RunOptionsFactory";
import {RunDfaCommandInputValidatorFactory} from "../../../lib/InputValidatorFactory";
import {PathFactory, PathFactoryImpl} from "../../../lib/PathFactory";
import {RunDfaEngineOptionsFactory} from "../../../lib/EngineOptionsFactory";
import {BUNDLE, getBundledMessage} from "../../../MessageCatalog";

export default class Dfa extends ScannerRunCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static summary = getBundledMessage(BUNDLE.RUN_DFA, 'commandSummary');
	public static description = getBundledMessage(BUNDLE.RUN_DFA, 'commandDescription');

	public static examples = [
		getBundledMessage(BUNDLE.RUN_DFA, 'examples')
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
			summary: getBundledMessage(BUNDLE.RUN_DFA, 'flags.withpilotSummary'),
			description: getBundledMessage(BUNDLE.RUN_DFA, 'flags.withpilotDescription')
		}),
		// END: Filter-related flags.
		// BEGIN: Flags for targeting files.
		// NOTE: All run commands have a `--target` flag, but they have differing functionalities,
		// and therefore different descriptions, so each command defines this flag separately.
		target: Flags.custom<string[]>({
			char: 't',
			summary: getBundledMessage(BUNDLE.RUN_DFA, 'flags.targetSummary'),
			description: getBundledMessage(BUNDLE.RUN_DFA, 'flags.targetDescription'),
			required: true,
			delimiter: ',',
			multiple: true
		})(),
		// END: Flags for targeting files.
		// BEGIN: Config-overrideable engine flags.
		'rule-thread-count': Flags.integer({
			summary: getBundledMessage(BUNDLE.RUN_DFA, 'flags.rulethreadcountSummary'),
			description: getBundledMessage(BUNDLE.RUN_DFA, 'flags.rulethreadcountDescription'),
			env: 'SFGE_RULE_THREAD_COUNT'
		}),
		'rule-thread-timeout': Flags.integer({
			summary: getBundledMessage(BUNDLE.RUN_DFA, 'flags.rulethreadtimeoutSummary'),
			description: getBundledMessage(BUNDLE.RUN_DFA, 'flags.rulethreadtimeoutDescription'),
			env: 'SFGE_RULE_THREAD_TIMEOUT'
		}),
		// NOTE: This flag can't use the `env` property to inherit a value automatically, because OCLIF boolean flags
		// don't support that. Instead, we check the env-var manually in a subsequent method.
		'rule-disable-warning-violation': Flags.boolean({
			summary: getBundledMessage(BUNDLE.RUN_DFA, 'flags.ruledisablewarningviolationSummary'),
			description: getBundledMessage(BUNDLE.RUN_DFA, 'flags.ruledisablewarningviolationDescription')
		}),
		'sfgejvmargs': Flags.string({
			summary: getBundledMessage(BUNDLE.RUN_DFA, 'flags.sfgejvmargsSummary'),
			description: getBundledMessage(BUNDLE.RUN_DFA, 'flags.sfgejvmargsDescription'),
			env: 'SFGE_JVM_ARGS'
		}),
		'pathexplimit': Flags.integer({
			summary: getBundledMessage(BUNDLE.RUN_DFA, 'flags.pathexplimitSummary'),
			description: getBundledMessage(BUNDLE.RUN_DFA, 'flags.pathexplimitDescription'),
			env: 'SFGE_PATH_EXPANSION_LIMIT'
		})
		// END: Config-overrideable engine flags.
	};

	public constructor(argv: string[], config: Config) {
		const pathFactory: PathFactory = new PathFactoryImpl();
		super(argv, config,
			new RunDfaCommandInputValidatorFactory(),
			new RuleFilterFactoryImpl(),
			new RunOptionsFactoryImpl(true, config.version),
			pathFactory,
			new RunDfaEngineOptionsFactory(pathFactory));
	}
}
