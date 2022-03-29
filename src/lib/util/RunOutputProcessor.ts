import {UX} from '@salesforce/command';
import {AnyJson} from '@salesforce/ts-types';
import {Messages, SfdxError} from '@salesforce/core';
import fs = require('fs');

import {RecombinedRuleResults, RecombinedData} from '../../types';
import {OUTPUT_FORMAT} from '../RuleManager';

Messages.importMessagesDirectory(__dirname);

const runMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run');
const INTERNAL_ERROR_CODE = 1;

export type RunOutputOptions = {
	format: OUTPUT_FORMAT;
	severityForError?: number;
	outfile?: string;
}

export class RunOutputProcessor {
	private opts: RunOutputOptions;
	private ux: UX;

	public constructor(opts: RunOutputOptions, ux: UX) {
		this.opts = opts;
		this.ux = ux;
	}


	public processRunOutput(rrr: RecombinedRuleResults): AnyJson {
		const {minSev, summaryMap, results} = rrr;
		// If the results are an empty string, it means no violations were found.
		if (results === '') {
			// Build an appropriate message...
			const msg = runMessages.getMessage('output.noViolationsDetected', [[...summaryMap.keys()].join(', ')]);
			// ...log it to the console...
			this.ux.log(msg);
			// ...and return it for use with the --json flag.
			return msg;
		}

		// If we actually have violations, there's some stuff we need to do with them. We'll build an array of message parts,
		// and then log them all at the end.
		let msgComponents: string[] = [];
		// We need a summary of the information we were provided.
		msgComponents = [...msgComponents, ...this.buildRunSummaryMsgParts(rrr)];
		// We need to surface the results directly to the user, then add a message describing what we did.
		msgComponents.push(this.opts.outfile ? this.writeToOutfile(results) : this.writeToConsole(results));
		// Now we need to decide what to do with these messages. We'll either throw them as an exception or log them to
		// the user.
		msgComponents = msgComponents.filter(cmp => cmp && cmp.length > 0);
		const msg = msgComponents.join('\n');
		if (this.shouldErrorForSeverity(minSev, this.opts.severityForError)) {
			// We want to throw an error when the highest severity (smallest num) is
			// equal to or more severe (equal to or less than number-wise) than the inputted number
			throw new SfdxError(msg, null, null, minSev);
		} else if (msg && msg.length > 0) {
			// No sense logging an empty message.
			this.ux.log(msg);
		}

		// Finally, we need to return something for use by the --json flag.
		if (this.opts.outfile) {
			// If we used an outfile, we should just return the summary message, since that says where the file is.
			return msg;
		} else if (typeof results === 'string') {
			// If the specified output format was JSON, then the results are a huge stringified JSON that we should parse
			// before returning. Otherwise, we should just return the result string.
			return this.opts.format === OUTPUT_FORMAT.JSON ? JSON.parse(results) as AnyJson : results;
		} else {
			// If the results are a JSON, return the `rows` property, since that's all of the data that would be displayed
			// in the table.
			return results.rows;
		}
	}

	// determines if -s flag should cause an error
	private shouldErrorForSeverity(minSev: number, severityForError): boolean {
		if (severityForError === undefined) {
			return false; // flag not used
		}
		if (minSev === 0) {
			return false;
		}

		if (minSev <= this.opts.severityForError) {
			return true;
		}
		return false;
	}

	private buildRunSummaryMsgParts(rrr: RecombinedRuleResults): string[] {
		const {summaryMap, minSev} = rrr;
		let msgParts: string[] = [];

		// If we're outputting our results as a table, or we're writing the results to a file, then we'll want to output
		// a summary of what engines ran and what they found.
		if ((this.opts.format === OUTPUT_FORMAT.TABLE) || this.opts.outfile) {
			const summaryMsgs = [...summaryMap.entries()]
				.map(([engine, summary]) => {
					return runMessages.getMessage('output.engineSummaryTemplate', [engine, summary.violationCount, summary.fileCount]);
				});
			msgParts = [...msgParts, ...summaryMsgs];
		}
		// If we're supposed to throw an exception in response to violations, we need an extra piece of summary.
		// Summary to print with --severity-threshold flag
		if (this.shouldErrorForSeverity(minSev, this.opts.severityForError)) {
			msgParts.push(runMessages.getMessage('output.sevThresholdSummary', [this.opts.severityForError]));
		}

		return msgParts;
	}

	private writeToOutfile(results: string | {columns; rows}): string {
		try {
			fs.writeFileSync(this.opts.outfile, results);
		} catch (e) {
			// Rethrow any errors as SfdxErrors.
			const message: string = e instanceof Error ? e.message : e as string;
			throw new SfdxError(message, null, null, INTERNAL_ERROR_CODE);
		}
		// Return a message indicating the action we took.
		return runMessages.getMessage('output.writtenToOutFile', [this.opts.outfile]);
	}

	private writeToConsole(results: RecombinedData): string {
		// Figure out what format we need.
		const format: OUTPUT_FORMAT = this.opts.format;
		// Prepare the format mismatch message in case we need it later.
		const msg = `Invalid combination of format ${format} and output type ${typeof results}`;
		switch (format) {
			case OUTPUT_FORMAT.CSV:
			case OUTPUT_FORMAT.HTML:
			case OUTPUT_FORMAT.JSON:
			case OUTPUT_FORMAT.JUNIT:
			case OUTPUT_FORMAT.SARIF:
			case OUTPUT_FORMAT.XML:
				// All of these formats should be represented as giant strings.
				if (typeof results !== 'string') {
					throw new SfdxError(msg, null, null, INTERNAL_ERROR_CODE);
				}
				// We can just dump those giant strings to the console without anything special.
				this.ux.log(results);
				break;
			case OUTPUT_FORMAT.TABLE:
				// This format should be a JSON with a `columns` property and a `rows` property, i.e. NOT a string.
				if (typeof results === 'string') {
					throw new SfdxError(msg, null, null, INTERNAL_ERROR_CODE);
				}
				this.ux.table(results.rows, results.columns);
				break;
			default:
				throw new SfdxError(msg, null, null, INTERNAL_ERROR_CODE);
		}
		// If the output format is table, then we should return a message indicating that the output was logged above.
		// Otherwise, just return an empty string so the output remains machine-readable.
		return format === OUTPUT_FORMAT.TABLE ? runMessages.getMessage('output.writtenToConsole') : '';
	}
}
