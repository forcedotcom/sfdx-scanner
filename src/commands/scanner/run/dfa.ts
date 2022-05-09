import path = require('path');
import globby = require('globby');
import normalize = require('normalize-path');
import untildify = require('untildify');
import {flags} from '@salesforce/command';
import {Messages, SfdxError} from '@salesforce/core';
import {CUSTOM_CONFIG} from '../../../Constants';
import {SfgeConfig} from '../../../types';
import {ScannerRunCommand} from '../../../lib/ScannerRunCommand';
import {OUTPUT_FORMAT} from '../../../lib/RuleManager';
import {FileHandler} from '../../../lib/util/FileHandler';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run-dfa');

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
		verbose: flags.builtin(),
		// BEGIN: Flags for targeting files.
		target: flags.array({
			char: 't',
			description: messages.getMessage('flags.targetDescription'),
			longDescription: messages.getMessage('flags.targetDescriptionLong'),
			required: true
		}),
		projectdir:  flags.array({
			char: 'p',
			description: messages.getMessage('flags.projectdirDescription'),
			longDescription: messages.getMessage('flags.projectdirDescriptionLong'),
			// eslint-disable-next-line @typescript-eslint/no-unsafe-argument
			map: d => path.resolve(normalize(untildify(d))),
			required: true
		}),
		// END: Flags for targeting files.
		// BEGIN: Flags for result processing.
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
		'severity-threshold': flags.integer({
			char: 's',
			description: messages.getMessage('flags.sevthresholdDescription'),
			longDescription: messages.getMessage('flags.sevthresholdDescriptionLong'),
			exclusive: ['json'],
			min: 1,
			max: 3
		}),
		'normalize-severity': flags.boolean({
			description: messages.getMessage('flags.normalizesevDescription'),
			longDescription: messages.getMessage('flags.normalizesevDescriptionLong')
		}),
		// END: Flags for result processing.
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
		'ignore-parse-errors': flags.boolean({
			description: messages.getMessage('flags.ignoreparseerrorsDescription'),
			longDescription: messages.getMessage('flags.ignoreparseerrorsDescriptionLong'),
			env: 'SFGE_IGNORE_PARSE_ERRORS'
		})
		// END: Config-overrideable engine flags.
	};

	protected async validateCommandFlags(): Promise<void> {
		// Entries in the projectdir array must be non-glob paths to existing directories.
		const fh = new FileHandler();
		for (const dir of (this.flags.projectdir as string[])) {
			if (globby.hasMagic(dir)) {
				throw SfdxError.create('@salesforce/sfdx-scanner', 'run-dfa', 'validations.projectdirCannotBeGlob', []);
			} else if (!(await fh.exists(dir))) {
				throw SfdxError.create('@salesforce/sfdx-scanner', 'run-dfa', 'validations.projectdirMustExist', []);
			} else if (!(await fh.stats(dir)).isDirectory()) {
				throw SfdxError.create('@salesforce/sfdx-scanner', 'run-dfa', 'validations.projectdirMustBeDir', []);
			}
		}
	}

	/**
	 * Gather a map of options that will be passed to the RuleManager without validation.
	 * @private
	 */
	protected gatherEngineOptions(): Map<string,string> {
		const options: Map<string,string> = new Map();
		const sfgeConfig: SfgeConfig = {
			projectDirs: this.flags.projectdir as string[]
		};

		if (this.flags['rule-thread-count'] != null) {
			sfgeConfig.ruleThreadCount = this.flags['rule-thread-count'] as number;
		}
		if (this.flags['rule-thread-timeout'] != null) {
			sfgeConfig.ruleThreadTimeout = this.flags['rule-thread-timeout'] as number;
		}
		if (this.flags['ignore-parse-errors'] != null) {
			sfgeConfig.ignoreParseErrors = this.flags['ignore-parse-errors'] as boolean;
		}
		options.set(CUSTOM_CONFIG.SfgeConfig, JSON.stringify(sfgeConfig));
		return options;
	}

	protected pathBasedEngines(): boolean {
		return true;
	}
}
