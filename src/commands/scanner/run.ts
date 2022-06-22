import {flags} from '@salesforce/command';
import {Messages, SfdxError} from '@salesforce/core';
import {LooseObject} from '../../types';
import {PathlessEngineFilters} from '../../Constants';
import {CUSTOM_CONFIG} from '../../Constants';
import {OUTPUT_FORMAT} from '../../lib/RuleManager';
import {ScannerRunCommand, INTERNAL_ERROR_CODE} from '../../lib/ScannerRunCommand';
import {TYPESCRIPT_ENGINE_OPTIONS} from '../../lib/eslint/TypescriptEslintStrategy';
import untildify = require('untildify');
import normalize = require('normalize-path');

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run');

export default class Run extends ScannerRunCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static description = messages.getMessage('commandDescription');
	public static longDescription = messages.getMessage('commandDescriptionLong');

	public static examples = [
		messages.getMessage('examples')
	];

	public static args = [{name: 'file'}];

	// This defines the flags accepted by this command.
	protected static flagsConfig = {
		verbose: flags.builtin(),
		// BEGIN: Flags consumed by ScannerCommand#buildRuleFilters
		// These flags are how you choose which rules you're running.
		category: flags.array({
			char: 'c',
			description: messages.getMessage('flags.categoryDescription'),
			longDescription: messages.getMessage('flags.categoryDescriptionLong')
		}),
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
		// END: Flags consumed by ScannerCommand#buildRuleFilters
		// These flags are how you choose which files you're targeting.
		target: flags.array({
			char: 't',
			description: messages.getMessage('flags.targetDescription'),
			longDescription: messages.getMessage('flags.targetDescriptionLong'),
			required: true
		}),
		// These flags modify how the process runs, rather than what it consumes.
		format: flags.enum({
			char: 'f',
			description: messages.getMessage('flags.formatDescription'),
			longDescription: messages.getMessage('flags.formatDescriptionLong'),
			options: [OUTPUT_FORMAT.CSV, OUTPUT_FORMAT.HTML, OUTPUT_FORMAT.JSON, OUTPUT_FORMAT.JUNIT, OUTPUT_FORMAT.SARIF, OUTPUT_FORMAT.TABLE, OUTPUT_FORMAT.XML]
		}),
		outfile: flags.string({
			char: 'o',
			description: messages.getMessage('flags.outfileDescription'),
			longDescription: messages.getMessage('flags.outfileDescriptionLong')
		}),
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
		'severity-threshold': flags.integer({
            char: 's',
            description: messages.getMessage('flags.stDescription'),
            longDescription: messages.getMessage('flags.stDescriptionLong'),
			exclusive: ['json'],
			min: 1,
			max: 3
        }),
		"normalize-severity": flags.boolean({
			description: messages.getMessage('flags.nsDescription'),
			longDescription: messages.getMessage('flags.nsDescriptionLong')
		}),
		"verbose-violations": flags.boolean({
			description: messages.getMessage('flags.verboseViolationsDescription'),
			longDescription: messages.getMessage('flags.verboseViolationsDescriptionLong')
		}),
	};

	protected validateCommandFlags(): Promise<void> {
		if (this.flags.tsconfig && this.flags.eslintconfig) {
			throw SfdxError.create('@salesforce/sfdx-scanner', 'run', 'validations.tsConfigEslintConfigExclusive', []);
		}

		if ((this.flags.pmdconfig || this.flags.eslintconfig) && (this.flags.category || this.flags.ruleset)) {
			this.ux.log(messages.getMessage('output.filtersIgnoredCustom', []));
		}
		// None of the pathless engines support method-level targeting, so attempting to use it should result in an error.
		for (const target of (this.flags.target as string[])) {
			if (target.indexOf('#') > -1) {
				throw SfdxError.create('@salesforce/sfdx-scanner', 'run', 'validations.methodLevelTargetingDisallowed', [target]);
			}
		}
		return Promise.resolve();
	}

	/**
	 * Gather a map of options that will be passed to the RuleManager without validation.
	 */
	protected gatherEngineOptions(): Map<string, string> {
		const options: Map<string,string> = new Map();
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
				throw new SfdxError(messages.getMessage('output.invalidEnvJson'), null, null, INTERNAL_ERROR_CODE);
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


		return options;
	}

	protected pathBasedEngines(): boolean {
		return false;
	}
}
