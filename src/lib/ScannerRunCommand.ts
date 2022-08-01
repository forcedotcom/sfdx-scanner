import {Messages, SfdxError} from '@salesforce/core';
import {AnyJson} from '@salesforce/ts-types';
import {ScannerCommand} from './ScannerCommand';
import {RecombinedRuleResults, SfgeConfig} from '../types';
import {RunOutputProcessor} from './util/RunOutputProcessor';
import {Controller} from '../Controller';
import {CUSTOM_CONFIG} from '../Constants';
import {OUTPUT_FORMAT, RunOptions} from './RuleManager';
import untildify = require('untildify');
import normalize = require('normalize-path');
import globby = require('globby');
import path = require('path');
import {FileHandler} from './util/FileHandler';
import {stringArrayTypeGuard} from './util/Utils';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run-common');
const commonMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'common');
// This code is used for internal errors.
export const INTERNAL_ERROR_CODE = 1;

const SFGE_IGNORE_PARSE_ERRORS = 'SFGE_IGNORE_PARSE_ERRORS';

export abstract class ScannerRunCommand extends ScannerCommand {

	public async run(): Promise<AnyJson> {
		// First, do any validations that can't be handled with out-of-the-box stuff.
		await this.validateFlags();
		this.ux.styledHeader(commonMessages.getMessage('FEEDBACK_SURVEY_BANNER'));

		// If severity-threshold is used, that implicitly normalizes the severity.
		const normalizeSeverity: boolean = (this.flags['normalize-severity'] || this.flags['severity-threshold']) as boolean;

		// Next, we need to build our input.
		const filters = this.buildRuleFilters();

		// We need to derive the output format, either from information that was explicitly provided or from default values.
		// We can't use the defaultValue property for the flag, because there needs to be a behavioral differenec between
		// defaulting to a value and having the user explicitly select it.
		const runOptions: RunOptions = {
			format: this.determineOutputFormat(),
			normalizeSeverity: normalizeSeverity,
			runDfa: this.pathBasedEngines(),
			sfdxVersion: this.config.version
		};

		const ruleManager = await Controller.createRuleManager();

		// Turn the paths into normalized Unix-formatted paths and strip out any single- or double-quotes, because
		// sometimes shells are stupid and will leave them in there.
		const target = (this.flags.target || []) as string[];
		const targetPaths = target.map(path => normalize(untildify(path)).replace(/['"]/g, ''));

		const engineOptions = this.gatherEngineOptions();

		let output: RecombinedRuleResults = null;
		try {
			output = await ruleManager.runRulesMatchingCriteria(filters, targetPaths, runOptions, engineOptions);
		} catch (e) {
			// Rethrow any errors as SFDX errors.
			const message: string = e instanceof Error ? e.message : e as string;
			throw new SfdxError(message, null, null, INTERNAL_ERROR_CODE);
		}

		return new RunOutputProcessor({
			format: runOptions.format,
			severityForError: this.flags['severity-threshold'] as number,
			outfile: this.flags.outfile as string
		}, this.ux)
			.processRunOutput(output);
	}

	private async validateFlags(): Promise<void> {
		// First, perform any validation of the command's specific flags.
		await this.validateCommandFlags();

		// Some flags are common between subclasses. Validate those too.
		await this.validateCommonFlags();
	}

	/**
	 * Validate whatever flags are specific to a given implementation of {@link ScannerRunCommand}.
	 * @protected
	 * @abstract
	 */
	protected abstract validateCommandFlags(): Promise<void>

	/**
	 * Validate the flags that are common to all implementations of {@link ScannerRunCommand} and share
	 * the same constraints.
	 * @private
	 */
	private async validateCommonFlags(): Promise<void> {
		const fh = new FileHandler();
		// Entries in the projectdir array must be non-glob paths to existing directories.
		if (this.flags.projectdir && stringArrayTypeGuard(this.flags.projectdir) && this.flags.projectdir.length > 0) {
			for (const dir of this.flags.projectdir) {
				if (globby.hasMagic(dir)) {
					throw SfdxError.create('@salesforce/sfdx-scanner', 'run-common', 'validations.projectdirCannotBeGlob', []);
				} else if (!(await fh.exists(dir))) {
					throw SfdxError.create('@salesforce/sfdx-scanner', 'run-common', 'validations.projectdirMustExist', []);
				} else if (!(await fh.stats(dir)).isDirectory()) {
					throw SfdxError.create('@salesforce/sfdx-scanner', 'run-common', 'validations.projectdirMustBeDir', []);
				}
			}
		}
		// If the user explicitly specified both a format and an outfile, we need to do a bit of validation there.
		if (this.flags.format && this.flags.outfile) {
			const inferredOutfileFormat = this.inferFormatFromOutfile();
			// For the purposes of this validation, we treat junit as xml.
			const chosenFormat = this.flags.format === 'junit' ? 'xml' : this.flags.format as string;
			// If the chosen format is TABLE, we immediately need to exit. There's no way to sensibly write the output
			// of TABLE to a file.
			if (chosenFormat === OUTPUT_FORMAT.TABLE) {
				throw SfdxError.create('@salesforce/sfdx-scanner', 'run-common', 'validations.cannotWriteTableToFile', []);
			}
			// Otherwise, we want to be liberal with the user. If the chosen format doesn't match the outfile's extension,
			// just log a message saying so.
			if (chosenFormat !== inferredOutfileFormat) {
				this.ux.log(messages.getMessage('validations.outfileFormatMismatch', [this.flags.format as string, inferredOutfileFormat]));
			}
		}
	}

	protected determineOutputFormat(): OUTPUT_FORMAT {
		// If an output format is explicitly specified, use that.
		if (this.flags.format) {
			return this.flags.format as OUTPUT_FORMAT;
		} else if (this.flags.outfile) {
			// Else If an outfile is explicitly specified, infer the format from its extension.
			return this.inferFormatFromOutfile();
		} else if (this.flags.json) {
			// Else If the --json flag is present, then we'll default to JSON format.
			return OUTPUT_FORMAT.JSON;
		} else {
			// Otherwise, we'll default to the Table format.
			return OUTPUT_FORMAT.TABLE;
		}
	}

	private inferFormatFromOutfile(): OUTPUT_FORMAT {
		const outfile = this.flags.outfile as string;
		const lastPeriod = outfile.lastIndexOf('.');
		// If the outfile is malformed, we're already hosed.
		if (lastPeriod < 1 || lastPeriod + 1 === outfile.length) {
			throw new SfdxError(messages.getMessage('validations.outfileMustBeValid'), null, null, INTERNAL_ERROR_CODE);
		} else {
			// Look at the file extension, and infer a corresponding output format.
			const fileExtension = outfile.slice(lastPeriod + 1).toLowerCase();
			switch (fileExtension) {
				case OUTPUT_FORMAT.CSV:
				case OUTPUT_FORMAT.HTML:
				case OUTPUT_FORMAT.JSON:
				case OUTPUT_FORMAT.SARIF:
				case OUTPUT_FORMAT.XML:
					return fileExtension;
				default:
					throw new SfdxError(messages.getMessage('validations.outfileMustBeSupportedType'), null, null, INTERNAL_ERROR_CODE);
			}
		}
	}

	private gatherBaseEngineOptions(): Map<string,string> {
		const options: Map<string,string> = new Map();

		if (this.flags.projectdir) {
			const sfgeConfig: SfgeConfig = {
				// At this point, we can assume a non-null projectdir flag
				// as already been type-guarded as a string[], and just cast
				// it to one.
				projectDirs: (this.flags.projectdir as string[]).map(p => path.resolve(p))
			};
			if (this.flags['rule-thread-count'] != null) {
				sfgeConfig.ruleThreadCount = this.flags['rule-thread-count'] as number;
			}
			if (this.flags['rule-thread-timeout'] != null) {
				sfgeConfig.ruleThreadTimeout = this.flags['rule-thread-timeout'] as number;
			}
			// Check the status of the flag first, since the flag being true should trump the environment variable's value.
			if (this.flags['ignore-parse-errors'] != null) {
				sfgeConfig.ignoreParseErrors = this.flags['ignore-parse-errors'] as boolean;
			} else if (SFGE_IGNORE_PARSE_ERRORS in process.env && process.env.SFGE_IGNORE_PARSE_ERRORS.toLowerCase() === 'true') {
				sfgeConfig.ignoreParseErrors = true;
			}
			options.set(CUSTOM_CONFIG.SfgeConfig, JSON.stringify(sfgeConfig));
		}
		return options;
	}

	private gatherEngineOptions(): Map<string, string> {
		const options = this.gatherBaseEngineOptions();
		this.gatherCommandEngineOptions(options);
		return options;
	}

	/**
	 * Gather a map of options that will be passed to the RuleManager without validation.
	 * @protected
	 * @abstract
	 */
	protected abstract gatherCommandEngineOptions(partialOptions: Map<string,string>): Map<string, string>;

	protected abstract pathBasedEngines(): boolean;
}
