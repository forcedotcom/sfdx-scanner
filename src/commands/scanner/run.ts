import {flags} from '@salesforce/command';
import {Messages, SfdxError} from '@salesforce/core';
import {LooseObject} from '../../types';
import {ENGINE, PathlessEngineFilters} from '../../Constants';
import {CUSTOM_CONFIG} from '../../Constants';
import {OUTPUT_FORMAT} from '../../lib/RuleManager';
import {ScannerRunCommand, INTERNAL_ERROR_CODE} from '../../lib/ScannerRunCommand';
import {TYPESCRIPT_ENGINE_OPTIONS} from '../../lib/eslint/TypescriptEslintStrategy';
import untildify = require('untildify');
import normalize = require('normalize-path');
import {stringArrayTypeGuard} from '../../lib/util/Utils';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const commonRunMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run-common');
const pathlessMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run');

export default class Run extends ScannerRunCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static description = pathlessMessages.getMessage('commandDescription');
	public static longDescription = pathlessMessages.getMessage('commandDescriptionLong');

	public static examples = [
		pathlessMessages.getMessage('examples')
	];

	public static args = [{name: 'file'}];

	// This defines the flags accepted by this command.
	protected static flagsConfig = {
		verbose: flags.builtin(),
		// BEGIN: Flags consumed by ScannerCommand#buildRuleFilters
		// These flags are how you choose which rules you're running.
		category: flags.array({
			char: 'c',
			description: pathlessMessages.getMessage('flags.categoryDescription'),
			longDescription: pathlessMessages.getMessage('flags.categoryDescriptionLong')
		}),
		ruleset: flags.array({
			char: 'r',
			deprecated: {
				messageOverride: pathlessMessages.getMessage('rulesetDeprecation')
			},
			description: pathlessMessages.getMessage('flags.rulesetDescription'),
			longDescription: pathlessMessages.getMessage('flags.rulesetDescriptionLong')
		}),
		engine: flags.array({
			char: 'e',
			description: pathlessMessages.getMessage('flags.engineDescription'),
			longDescription: pathlessMessages.getMessage('flags.engineDescriptionLong'),
			options: [...PathlessEngineFilters]
		}),
		// END: Flags consumed by ScannerCommand#buildRuleFilters
		// These flags are how you choose which files you're targeting.
		target: flags.array({
			char: 't',
			description: commonRunMessages.getMessage('flags.targetDescription'),
			longDescription: pathlessMessages.getMessage('flags.targetDescriptionLong'),
			required: true
		}),
		// These flags modify how the process runs, rather than what it consumes.
		format: flags.enum({
			char: 'f',
			description: commonRunMessages.getMessage('flags.formatDescription'),
			longDescription: commonRunMessages.getMessage('flags.formatDescriptionLong'),
			options: [OUTPUT_FORMAT.CSV, OUTPUT_FORMAT.HTML, OUTPUT_FORMAT.JSON, OUTPUT_FORMAT.JUNIT, OUTPUT_FORMAT.SARIF, OUTPUT_FORMAT.TABLE, OUTPUT_FORMAT.XML]
		}),
		outfile: flags.string({
			char: 'o',
			description: commonRunMessages.getMessage('flags.outfileDescription'),
			longDescription: commonRunMessages.getMessage('flags.outfileDescriptionLong')
		}),
		tsconfig: flags.string({
			description: pathlessMessages.getMessage('flags.tsconfigDescription'),
			longDescription: pathlessMessages.getMessage('flags.tsconfigDescriptionLong')
		}),
		eslintconfig: flags.string({
			description: pathlessMessages.getMessage('flags.eslintConfigDescription'),
			longDescription: pathlessMessages.getMessage('flags.eslintConfigDescriptionLong')
		}),
		pmdconfig: flags.string({
			description: pathlessMessages.getMessage('flags.pmdConfigDescription'),
			longDescription: pathlessMessages.getMessage('flags.pmdConfigDescriptionLong')
		}),
		// TODO: This flag was implemented for W-7791882, and it's suboptimal. It leaks the abstraction and pollutes the command.
		//   It should be replaced during the 3.0 release cycle.
		env: flags.string({
			description: pathlessMessages.getMessage('flags.envDescription'),
			longDescription: pathlessMessages.getMessage('flags.envDescriptionLong'),
			deprecated: {
				messageOverride: pathlessMessages.getMessage('flags.envParamDeprecationWarning')
			}
		}),
		projectdir:  flags.array({
			char: 'p',
			description: commonRunMessages.getMessage('flags.projectdirDescription'),
			longDescription: commonRunMessages.getMessage('flags.projectdirDescriptionLong'),
			// eslint-disable-next-line @typescript-eslint/no-unsafe-argument
			map: d => normalize(untildify(d))
		}),
		// NOTE: This flag can't use the `env` property to inherit a value automatically, because OCLIF boolean flags
		// don't support that. Instead, we check the env-var manually in a subsequent method.
		'ignore-parse-errors': flags.boolean({
			description: commonRunMessages.getMessage('flags.ignoreparseerrorsDescription'),
			longDescription: commonRunMessages.getMessage('flags.ignoreparseerrorsDescriptionLong'),
			env: 'SFGE_IGNORE_PARSE_ERRORS'
		}),
		'severity-threshold': flags.integer({
            char: 's',
            description: commonRunMessages.getMessage('flags.sevthresholdDescription'),
            longDescription: commonRunMessages.getMessage('flags.sevthresholdDescriptionLong'),
			exclusive: ['json'],
			min: 1,
			max: 3
        }),
		"normalize-severity": flags.boolean({
			description: commonRunMessages.getMessage('flags.normalizesevDescription'),
			longDescription: commonRunMessages.getMessage('flags.normalizesevDescriptionLong')
		}),
		"verbose-violations": flags.boolean({
			description: pathlessMessages.getMessage('flags.verboseViolationsDescription'),
			longDescription: pathlessMessages.getMessage('flags.verboseViolationsDescriptionLong')
		}),
	};

	protected validateCommandFlags(): Promise<void> {
		if (this.flags.engine && stringArrayTypeGuard(this.flags.engine) && this.flags.engine.some(e => e === ENGINE.SFGE)) {
			if (!this.flags.projectdir || !stringArrayTypeGuard(this.flags.projectdir) || this.flags.projectdir.length === 0) {
				// If SFGE is specifically requested, then the projectdir flag must be used.
				throw SfdxError.create('@salesforce/sfdx-scanner', 'run', 'validations.sfgeRequiresProjectdir', []);
			}
		}
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
	protected gatherCommandEngineOptions(partialOptions: Map<string,string>): Map<string, string> {
		if (this.flags.tsconfig) {
			const tsconfig = normalize(untildify(this.flags.tsconfig as string));
			partialOptions.set(TYPESCRIPT_ENGINE_OPTIONS.TSCONFIG, tsconfig);
		}

		// TODO: This fix for W-7791882 is suboptimal, because it leaks our abstractions and pollutes the command with
		//  engine-specific flags. Replace it in 3.0.
		if (this.flags.env) {
			try {
				const parsedEnv: LooseObject = JSON.parse(this.flags.env as string) as LooseObject;
				partialOptions.set('env', JSON.stringify(parsedEnv));
			} catch (e) {
				throw new SfdxError(messages.getMessage('output.invalidEnvJson'), null, null, INTERNAL_ERROR_CODE);
			}
		}

		// Capturing eslintconfig value, if provided
		if (this.flags.eslintconfig) {
			const eslintConfig = normalize(untildify(this.flags.eslintconfig as string));
			partialOptions.set(CUSTOM_CONFIG.EslintConfig, eslintConfig);
		}

		// Capturing pmdconfig value, if provided
		if (this.flags.pmdconfig) {
			const pmdConfig = normalize(untildify(this.flags.pmdconfig as string));
			partialOptions.set(CUSTOM_CONFIG.PmdConfig, pmdConfig);
		}

		// Capturing verbose-violations flag value (used for RetireJS output)
		if (this.flags["verbose-violations"]) {
			partialOptions.set(CUSTOM_CONFIG.VerboseViolations, "true");
		}


		return partialOptions;
	}

	protected pathBasedEngines(): boolean {
		return false;
	}
}
