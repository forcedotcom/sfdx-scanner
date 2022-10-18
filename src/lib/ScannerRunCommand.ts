import {Messages, SfdxError} from '@salesforce/core';
import {AnyJson} from '@salesforce/ts-types';
import {ScannerCommand} from './ScannerCommand';
import {RecombinedRuleResults} from '../types';
import {RunOutputProcessor} from './util/RunOutputProcessor';
import {Controller} from '../Controller';
import {OUTPUT_FORMAT, RunOptions} from './RuleManager';
import untildify = require('untildify');
import normalize = require('normalize-path');

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run');
// This code is used for internal errors.
export const INTERNAL_ERROR_CODE = 1;

export abstract class ScannerRunCommand extends ScannerCommand {

	async runInternal(): Promise<AnyJson> {
		// First, do any validations that can't be handled with out-of-the-box stuff.
		await this.validateFlags();

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
		if (target.length === -0) {
			console.log('beep');
		}
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

		// The output flags are common between subclasses. Validate those too.
		this.validateOutputFlags();
	}

	/**
	 * Validate whatever flags are specific to a given implementation of {@link ScannerRunCommand}.
	 * @protected
	 * @abstract
	 */
	protected abstract validateCommandFlags(): Promise<void>

	/**
	 * Validate the output-related flags, which are common to all implementations of {@link ScannerRunCommand} and share
	 * the same constraints.
	 * @private
	 */
	private validateOutputFlags(): void {
		// If the user explicitly specified both a format and an outfile, we need to do a bit of validation there.
		if (this.flags.format && this.flags.outfile) {
			const inferredOutfileFormat = this.inferFormatFromOutfile();
			// For the purposes of this validation, we treat junit as xml.
			const chosenFormat = this.flags.format === 'junit' ? 'xml' : this.flags.format as string;
			// If the chosen format is TABLE, we immediately need to exit. There's no way to sensibly write the output
			// of TABLE to a file.
			if (chosenFormat === OUTPUT_FORMAT.TABLE) {
				throw SfdxError.create('@salesforce/sfdx-scanner', 'run', 'validations.cannotWriteTableToFile', []);
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

	/**
	 * Gather a map of options that will be passed to the RuleManager without validation.
	 * @protected
	 * @abstract
	 */
	protected abstract gatherEngineOptions(): Map<string, string>;

	protected abstract pathBasedEngines(): boolean;
}
