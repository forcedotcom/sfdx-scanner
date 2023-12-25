import {AnyJson} from '@salesforce/ts-types';
import {SfError} from '@salesforce/core';
import fs = require('fs');
import {RecombinedRuleResults, FormattedOutput} from '../../types';
import {BundleName, getMessage} from "../../MessageCatalog";
import {Display} from "../Display";
import {INTERNAL_ERROR_CODE} from "../../Constants";
import {OutputFormat} from "../output/OutputFormat";

export type RunOutputOptions = {
	format: OutputFormat;
	severityForError?: number;
	outfile?: string;
}

export class RunOutputProcessor {
	private readonly display: Display;
	private readonly opts: RunOutputOptions;

	public constructor(display: Display, opts: RunOutputOptions) {
		this.display = display;
		this.opts = opts;
	}

	public processRunOutput(rrr: RecombinedRuleResults): AnyJson {
		const {minSev, results, summaryMap} = rrr;

		const hasViolations = [...summaryMap.values()].some(summary => summary.violationCount !== 0);

		// If there are neither violations nor an outfile, then we want to avoid writing empty
		// results to the console.
		// NOTE: If there's an outfile, we skip this part. This is because we still want to generate
		//       an empty outfile
		if (!this.opts.outfile && !hasViolations) {
			// Build a message indicating which engines were run...
			const msg = getMessage(BundleName.RunOutputProcessor, 'output.noViolationsDetected', [[...summaryMap.keys()].join(', ')]);
			// ...log it to the console...
			this.display.displayInfo(msg);
			// ...and return it for use with the --json flag.
			return msg;
		}

		// If we have violations (or an outfile but no violations), we'll build an array of
		// message parts, and then log them all at the end.
		let msgComponents: string[] = [];
		// We need a summary of the information we were provided (blank/empty if no violations).
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
			throw new SfError(msg, null, null, minSev);
		} else if (msg && msg.length > 0) {
			// No sense logging an empty message.
			this.display.displayInfo(msg);
		}

		// Finally, we need to return something for use by the --json flag.
		if (this.opts.outfile) {
			// If we used an outfile, we should just return the summary message, since that says where the file is.
			return msg;
		} else if (typeof results === 'string') {
			// If the specified output format was JSON, then the results are a huge stringified JSON that we should parse
			// before returning. Otherwise, we should just return the result string.
			return this.opts.format === OutputFormat.JSON ? JSON.parse(results) as AnyJson : results;
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
		if ((this.opts.format === OutputFormat.TABLE) || this.opts.outfile) {
			const summaryMsgs = [...summaryMap.entries()]
				.map(([engine, summary]) => {
					return getMessage(BundleName.RunOutputProcessor, 'output.engineSummaryTemplate', [engine, summary.violationCount, summary.fileCount]);
				});
			msgParts = [...msgParts, ...summaryMsgs];
		}
		// If we're supposed to throw an exception in response to violations, we need an extra piece of summary.
		// Summary to print with --severity-threshold flag
		if (this.shouldErrorForSeverity(minSev, this.opts.severityForError)) {
			msgParts.push(getMessage(BundleName.RunOutputProcessor, 'output.sevThresholdSummary', [this.opts.severityForError]));
		}

		return msgParts;
	}

	private writeToOutfile(results: string | {columns; rows}): string {
		try {
			// At this point, we can cast `results` to a string, since it being an object would indicate that the format
			// is `table`, and we already have validations preventing tables from being written to files.
			fs.writeFileSync(this.opts.outfile, results as string);
		} catch (e) {
			// Rethrow any errors as SfdxErrors.
			const message: string = e instanceof Error ? e.message : e as string;
			throw new SfError(message, null, null, INTERNAL_ERROR_CODE);
		}
		// Return a message indicating the action we took.
		return getMessage(BundleName.RunOutputProcessor, 'output.writtenToOutFile', [this.opts.outfile]);
	}

	private writeToConsole(results: FormattedOutput): string {
		// Figure out what format we need.
		const format: OutputFormat = this.opts.format;
		// Prepare the format mismatch message in case we need it later.
		const msg = `Invalid combination of format ${format} and output type ${typeof results}`;
		switch (format) {
			case OutputFormat.CSV:
			case OutputFormat.HTML:
			case OutputFormat.JSON:
			case OutputFormat.JUNIT:
			case OutputFormat.SARIF:
			case OutputFormat.XML:
				// All of these formats should be represented as giant strings.
				if (typeof results !== 'string') {
					throw new SfError(msg, null, null, INTERNAL_ERROR_CODE);
				}
				// We can just dump those giant strings to the console without anything special.
				this.display.displayInfo(results);
				break;
			case OutputFormat.TABLE:
				// This format should be a JSON with a `columns` property and a `rows` property, i.e. NOT a string.
				if (typeof results === 'string') {
					throw new SfError(msg, null, null, INTERNAL_ERROR_CODE);
				}
				this.display.displayTable(results.rows, results.columns);
				break;
			default:
				throw new SfError(msg, null, null, INTERNAL_ERROR_CODE);
		}
		// If the output format is table, then we should return a message indicating that the output was logged above.
		// Otherwise, just return an empty string so the output remains machine-readable.
		return format === OutputFormat.TABLE ? getMessage(BundleName.RunOutputProcessor, 'output.writtenToConsole') : '';
	}
}
