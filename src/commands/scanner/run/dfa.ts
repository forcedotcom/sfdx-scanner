import globby = require('globby');
import {flags} from '@salesforce/command';
import {Messages, SfdxError} from '@salesforce/core';
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
	public static description = messages.getMessage('commandDescription');
	public static longDescription = messages.getMessage('commandDescriptionLong');

	public static examples = [
		messages.getMessage('examples')
	];

	public static args = [{name: 'file'}];

	// This defines the flags accepted by this command.
	// NOTE: Unlike the other classes that extend ScannerCommand, this class has no flags for specifying rules. This is
	// because the command currently supports only a single engine with a single rule. So no such flags are currently
	// needed. If, at some point, we add additional rules or engines to this command, those flags will need to be added.
	protected static flagsConfig = {
		// Include all common flags from the super class.
		...ScannerRunCommand.flagsConfig,
		// BEGIN: Flags for targeting files.
		// NOTE: All run commands have a `--target` flag, but they have differing functionalities,
		// and therefore different descriptions, so each command defines this flag separately.
		target: flags.array({
			char: 't',
			description: messages.getMessage('flags.targetDescription'),
			longDescription: messages.getMessage('flags.targetDescriptionLong'),
			required: true
		}),
		// END: Flags for targeting files.
		// BEGIN: Config-overrideable engine flags.
		'rule-thread-count': flags.integer({
			description: messages.getMessage('flags.rulethreadcountDescription'),
			longDescription: messages.getMessage('flags.rulethreadcountDescriptionLong'),
			env: 'SFGE_RULE_THREAD_COUNT'
		}),
		'rule-thread-timeout': flags.integer({
			description: messages.getMessage('flags.rulethreadtimeoutDescription'),
			longDescription: messages.getMessage('flags.rulethreadtimeoutDescriptionLong'),
			env: 'SFGE_RULE_THREAD_TIMEOUT'
		}),
		// NOTE: This flag can't use the `env` property to inherit a value automatically, because OCLIF boolean flags
		// don't support that. Instead, we check the env-var manually in a subsequent method.
		[RULE_DISABLE_WARNING_VIOLATION_FLAG]: flags.boolean({
			description: messages.getMessage('flags.ruledisablewarningviolationDescription'),
			longDescription: messages.getMessage('flags.ruledisablewarningviolationDescriptionLong')
		}),
		'sfgejvmargs': flags.string({
			description: messages.getMessage('flags.sfgejvmargsDescription'),
			longDescription: messages.getMessage('flags.sfgejvmargsDescriptionLong'),
			env: 'SFGE_JVM_ARGS'
		})
		// END: Config-overrideable engine flags.
	};

	protected async validateVariantFlags(): Promise<void> {
		const fh = new FileHandler();
		// The superclass will validate that --projectdir is well-formed,
		// but doesn't require that the flag actually be present.
		// So we should make sure it exists here.
		if (!this.flags.projectdir || (this.flags.projectdir as string[]).length === 0) {
			throw SfdxError.create('@salesforce/sfdx-scanner', 'run-dfa', 'validations.projectdirIsRequired', []);
		}
		// Entries in the target array may specify methods, but only if the entry is neither a directory nor a glob.
		for (const target of (this.flags.target as string[])) {
			// The target specifies a method if it includes the `#` syntax.
			if (target.indexOf('#') > -1) {
				if( globby.hasMagic(target)) {
					throw SfdxError.create('@salesforce/sfdx-scanner', 'run-dfa', 'validations.methodLevelTargetCannotBeGlob', []);
				}
				const potentialFilePath = target.split('#')[0];
				if (!(await fh.isFile(potentialFilePath))) {
					throw SfdxError.create('@salesforce/sfdx-scanner', 'run-dfa', 'validations.methodLevelTargetMustBeRealFile', [potentialFilePath]);
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
		if (this.flags['rule-thread-count'] != null) {
			sfgeConfig.ruleThreadCount = this.flags['rule-thread-count'] as number;
		}
		if (this.flags['rule-thread-timeout'] != null) {
			sfgeConfig.ruleThreadTimeout = this.flags['rule-thread-timeout'] as number;
		}
		if (this.flags['sfgejvmargs'] != null) {
			sfgeConfig.jvmArgs = this.flags['sfgejvmargs'] as string;
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
		if (this.flags[flag] != null) {
			return this.flags[flag] as boolean;
		}
		// If the flag isn't set, get the name of the corresponding environment variable and check its value.
		const envVar = BOOLEAN_ENVARS_BY_FLAG.get(flag);
		return envVar in process.env && process.env[envVar].toLowerCase() === 'true';
	}

	protected pathBasedEngines(): boolean {
		return true;
	}
}
