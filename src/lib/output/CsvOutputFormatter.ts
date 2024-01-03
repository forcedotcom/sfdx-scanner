import {FormattedOutput, RuleResult} from "../../types";
import {isPathlessViolation} from "../util/Utils";
import {stringify} from "csv-stringify";
import {OutputFormatter, Results} from "./Results";

export class CsvOutputFormatter implements OutputFormatter {
	public async format(results: Results): Promise<FormattedOutput> {
		const isDfa: boolean = results.violationsAreDfa();
		const ruleResults: RuleResult[] = results.getRuleResults();
		const normalizeSeverity: boolean = ruleResults[0]?.violations.length > 0 && !(ruleResults[0]?.violations[0].normalizedSeverity === undefined)

		const csvRows = [];
		// There will always be columns for the problem counter and the severity.
		const columns: string[] = ['Problem', 'Severity'];
		// Only include the normalized severity column if requested.
		if (normalizeSeverity) {
			columns.push('Normalized Severity');
		}
		// DFA violations and Pathless violations have different columns for their location data.
		if (isDfa) {
			columns.push('Source File', 'Source Line', 'Source Column', 'Source Type', 'Source Method', 'Sink File', 'Sink Line', 'Sink Column');
		} else {
			columns.push('File', 'Line', 'Column');
		}
		// Always have information about the rule.
		columns.push('Rule', 'Description', 'URL', 'Category', 'Engine');
		csvRows.push(columns);

		let problemCount = 0;
		for (const result of ruleResults) {
			const fileName = result.fileName;
			for (const v of result.violations) {
				const msg = v.message.trim();

				const row: (string|number)[] = [++problemCount, v.severity];
				if (normalizeSeverity) {
					row.push(v.normalizedSeverity);
				}
				if (isPathlessViolation(v)) {
					row.push(fileName, v.line, v.column);
				} else {
					row.push(fileName, v.sourceLine, v.sourceColumn, v.sourceType, v.sourceMethodName, v.sinkFileName, v.sinkLine, v.sinkColumn);
				}
				row.push(v.ruleName, msg, v.url, v.category, result.engine);
				csvRows.push(row);
			}
		}

		// Force all cells to have quotes
		const csvOptions = {
			quoted: true,
			quoted_empty: true
		};
		return new Promise((resolve, reject) => {
			stringify(csvRows, csvOptions,  (err, output) => {
				if (err) {
					reject(err);
				}
				resolve(output);
			});
		});
	}
}
