import {flags} from '@salesforce/command';
import {Messages, SfdxError} from '@salesforce/core';
import {AnyJson} from '@salesforce/ts-types';
import {LooseObject} from '../../types';
import {Controller} from '../../ioc.config';
import {OUTPUT_FORMAT} from '../../lib/RuleManager';
import {ScannerCommand} from './scannerCommand';
import {overrideDefaultEnv} from '../../lib/eslint/BaseEslintEngine';
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
		// These flags are how you choose which rules you're running.
		category: flags.array({
			char: 'c',
			description: messages.getMessage('flags.categoryDescription'),
			longDescription: messages.getMessage('flags.categoryDescriptionLong')
		}),
		ruleset: flags.array({
			char: 'r',
			description: messages.getMessage('flags.rulesetDescription'),
			longDescription: messages.getMessage('flags.rulesetDescriptionLong')
		}),
		// TODO: After implementing this flag, unhide it.
		rulename: flags.string({
			char: 'n',
			description: messages.getMessage('flags.rulenameDescription'),
			// If you're specifying by name, it doesn't make sense to let you specify by any other means.
			exclusive: ['category', 'ruleset', 'severity', 'exclude-rule'],
			hidden: true
		}),
		// TODO: After implementing this flag, unhide it.
		severity: flags.string({
			char: 's',
			description: messages.getMessage('flags.severityDescription'),
			hidden: true
		}),
		// TODO: After implementing this flag, unhide it.
		'exclude-rule': flags.array({
			description: messages.getMessage('flags.excluderuleDescription'),
			hidden: true
		}),
		// These flags are how you choose which files you're targeting.
		target: flags.array({
			char: 't',
			description: messages.getMessage('flags.targetDescription'),
			longDescription: messages.getMessage('flags.targetDescriptionLong'),
			// If you're specifying local files, it doesn't make much sense to let you specify anything else.
			exclusive: ['org']
		}),
		// TODO: After implementing this flag, unhide it.
		org: flags.string({
			char: 'a',
			description: messages.getMessage('flags.orgDescription'),
			// If you're specifying an org, it doesn't make sense to let you specify anything else.
			exclusive: ['target'],
			hidden: true
		}),
		// These flags modify how the process runs, rather than what it consumes.
		// TODO: After implementing this flag, unhide it.
		'suppress-warnings': flags.boolean({
			description: messages.getMessage('flags.suppresswarningsDescription'),
			hidden: true
		}),
		format: flags.enum({
			char: 'f',
			description: messages.getMessage('flags.formatDescription'),
			longDescription: messages.getMessage('flags.formatDescriptionLong'),
			options: [OUTPUT_FORMAT.JSON, OUTPUT_FORMAT.XML, OUTPUT_FORMAT.JUNIT, OUTPUT_FORMAT.CSV, OUTPUT_FORMAT.TABLE]
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
		// TODO: THIS FLAG WAS IMPLEMENTED FOR W-7791882, AND IT'S TERRIBLE. REPLACE IT DURING THE 3.0 RELEASE CYCLE.
		env: flags.string({
			description: messages.getMessage('flags.envDescription'),
			longDescription: messages.getMessage('flags.envDescriptionLong'),
			deprecated: {
				messageOverride: messages.getMessage('flags.envParamDeprecationWarning')
			}
		})
	};

	public async run(): Promise<AnyJson> {
		// First, we need to do some input validation that's a bit too sophisticated for the out-of-the-box flag validations.
		this.validateFlags();

		// We don't yet support running rules against an org, so we'll just throw an error for now.
		if (this.flags.org) {
			throw new SfdxError('Running rules against orgs is not yet supported');
		}

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
		if (this.args.file) {
			target.push(this.args.file);
		}
		const targetPaths = target.map(path => normalize(untildify(path)).replace(/['"]/g, ''));
		const engineOptions = this.gatherEngineOptions();
		const output = await ruleManager.runRulesMatchingCriteria(filters, targetPaths, format, engineOptions);
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
		return options;
	}

	private validateFlags(): void {
		// file, --target and --org are mutually exclusive, but they can't all be null.
		if (!this.args.file && !this.flags.target && !this.flags.org) {
			throw new SfdxError(messages.getMessage('validations.mustTargetSomething'));
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

		// TODO: THIS FIX FOR W-7791882 IS TERRIBLE. REPLACE IT DURING 3.0.
		if (this.flags.env) {
			// Try to parse the flag into a JSON.
			try {
				const overrideEnv: LooseObject = JSON.parse(this.flags.env);
				overrideDefaultEnv(overrideEnv);
			} catch (e) {
				throw new SfdxError(messages.getMessage('output.invalidEnvJson'));
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
			throw new SfdxError(messages.getMessage('validations.outfileMustBeValid'));
		} else {
			const fileExtension = outfile.slice(lastPeriod + 1);
			switch (fileExtension) {
				case OUTPUT_FORMAT.JSON:
					return OUTPUT_FORMAT.JSON;
				case OUTPUT_FORMAT.CSV:
					return OUTPUT_FORMAT.CSV;
				case OUTPUT_FORMAT.XML:
					return OUTPUT_FORMAT.XML;
				default:
					throw new SfdxError(messages.getMessage('validations.outfileMustBeSupportedType'));
			}
		}
	}

	private processOutput(output: string | {columns; rows}): AnyJson {
		// If the output is an empty string, it means no violations were found, and we should log that information to the console
		// so the user doesn't get confused.
		if (output === '') {
			const msg = messages.getMessage('output.noViolationsDetected');
			this.ux.log(msg);
			return msg;
		}
		if (this.flags.outfile) {
			// If we were given a file, we should write the output to that file.
			try {
				fs.writeFileSync(this.flags.outfile, output);
				const msg = messages.getMessage('output.writtenToOutFile', [this.flags.outfile]);
				this.ux.log(msg);
				return msg;
			} catch (e) {
				throw new SfdxError(e.message || e);
			}
		} else {
			// Default properly, again, as we did earlier.
			const format: OUTPUT_FORMAT = this.determineOutputFormat();
			// If we're just supposed to dump the output to the console, what precisely we do depends on the format.
			if (format === OUTPUT_FORMAT.JSON && typeof output === 'string') {
				// JSON is just one giant string that we can dump directly to the console. We'll want to parse it into
				// an object before we return it for the --json output, though.
				this.ux.log(output);
				return JSON.parse(output);
			} else if (format === OUTPUT_FORMAT.CSV && typeof output === 'string') {
				// Also just one giant string that we can dump directly to the console. This time, we'll want to return
				// the string for --json output.
				this.ux.log(output);
				return output;
			} else if ((format === OUTPUT_FORMAT.XML || format === OUTPUT_FORMAT.JUNIT) && typeof output === 'string') {
				// For XML, we can just dump it to the console. Again, return the string for --json.
				this.ux.log(output);
				return output;
			} else if (format === OUTPUT_FORMAT.TABLE && typeof output === 'object') {
				// For tables, don't even bother printing anything unless we have something to print. For this one, we'll
				// return the `columns` property, since that's a bunch of JSONs.
				this.ux.table(output.rows, output.columns);
				return output.rows;
			} else {
				throw new SfdxError(`Invalid combination of format ${format} and output type ${typeof output}`);
			}
		}
	}
}
