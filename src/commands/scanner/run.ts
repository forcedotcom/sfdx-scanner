import {flags} from '@salesforce/command';
import {Messages, SfdxError} from '@salesforce/core';
import {AnyJson} from '@salesforce/ts-types';
import {LooseObject, RecombinedRuleResults} from '../../types';
import {AllowedEngineFilters, INTERNAL_ERROR_CODE} from '../../Constants';
import {Controller} from '../../Controller';
import {CUSTOM_CONFIG} from '../../Constants';
import {OUTPUT_FORMAT} from '../../lib/RuleManager';
import {ScannerCommand} from '../../lib/ScannerCommand';
import {TYPESCRIPT_ENGINE_OPTIONS} from '../../lib/eslint/TypescriptEslintStrategy';
import fs = require('fs');
import untildify = require('untildify');
import normalize = require('normalize-path');

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run');

export default class Run extends ScannerCommand {
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
			options: [...AllowedEngineFilters]
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
			options: [OUTPUT_FORMAT.CSV, OUTPUT_FORMAT.HTML, OUTPUT_FORMAT.JSON, OUTPUT_FORMAT.JUNIT, OUTPUT_FORMAT.TABLE, OUTPUT_FORMAT.XML]
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
		"violations-cause-error": flags.boolean({
			char: 'v',
			description: messages.getMessage('flags.vceDescription'),
			longDescription: messages.getMessage('flags.vceDescriptionLong'),
			exclusive: ['json']
		})
	};

	public async run(): Promise<AnyJson> {
		// First, we need to do some input validation that's a bit too sophisticated for the out-of-the-box flag validations.
		this.validateFlags();

		// Next, we need to build our input.
		const filters = this.buildRuleFilters();

		// We need to derive the output format, either from information that was explicitly provided or from default values.
		// We can't use the defaultValue property for the flag, because there needs to be a difference between defaulting
		// to a value and having the user explicitly select it.
		const format: OUTPUT_FORMAT = this.determineOutputFormat();
		const ruleManager = await Controller.createRuleManager();

		// Turn the paths into normalized Unix-formatted paths and strip out any single- or double-quotes, because
		// sometimes shells are stupid and will leave them in there.
		const target = this.flags.target || [];
		const targetPaths = target.map(path => normalize(untildify(path)).replace(/['"]/g, ''));
		const engineOptions = this.gatherEngineOptions();
		let output: RecombinedRuleResults = null;
		try {
			output = await ruleManager.runRulesMatchingCriteria(filters, targetPaths, format, engineOptions);
		} catch (e) {
			// Rethrow any errors as SFDX errors.
			throw new SfdxError(e.message || e, null, null, this.getInternalErrorCode());
		}
		return this.processOutput(output);
	}

	/**
	 * Gather a map of options that will be passed to the RuleManager without validation.
	 */
	private gatherEngineOptions(): Map<string, string> {
		const options = new Map();
		if (this.flags.tsconfig) {
			const tsconfig = normalize(untildify(this.flags.tsconfig));
			options.set(TYPESCRIPT_ENGINE_OPTIONS.TSCONFIG, tsconfig);
		}

		// TODO: This fix for W-7791882 is suboptimal, because it leaks our abstractions and pollutes the command with
		//  engine-specific flags. Replace it in 3.0.
		if (this.flags.env) {
			try {
				const parsedEnv: LooseObject = JSON.parse(this.flags.env);
				options.set('env', JSON.stringify(parsedEnv));
			} catch (e) {
				throw new SfdxError(messages.getMessage('output.invalidEnvJson'), null, null, this.getInternalErrorCode());
			}
		}

		// Capturing eslintconfig value, if provided
		if (this.flags.eslintconfig) {
			const eslintConfig = normalize(untildify(this.flags.eslintconfig));
			options.set(CUSTOM_CONFIG.EslintConfig, eslintConfig);
		}

		// Capturing pmdconfig value, if provided
		if (this.flags.pmdconfig) {
			const pmdConfig = normalize(untildify(this.flags.pmdconfig));
			options.set(CUSTOM_CONFIG.PmdConfig, pmdConfig);
		}
		return options;
	}

	private validateFlags(): void {
		if (this.flags.tsconfig && this.flags.eslintconfig) {
			throw SfdxError.create('@salesforce/sfdx-scanner', 'run', 'validations.tsConfigEslintConfigExclusive', []);
		}

		if ((this.flags.pmdconfig || this.flags.eslintconfig) && (this.flags.category || this.flags.ruleset)) {
			this.ux.log(messages.getMessage('output.filtersIgnoredCustom', []));
		}

		// Be liberal with the user, but do log an info message if they choose a file extension that does not match their format.
		if (this.flags.format && this.flags.outfile) {
			const derivedFormat = this.deriveFormatFromOutfile();
			// For validation purposes, treat junit as xml.
			const chosenFormat = this.flags.format == 'junit' ? 'xml' : this.flags.format;
			if (derivedFormat !== chosenFormat) {
				this.ux.log(messages.getMessage('validations.outfileFormatMismatch', [this.flags.format, derivedFormat]));
			}
		}
	}

	private determineOutputFormat(): OUTPUT_FORMAT {
		// If an output format is explicitly specified, use that.
		if (this.flags.format) {
			return this.flags.format;
		} else if (this.flags.outfile) {
			// Else If an outfile is explicitly specified, infer the format from its extension.
			return this.deriveFormatFromOutfile();
		} else if (this.flags.json) {
			// Else If the --json flag is present, then we'll default to JSON format.
			return OUTPUT_FORMAT.JSON;
		} else {
			// Otherwise, we'll default to the Table format.
			return OUTPUT_FORMAT.TABLE;
		}
	}

	private deriveFormatFromOutfile(): OUTPUT_FORMAT {
		const outfile = this.flags.outfile;
		const lastPeriod = outfile.lastIndexOf('.');
		if (lastPeriod < 1 || lastPeriod + 1 === outfile.length) {
			throw new SfdxError(messages.getMessage('validations.outfileMustBeValid'), null, null, this.getInternalErrorCode());
		} else {
			const fileExtension = outfile.slice(lastPeriod + 1);
			switch (fileExtension) {
				case OUTPUT_FORMAT.CSV:
				case OUTPUT_FORMAT.HTML:
				case OUTPUT_FORMAT.JSON:
				case OUTPUT_FORMAT.XML:
					return fileExtension;
				default:
					throw new SfdxError(messages.getMessage('validations.outfileMustBeSupportedType'), null, null, this.getInternalErrorCode());
			}
		}
	}

	private processOutput(rrr: RecombinedRuleResults): AnyJson {
		const {minSev, summaryMap, results} = rrr;
		// If the results were an empty string, then we found no violations.
		if (results === '') {
			const msg = messages.getMessage('output.noViolationsDetected', [[...summaryMap.keys()].join(', ')]);
			this.ux.log(msg);
			return msg;
		}
		// Part of processing the results is determining what we need to log to the console.
		let outputPieces: string[] = this.buildRunSummaryMessage(rrr);
		// We surface violations to the user by writing them to the console or a file. These methods do that as a side effect,
		// and return the message we should display to the user at the end.
		outputPieces = [...outputPieces, this.flags.outfile ? this.writeToOutfile(results) : this.writeToConsole(results)];

		// Now we can create our message by joining all of our pieces, then display it to the user by either logging
		// it to the console or throwing it as an exception.
		const msg = outputPieces.join('\n').trim();
		if (minSev > 0 && this.flags['violations-cause-error']) {
			throw new SfdxError(msg, null, null, minSev);
		} else {
			this.ux.log(msg);
		}

		// Finally, we need to return something for use by the --json flag.
		if (this.flags.outfile) {
			// If we used an outfile, we should just return the summary message, since that says where the file is.
			return msg;
		} else if (typeof results === 'string') {
			// If the specified output format was JSON, then the results are a huge stringified JSON that we should parse
			// before returning. Otherwise, we should just return the result string.
			return this.determineOutputFormat() === OUTPUT_FORMAT.JSON ? JSON.parse(results) : results;
		} else {
			// If the results are a JSON, return the `rows` property, since that's all of the data that would be displayed
			// in the table.
			return results.rows;
		}
	}

	private getInternalErrorCode(): number {
		return this.flags['violations-cause-error'] ? INTERNAL_ERROR_CODE : 1;
	}

	private buildRunSummaryMessage(rrr: RecombinedRuleResults): string[] {
		const {summaryMap, minSev} = rrr;
		let msgPieces: string[] = [];
		// Until we decide how we ultimately want to handle W-8388246's run summary message, we'll just have this
		// always be skipped.
		if (this.flags['no-such-flag']) {
			const engineSummaries = [...summaryMap.entries()]
				.map(([engine, summary]) => {
					return messages.getMessage('output.engineSummaryTemplate', [engine, summary.violationCount, summary.fileCount]);
				});
			msgPieces = [...msgPieces, ...engineSummaries];
		}
		// If we're supposed to throw an exception for violations, we need to add an extra sentence to the summary message.
		if (minSev > 0 && this.flags['violations-cause-error']) {
			msgPieces.push(`${messages.getMessage('output.sevDetectionSummary', [minSev])}`);
		}
		return msgPieces;
	}

	private writeToOutfile(results: string | {columns; rows}): string {
		try {
			fs.writeFileSync(this.flags.outfile, results);
		} catch (e) {
			// Rethrow any errors.
			throw new SfdxError(e.message || e, null, null, this.getInternalErrorCode());
		}
		// Return a message indicating the action we took.
		return messages.getMessage('output.writtenToOutFile', [this.flags.outfile]);
	}

	private writeToConsole(results: string | {columns; rows}): string {
		// Figure out what format we need.
		const format: OUTPUT_FORMAT = this.determineOutputFormat();
		// Prepare the format mismatch message in case we need it later.
		const msg = `Invalid combination of format ${format} and output type ${typeof results}`;
		switch (format) {
			case OUTPUT_FORMAT.JSON:
			case OUTPUT_FORMAT.CSV:
			case OUTPUT_FORMAT.XML:
			case OUTPUT_FORMAT.JUNIT:
			case OUTPUT_FORMAT.HTML:
				// All of these formats should be represented as giant strings.
				if (typeof results !== 'string') {
					throw new SfdxError(msg, null, null, this.getInternalErrorCode());
				}
				// We can just dump those giant strings to the console without anything special.
				this.ux.log(results);
				break;
			case OUTPUT_FORMAT.TABLE:
				// This format should be a JSON with a `columns` property and a `rows` property, i.e. NOT a string.
				if (typeof results === 'string') {
					throw new SfdxError(msg, null, null, this.getInternalErrorCode());
				}
				this.ux.table(results.rows, results.columns);
				break;
			default:
				throw new SfdxError(msg, null, null, this.getInternalErrorCode());
		}
		// When we actually figure out a solution for W-8388246, we can have this start returning a meaningful message.
		// Until then, an empty string is fine.
		return '';
	}
}
