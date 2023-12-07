import globby = require('globby');
import {Flags} from '@salesforce/sf-plugins-core';
import {Messages, SfError} from '@salesforce/core';
import {CUSTOM_CONFIG} from '../../../Constants';
import {SfgeConfig} from '../../../types';
import {ScannerRunCommand} from '../../../lib/ScannerRunCommand';
import {FileHandler} from '../../../lib/util/FileHandler';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run-dfa');

const RULE_DISABLE_WARNING_VIOLATION_FLAG = 'rule-disable-warning-violation';
const RULE_DISABLE_WARNING_VIOLATION_ENVVAR = 'SFGE_RULE_DISABLE_WARNING_VIOLATION';
const BOOLEAN_ENVARS_BY_FLAG: Map<string,string> = new Map([
	[RULE_DISABLE_WARNING_VIOLATION_FLAG, RULE_DISABLE_WARNING_VIOLATION_ENVVAR]
]);

export default class Dfa extends ScannerRunCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static summary = messages.getMessage('commandSummary');
	public static description = messages.getMessage('commandDescription');

	public static examples = [
		messages.getMessage('examples')
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
			summary: messages.getMessage('flags.withpilotSummary'),
			description: messages.getMessage('flags.withpilotDescription')
		}),
		// END: Filter-related flags.
		// BEGIN: Flags for targeting files.
		// NOTE: All run commands have a `--target` flag, but they have differing functionalities,
		// and therefore different descriptions, so each command defines this flag separately.
		target: Flags.custom<string[]>({
			char: 't',
			summary: messages.getMessage('flags.targetSummary'),
			description: messages.getMessage('flags.targetDescription'),
			required: true,
			delimiter: ',',
			multiple: true
		})(),
		// END: Flags for targeting files.
		// BEGIN: Config-overrideable engine flags.
		'rule-thread-count': Flags.integer({
			summary: messages.getMessage('flags.rulethreadcountSummary'),
			description: messages.getMessage('flags.rulethreadcountDescription'),
			env: 'SFGE_RULE_THREAD_COUNT'
		}),
		'rule-thread-timeout': Flags.integer({
			summary: messages.getMessage('flags.rulethreadtimeoutSummary'),
			description: messages.getMessage('flags.rulethreadtimeoutDescription'),
			env: 'SFGE_RULE_THREAD_TIMEOUT'
		}),
		// NOTE: This flag can't use the `env` property to inherit a value automatically, because OCLIF boolean flags
		// don't support that. Instead, we check the env-var manually in a subsequent method.
		[RULE_DISABLE_WARNING_VIOLATION_FLAG]: Flags.boolean({
			summary: messages.getMessage('flags.ruledisablewarningviolationSummary'),
			description: messages.getMessage('flags.ruledisablewarningviolationDescription')
		}),
		'sfgejvmargs': Flags.string({
			summary: messages.getMessage('flags.sfgejvmargsSummary'),
			description: messages.getMessage('flags.sfgejvmargsDescription'),
			env: 'SFGE_JVM_ARGS'
		}),
		'pathexplimit': Flags.integer({
			summary: messages.getMessage('flags.pathexplimitSummary'),
			description: messages.getMessage('flags.pathexplimitDescription'),
			env: 'SFGE_PATH_EXPANSION_LIMIT'
		})
		// END: Config-overrideable engine flags.
	};

	protected async validateVariantFlags(): Promise<void> {
		const fh = new FileHandler();
		// The superclass will validate that --projectdir is well-formed,
		// but doesn't require that the flag actually be present.
		// So we should make sure it exists here.
		if (!this.parsedFlags.projectdir || (this.parsedFlags.projectdir as string[]).length === 0) {
			throw new SfError(messages.getMessage('validations.projectdirIsRequired'));
		}
		// Entries in the target array may specify methods, but only if the entry is neither a directory nor a glob.
		for (const target of (this.parsedFlags.target as string[])) {
			// The target specifies a method if it includes the `#` syntax.
			if (target.indexOf('#') > -1) {
				if( globby.hasMagic(target)) {
					throw new SfError(messages.getMessage('validations.methodLevelTargetCannotBeGlob'));
				}
				const potentialFilePath = target.split('#')[0];
				if (!(await fh.isFile(potentialFilePath))) {
					throw new SfError(messages.getMessage('validations.methodLevelTargetMustBeRealFile', [potentialFilePath]));
				}
			}
		}
	}

	/**
	 * Gather engine options that are unique to each sub-variant.
	 * @protected
	 * @override
	 */
	protected mergeVariantEngineOptions(options: Map<string,string>): void {
		// The flags have been validated by now, meaning --projectdir is confirmed as present,
		// meaning we can assume the existence of a GraphEngine config in the common options.
		const sfgeConfig: SfgeConfig = JSON.parse(options.get(CUSTOM_CONFIG.SfgeConfig)) as SfgeConfig;
		if (this.parsedFlags['rule-thread-count'] != null) {
			sfgeConfig.ruleThreadCount = this.parsedFlags['rule-thread-count'] as number;
		}
		if (this.parsedFlags['rule-thread-timeout'] != null) {
			sfgeConfig.ruleThreadTimeout = this.parsedFlags['rule-thread-timeout'] as number;
		}
		if (this.parsedFlags['sfgejvmargs'] != null) {
			sfgeConfig.jvmArgs = this.parsedFlags['sfgejvmargs'] as string;
		}
		if (this.parsedFlags['pathexplimit'] != null) {
			sfgeConfig.pathexplimit = this.parsedFlags['pathexplimit'] as number;
		}
		sfgeConfig.ruleDisableWarningViolation = this.getBooleanEngineOption(RULE_DISABLE_WARNING_VIOLATION_FLAG);
		options.set(CUSTOM_CONFIG.SfgeConfig, JSON.stringify(sfgeConfig));
	}

	/**
	 * Boolean flags cannot automatically inherit their value from environment variables. Instead, we use this
	 * method to handle that inheritance if necessary.
	 * @param flag - The name of a boolean flag associated with this command
	 * @returns true if the flag is set or the associated env-var is set to "true"; else false.
	 * @protected
	 */
	private getBooleanEngineOption(flag: string): boolean {
		// Check the status of the flag first, since the flag being true should trump the environment variable's value.
		if (this.parsedFlags[flag] != null) {
			return this.parsedFlags[flag] as boolean;
		}
		// If the flag isn't set, get the name of the corresponding environment variable and check its value.
		const envVar = BOOLEAN_ENVARS_BY_FLAG.get(flag);
		return envVar in process.env && process.env[envVar].toLowerCase() === 'true';
	}

	protected pathBasedEngines(): boolean {
		return true;
	}
}
