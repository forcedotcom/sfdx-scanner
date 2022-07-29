import globby = require('globby');
import normalize = require('normalize-path');
import untildify = require('untildify');
import {flags} from '@salesforce/command';
import {Messages, SfdxError} from '@salesforce/core';
import {ScannerRunCommand} from '../../../lib/ScannerRunCommand';
import {OUTPUT_FORMAT} from '../../../lib/RuleManager';
import {FileHandler} from '../../../lib/util/FileHandler';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const commonRunMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run-common');
const dfaMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run-dfa');

export default class Dfa extends ScannerRunCommand {
	// These determine what's displayed when the --help/-h flag is provided.
	public static description = dfaMessages.getMessage('commandDescription');
	public static longDescription = dfaMessages.getMessage('commandDescriptionLong');

	public static examples = [
		dfaMessages.getMessage('examples')
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
			description: commonRunMessages.getMessage('flags.targetDescription'),
			longDescription: dfaMessages.getMessage('flags.targetDescriptionLong'),
			required: true
		}),
		projectdir:  flags.array({
			char: 'p',
			description: commonRunMessages.getMessage('flags.projectdirDescription'),
			longDescription: commonRunMessages.getMessage('flags.projectdirDescriptionLong'),
			// eslint-disable-next-line @typescript-eslint/no-unsafe-argument
			map: d => normalize(untildify(d)),
			required: true
		}),
		// END: Flags for targeting files.
		// BEGIN: Flags for result processing.
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
		'severity-threshold': flags.integer({
			char: 's',
			description: commonRunMessages.getMessage('flags.sevthresholdDescription'),
			longDescription: commonRunMessages.getMessage('flags.sevthresholdDescriptionLong'),
			exclusive: ['json'],
			min: 1,
			max: 3
		}),
		'normalize-severity': flags.boolean({
			description: commonRunMessages.getMessage('flags.normalizesevDescription'),
			longDescription: commonRunMessages.getMessage('flags.normalizesevDescriptionLong')
		}),
		// END: Flags for result processing.
		// BEGIN: Config-overrideable engine flags.
		'rule-thread-count': flags.integer({
			description: dfaMessages.getMessage('flags.rulethreadcountDescription'),
			longDescription: dfaMessages.getMessage('flags.rulethreadcountDescriptionLong'),
			env: 'SFGE_RULE_THREAD_COUNT'
		}),
		'rule-thread-timeout': flags.integer({
			description: dfaMessages.getMessage('flags.rulethreadtimeoutDescription'),
			longDescription: dfaMessages.getMessage('flags.rulethreadtimeoutDescriptionLong'),
			env: 'SFGE_RULE_THREAD_TIMEOUT'
		}),
		// NOTE: This flag can't use the `env` property to inherit a value automatically, because OCLIF boolean flags
		// don't support that. Instead, we check the env-var manually in a subsequent method.
		'ignore-parse-errors': flags.boolean({
			description: commonRunMessages.getMessage('flags.ignoreparseerrorsDescription'),
			longDescription: commonRunMessages.getMessage('flags.ignoreparseerrorsDescriptionLong'),
			env: 'SFGE_IGNORE_PARSE_ERRORS'
		})
		// END: Config-overrideable engine flags.
	};

	protected async validateCommandFlags(): Promise<void> {
		const fh = new FileHandler();
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
	 * Gather a map of options that will be passed to the RuleManager without validation.
	 * @protected
	 * @override
	 */
	protected gatherCommandEngineOptions(partialOptions: Map<string,string>): Map<string,string> {
		// All relevant options are already gathered by the super class, so this can
		// just return the map as-is.
		return partialOptions;
	}

	protected pathBasedEngines(): boolean {
		return true;
	}
}
