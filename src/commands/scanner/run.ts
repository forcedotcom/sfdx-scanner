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
		const {minSev, results} = rrr;
		// If the results were an empty string, it means we found no violations.
		if (results === '') {
			// We can just log a message to the console, and also return it for the --json route.
			const msg = messages.getMessage('output.noViolationsDetected');
			this.ux.log(msg);
			return msg;
		} else if (this.flags.outfile) {
			return this.writeToOutfile(minSev, results);
		} else {
			return this.formatAndDisplayOutput(minSev, results);
		}
	}

	private getInternalErrorCode(): number {
		return this.flags['violations-cause-error'] ? INTERNAL_ERROR_CODE : 1;
	}

	private writeToOutfile(minSev: number, results: string | {columns; rows}): AnyJson {
		try {
			fs.writeFileSync(this.flags.outfile, results)
		} catch (e) {
			// Rethrow any errors.
			throw new SfdxError(e.message || e, null, null, this.getInternalErrorCode());
		}
		// Afterwards, we need to build a message saying that we wrote to the correct file.
		const outfileMsg = messages.getMessage('output.writtenToOutFile', [this.flags.outfile]);
		if (minSev > 0 && this.flags['violations-cause-error']) {
			// If the user gave us the flag to throw errors when we find violations, we should prefix the message with
			// one about the errors we found, and throw the whole thing as an exception.
			const errMsg = messages.getMessage('output.sevDetectionSummary', [minSev]) + ' ' + outfileMsg;
			throw new SfdxError(errMsg, null, null, minSev);
		} else {
			// Otherwise, we can just log the message, then return it for the --json route.
			this.ux.log(outfileMsg);
			return outfileMsg;
		}
	}

	private formatAndDisplayOutput(minSev: number, results: string | {columns; rows}): AnyJson {
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
		// Now that we've displayed the results, we need to figure out what to return. If the flag for throwing an error
		// in response to violations is present, we'll need to do that. Otherwise, we need to return some value to be used
		// by the --json flag.
		if (this.flags['violations-cause-error'] && minSev > 0) {
			// When the error flag is active, we need to throw an error. So generate the message and throw it.
			const errMsg = messages.getMessage('output.sevDetectionSummary', [minSev])
				+ ' ' + messages.getMessage('output.pleaseSeeAbove');
			throw new SfdxError(errMsg, null, null, minSev);
		} else if (typeof results === 'string') {
			// If the specified output format was JSON, then the results are a huge stringified JSON that we should parse
			// and return. Otherwise we should just return the result string.
			return format === OUTPUT_FORMAT.JSON ? JSON.parse(results) : results;
		} else {
			// If the results are a JSON, return the `rows` property, since that's all of the data that would be displayed
			// in the table.
			return results.rows;
		}
	}
}
