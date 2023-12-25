import {OutputFormatter, Results} from "./Results";
import {FormattedOutput, ResultTableRow, RuleResult} from "../../types";
import * as path from "path";
import * as wrap from "word-wrap";
import {isPathlessViolation} from "../util/Utils";
import {Ux} from "@salesforce/sf-plugins-core";

const BASE_COLUMNS: Ux.Table.Columns<ResultTableRow> = {
	description: {},
	category: {},
	url: {
		header: "URL"
	}
};

export const PATHLESS_COLUMNS: Ux.Table.Columns<ResultTableRow> = {
	location: {},
	...BASE_COLUMNS
};

const DFA_COLUMNS: Ux.Table.Columns<ResultTableRow> = {
	sourceLocation: {
		header: 'Source Location'
	},
	sinkLocation: {
		header: 'Sink Location'
	},
	...BASE_COLUMNS
};


// TODO: This is the only formatter that doesn't return a string. We may want to revisit this at some point.
export class TableOutputFormatter implements OutputFormatter {
	public format(results: Results): Promise<FormattedOutput> {
		const ruleResults: RuleResult[] = results.getRuleResults();

		const columns = results.violationsAreDfa()
			? DFA_COLUMNS
			: PATHLESS_COLUMNS

		// Build the rows.
		const rows: ResultTableRow[] = [];
		for (const result of ruleResults) {
			const fileName = result.fileName;
			for (const violation of result.violations) {
				const message = violation.message.trim();
				const relativeFile = path.relative(process.cwd(), fileName);
				// Instantiate our Row object.
				const baseRow: ResultTableRow = {
					description: wrap(message),
					category: violation.category,
					url: violation.url
				};
				// A pathless violation can be trivially converted into a row.
				if (isPathlessViolation(violation)) {
					rows.push({
						...baseRow,
						location: `${relativeFile}:${violation.line}`
					});
				} else if (!(violation.sinkFileName)) {
					// If the violation is path-based but has no sink file, then the violation indicates an error of some
					// kind. So use the source information we were given, but omit sink information.
					rows.push({
						...baseRow,
						sourceLocation: `${relativeFile}:${violation.sourceLine}`
					});
				} else {
					const relativeSinkFile = path.relative(process.cwd(), violation.sinkFileName);
					rows.push({
						...baseRow,
						sourceLocation: `${relativeFile}:${violation.sourceLine}`,
						sinkLocation: `${relativeSinkFile}:${violation.sinkLine}`
					});
				}
			}
		}
		return Promise.resolve({columns, rows});
	}
}
