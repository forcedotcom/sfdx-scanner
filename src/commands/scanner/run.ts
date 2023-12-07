import {Flags} from '@salesforce/sf-plugins-core';
import {Messages, SfError} from '@salesforce/core';
import {LooseObject} from '../../types';
import {PathlessEngineFilters} from '../../Constants';
import {CUSTOM_CONFIG} from '../../Constants';
import {ScannerRunCommand, INTERNAL_ERROR_CODE} from '../../lib/ScannerRunCommand';
import {TYPESCRIPT_ENGINE_OPTIONS} from '../../lib/eslint/TypescriptEslintStrategy';
import untildify = require('untildify');
import normalize = require('normalize-path');

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run-pathless');

export default class Run extends ScannerRunCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static summary = messages.getMessage('commandSummary');
	public static description = messages.getMessage('commandDescription');

	public static examples = [
		messages.getMessage('examples')
	];

	// This defines the flags accepted by this command.
	public static readonly flags= {
		// Include all common flags from the super class.
		...ScannerRunCommand.flags,
		// BEGIN: Filter-related flags.
		ruleset: Flags.custom<string[]>({
			char: 'r',
			deprecated: {
				message: messages.getMessage('rulesetDeprecation')
			},
			summary: messages.getMessage('flags.rulesetSummary'),
			description: messages.getMessage('flags.rulesetDescription'),
			delimiter: ',',
			multiple: true
		})(),
		engine: Flags.custom<string[]>({
			char: 'e',
			summary: messages.getMessage('flags.engineSummary'),
			description: messages.getMessage('flags.engineDescription'),
			options: [...PathlessEngineFilters],
			delimiter: ',',
			multiple: true
		})(),
		// END: Filter-related flags.
		// BEGIN: Targeting-related flags.
		target: Flags.custom<string[]>({
			char: 't',
			summary: messages.getMessage('flags.targetSummary'),
			description: messages.getMessage('flags.targetDescription'),
			delimiter: ',',
			multiple: true,
			required: true
		})(),
		// END: Targeting-related flags.
		// BEGIN: Engine config flags.
		tsconfig: Flags.string({
			summary: messages.getMessage('flags.tsconfigSummary'),
			description: messages.getMessage('flags.tsconfigDescription')
		}),
		eslintconfig: Flags.string({
			summary: messages.getMessage('flags.eslintConfigSummary'),
			description: messages.getMessage('flags.eslintConfigDescription')
		}),
		pmdconfig: Flags.string({
			summary: messages.getMessage('flags.pmdConfigSummary'),
			description: messages.getMessage('flags.pmdConfigDescription')
		}),
		// TODO: This flag was implemented for W-7791882, and it's suboptimal. It leaks the abstraction and pollutes the command.
		//   It should be replaced during the 3.0 release cycle.
		env: Flags.string({
			summary: messages.getMessage('flags.envSummary'),
			description: messages.getMessage('flags.envDescription'),
			deprecated: {
				message: messages.getMessage('flags.envParamDeprecationWarning')
			}
		}),
		// END: Engine config flags.
		// BEGIN: Flags related to results processing.
		"verbose-violations": Flags.boolean({
			summary: messages.getMessage('flags.verboseViolationsSummary'),
			description: messages.getMessage('flags.verboseViolationsDescription')
		})
		// END: Flags related to results processing.
	};

	protected validateVariantFlags(): Promise<void> {
		if (this.parsedFlags.tsconfig && this.parsedFlags.eslintconfig) {
			throw new SfError(messages.getMessage('validations.tsConfigEslintConfigExclusive'));
		}

		if ((this.parsedFlags.pmdconfig || this.parsedFlags.eslintconfig) && (this.parsedFlags.category || this.parsedFlags.ruleset)) {
			this.log(messages.getMessage('output.filtersIgnoredCustom', []));
		}
		// None of the pathless engines support method-level targeting, so attempting to use it should result in an error.
		for (const target of (this.parsedFlags.target as string[])) {
			if (target.indexOf('#') > -1) {
				throw new SfError(messages.getMessage('validations.methodLevelTargetingDisallowed', [target]));
			}
		}
		return Promise.resolve();
	}

	/**
	 * Gather engine options that are unique to each sub-variant.
	 */
	protected mergeVariantEngineOptions(options: Map<string,string>): void {
		if (this.parsedFlags.tsconfig) {
			const tsconfig = normalize(untildify(this.parsedFlags.tsconfig as string));
			options.set(TYPESCRIPT_ENGINE_OPTIONS.TSCONFIG, tsconfig);
		}

		// TODO: This fix for W-7791882 is suboptimal, because it leaks our abstractions and pollutes the command with
		//  engine-specific flags. Replace it in 3.0.
		if (this.parsedFlags.env) {
			try {
				const parsedEnv: LooseObject = JSON.parse(this.parsedFlags.env as string) as LooseObject;
				options.set('env', JSON.stringify(parsedEnv));
			} catch (e) {
				throw new SfError(messages.getMessage('output.invalidEnvJson'), null, null, INTERNAL_ERROR_CODE);
			}
		}

		// Capturing eslintconfig value, if provided
		if (this.parsedFlags.eslintconfig) {
			const eslintConfig = normalize(untildify(this.parsedFlags.eslintconfig as string));
			options.set(CUSTOM_CONFIG.EslintConfig, eslintConfig);
		}

		// Capturing pmdconfig value, if provided
		if (this.parsedFlags.pmdconfig) {
			const pmdConfig = normalize(untildify(this.parsedFlags.pmdconfig as string));
			options.set(CUSTOM_CONFIG.PmdConfig, pmdConfig);
		}

		// Capturing verbose-violations flag value (used for RetireJS output)
		if (this.parsedFlags["verbose-violations"]) {
			options.set(CUSTOM_CONFIG.VerboseViolations, "true");
		}
	}

	protected pathBasedEngines(): boolean {
		return false;
	}
}
