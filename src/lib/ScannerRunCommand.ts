import {flags} from '@salesforce/command';
import {Messages, SfError} from '@salesforce/core';
import {AnyJson} from '@salesforce/ts-types';
import {ScannerCommand} from './ScannerCommand';
import {RecombinedRuleResults, SfgeConfig} from '../types';
import {RunOutputProcessor} from './util/RunOutputProcessor';
import {Controller} from '../Controller';
import {CUSTOM_CONFIG} from '../Constants';
import {OUTPUT_FORMAT, RunOptions} from './RuleManager';
import {FileHandler} from './util/FileHandler';
import untildify = require('untildify');
import globby = require('globby');
import path = require('path');
import normalize = require('normalize-path');

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run-common');
// This code is used for internal errors.
export const INTERNAL_ERROR_CODE = 1;

export abstract class ScannerRunCommand extends ScannerCommand {

	/**
	 * There are flags that are common to all variants of the run command. We can define those flags
	 * here to avoid duplicate code.
	 * @protected
	 */
	protected static flagsConfig = {
		verbose: flags.builtin(),
		// BEGIN: Filter-related flags.
		category: flags.array({
			char: 'c',
			description: messages.getMessage('flags.categoryDescription'),
			longDescription: messages.getMessage('flags.categoryDescriptionLong')
		}),
		// BEGIN: Flags related to results processing.
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
		// END: Flags related to results processing.
		// BEGIN: Flags related to targeting.
		projectdir: flags.array({
			char: 'p',
			description: messages.getMessage('flags.projectdirDescription'),
			longDescription: messages.getMessage('flags.projectdirDescriptionLong'),
			// eslint-disable-next-line @typescript-eslint/no-unsafe-argument
			map: d => normalize(untildify(d))
		}),
		// END: Flags related to targeting.
	};

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
			withExperimental: this.flags['with-experimental'],
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
			throw new SfError(message, null, null, INTERNAL_ERROR_CODE);
		}

		return new RunOutputProcessor({
			format: runOptions.format,
			severityForError: this.flags['severity-threshold'] as number,
			outfile: this.flags.outfile as string
		}, this.ux)
			.processRunOutput(output);
	}

	private async validateFlags(): Promise<void> {
		// First, validate the flags specific to the sub-variant.
		await this.validateVariantFlags();

		// Then, validate the flags that are common to all variants.
		await this.validateCommonFlags();
	}

	/**
	 * Validate whatever flags are specific to a given implementation of {@link ScannerRunCommand}.
	 * @protected
	 * @abstract
	 */
	protected abstract validateVariantFlags(): Promise<void>


	private async validateCommonFlags(): Promise<void> {
		const fh = new FileHandler();
		// If there's a --projectdir flag, its entries must be non-glob paths pointing
		// to existing directories.
		if (this.flags.projectdir) {
			for (const dir of (this.flags.projectdir as string[])) {
				if (globby.hasMagic(dir)) {
					throw new SfError(messages.getMessage('validations.projectdirCannotBeGlob', []));
				} else if (!(await fh.exists(dir))) {
					throw new SfError(messages.getMessage('validations.projectdirMustExist', []));
				} else if (!(await fh.stats(dir)).isDirectory()) {
					throw new SfError(messages.getMessage('validations.projectdirMustBeDir', []));
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
				throw new SfError(messages.getMessage('validations.cannotWriteTableToFile', []));
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
			throw new SfError(messages.getMessage('validations.outfileMustBeValid'), null, null, INTERNAL_ERROR_CODE);
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
					throw new SfError(messages.getMessage('validations.outfileMustBeSupportedType'), null, null, INTERNAL_ERROR_CODE);
			}
		}
	}

	/**
	 * Gather a map of options that will be passed to the RuleManager without validation.
	 * @protected
	 */
	protected gatherEngineOptions(): Map<string, string> {
		const options: Map<string,string> = this.gatherCommonEngineOptions();
		this.mergeVariantEngineOptions(options);
		return options;
	}

	/**
	 * Gather engine options that are shared across sub-variants.
	 * @private
	 */
	private gatherCommonEngineOptions(): Map<string,string> {
		const options: Map<string,string> = new Map();
		// We should only add a GraphEngine config if we were given a --projectdir flag.
		if (this.flags.projectdir && (this.flags.projectdir as string[]).length > 0) {
			const sfgeConfig: SfgeConfig = {
				projectDirs: (this.flags.projectdir as string[]).map(p => path.resolve(p))
			};
			options.set(CUSTOM_CONFIG.SfgeConfig, JSON.stringify(sfgeConfig));
		}
		return options;
	}

	/**
	 * Gather engine options that are unique to each sub-variant.
	 * @protected
	 * @abstract
	 */
	protected abstract mergeVariantEngineOptions(commonOptions: Map<string,string>): void;

	protected abstract pathBasedEngines(): boolean;
}
