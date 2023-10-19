import {flags} from '@salesforce/command';
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
	public static description = messages.getMessage('commandDescription');
	public static longDescription = messages.getMessage('commandDescriptionLong');

	public static examples = [
		messages.getMessage('examples')
	];

	// This defines the flags accepted by this command.
	protected static flagsConfig = {
		// Include all common flags from the super class.
		...ScannerRunCommand.flagsConfig,
		// BEGIN: Filter-related flags.
		ruleset: flags.array({
			char: 'r',
			deprecated: {
				messageOverride: messages.getMessage('rulesetDeprecation')
			},
			description: messages.getMessage('flags.rulesetDescription'),
			longDescription: messages.getMessage('flags.rulesetDescriptionLong')
		}),
		engine: flags.array({
			char: 'e',
			description: messages.getMessage('flags.engineDescription'),
			longDescription: messages.getMessage('flags.engineDescriptionLong'),
			options: [...PathlessEngineFilters]
		}),
		// END: Filter-related flags.
		// BEGIN: Targeting-related flags.
		target: flags.array({
			char: 't',
			description: messages.getMessage('flags.targetDescription'),
			longDescription: messages.getMessage('flags.targetDescriptionLong'),
			required: true
		}),
		// END: Targeting-related flags.
		// BEGIN: Engine config flags.
		tsconfig: flags.string({
			description: messages.getMessage('flags.tsconfigDescription'),
			longDescription: messages.getMessage('flags.tsconfigDescriptionLong')
		}),
		eslintconfig: flags.string({
			description: messages.getMessage('flags.eslintConfigDescription'),
			longDescription: messages.getMessage('flags.eslintConfigDescriptionLong')
		}),
		pmdconfig: flags.string({
			description: messages.getMessage('flags.pmdConfigDescription'),
			longDescription: messages.getMessage('flags.pmdConfigDescriptionLong')
		}),
		// TODO: This flag was implemented for W-7791882, and it's suboptimal. It leaks the abstraction and pollutes the command.
		//   It should be replaced during the 3.0 release cycle.
		env: flags.string({
			description: messages.getMessage('flags.envDescription'),
			longDescription: messages.getMessage('flags.envDescriptionLong'),
			deprecated: {
				messageOverride: messages.getMessage('flags.envParamDeprecationWarning')
			}
		}),
		// END: Engine config flags.
		// BEGIN: Flags related to results processing.
		"verbose-violations": flags.boolean({
			description: messages.getMessage('flags.verboseViolationsDescription'),
			longDescription: messages.getMessage('flags.verboseViolationsDescriptionLong')
		})
		// END: Flags related to results processing.
	};

	protected validateVariantFlags(): Promise<void> {
		if (this.flags.tsconfig && this.flags.eslintconfig) {
			throw new SfError(messages.getMessage('validations.tsConfigEslintConfigExclusive'));
		}

		if ((this.flags.pmdconfig || this.flags.eslintconfig) && (this.flags.category || this.flags.ruleset)) {
			this.ux.log(messages.getMessage('output.filtersIgnoredCustom', []));
		}
		// None of the pathless engines support method-level targeting, so attempting to use it should result in an error.
		for (const target of (this.flags.target as string[])) {
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
		if (this.flags.tsconfig) {
			const tsconfig = normalize(untildify(this.flags.tsconfig as string));
			options.set(TYPESCRIPT_ENGINE_OPTIONS.TSCONFIG, tsconfig);
		}

		// TODO: This fix for W-7791882 is suboptimal, because it leaks our abstractions and pollutes the command with
		//  engine-specific flags. Replace it in 3.0.
		if (this.flags.env) {
			try {
				const parsedEnv: LooseObject = JSON.parse(this.flags.env as string) as LooseObject;
				options.set('env', JSON.stringify(parsedEnv));
			} catch (e) {
				throw new SfError(messages.getMessage('output.invalidEnvJson'), null, null, INTERNAL_ERROR_CODE);
			}
		}

		// Capturing eslintconfig value, if provided
		if (this.flags.eslintconfig) {
			const eslintConfig = normalize(untildify(this.flags.eslintconfig as string));
			options.set(CUSTOM_CONFIG.EslintConfig, eslintConfig);
		}

		// Capturing pmdconfig value, if provided
		if (this.flags.pmdconfig) {
			const pmdConfig = normalize(untildify(this.flags.pmdconfig as string));
			options.set(CUSTOM_CONFIG.PmdConfig, pmdConfig);
		}

		// Capturing verbose-violations flag value (used for RetireJS output)
		if (this.flags["verbose-violations"]) {
			options.set(CUSTOM_CONFIG.VerboseViolations, "true");
		}
	}

	protected pathBasedEngines(): boolean {
		return false;
	}
}
