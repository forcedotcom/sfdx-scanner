import {SfdxError} from '@salesforce/core';
import * as path from 'path';
import {RuleResult} from '../types';
import {PmdEngine} from './pmd/PmdEngine';
import {OUTPUT_FORMAT} from './RuleManager';

export class RuleResultRecombinator {

	public static recombineAndReformatResults(results: RuleResult[], format: OUTPUT_FORMAT): string | { columns; rows } {
		// We need to change the results we were given into the desired final format.
		switch (format) {
			case OUTPUT_FORMAT.CSV:
				return this.constructCsv(results);
			case OUTPUT_FORMAT.XML:
				return this.constructXml(results);
			case OUTPUT_FORMAT.JUNIT:
				return this.constructJunit(results);
			case OUTPUT_FORMAT.TABLE:
				return this.constructTable(results);
			default:
				throw new SfdxError('Unrecognized output format.');
		}
	}

	private static constructXml(results: RuleResult[]): string {
		let resultXml = ``;

		// If the results were just an empty string, we can return it.
		if (results.length === 0) {
			return resultXml;
		}

		let problemCount = 0;

		// Iterate through the results to create an XML string format:
		// <results...>
		//   <result...>
		//     <violation severity="1" line="4" column="7" ...>Message</violation>
		//     <violation severity="2" line="5" column="8" ...>Message</violation>
		//     <violation severity="3" line="6" column="9" ...>Message</violation>
		//     ...
		//   </result>
		// </results>
		for (const result of results) {
			const from = process.cwd();
			const fileName = path.relative(from, result.fileName);
			let violations = '';
			for (const v of result.violations) {
				problemCount++;
				violations += `
            <violation severity="${v.severity}" line="${v.line}" column="${v.column}" endLine="${v.endLine}" endColumn="${v.endColumn}" rule="${v.ruleName}" category="${v.category}">
${v.message.trim()}
            </violation>`;
			}
			resultXml += `
      <result file="${fileName}">
          ${violations}
      </result>`;
		}

		return `<results total="${results.length}" totalViolations="${problemCount}">
        ${resultXml}
</results>`;
	}

	private static constructJunit(results: RuleResult[]): string {
		let junitXml = ``;
		// If the results were just an empty string, we can return it.
		if (results.length === 0) {
			return junitXml;
		}
		let problemCount = 0;

		for (const result of results) {
			const fileName = result.fileName;
			let failures = '';
			for (const v of result.violations) {
				problemCount++;
				const msg = v.message.trim();
				failures += `
            <failure message="${fileName}: ${v.line} ${msg}" type="${v.severity}">
${v.severity}: ${msg}
Category: ${v.category} - ${v.ruleName}
File: ${fileName}
Line: ${v.line}
Column: ${v.column}
            </failure>`;
			}
			junitXml += `
      <testcase id="${fileName}" name="${fileName}">
          ${failures}
      </testcase>`;
		}

		return `<testsuites tests="${results.length}" failures="${problemCount}">
    <testsuite tests="${results.length}" failures="${problemCount}">
        ${junitXml}
    </testsuite>
</testsuites>`;
	}

	private static constructTable(results: RuleResult[]): { columns; rows } | string {
		// If the results were just an empty string, we can return it.
		if (results.length === 0) {
			return '';
		}

		const columns = ["Location", "Description", "Rule", "Category", "Severity", "Line", "Column", "Engine"];
		const rows = [];
		for (const result of results) {
			const fileName = result.fileName;
			for (const v of result.violations) {
				const msg = v.message.trim();
				const relativeFile = path.relative(process.cwd(), fileName);
				rows.push({
					Location: `${relativeFile}:${v.line}`,
					Description: msg,
					Rule: v.ruleName,
					Category: v.category,
					Severity: v.severity,
					Line: v.line,
					Column: v.column,
					Engine: result.engine
				});
			}
		}
		// Turn our JSON into a string so we can pass it back up through and parse it when we need.
		return {columns, rows};
	}

	private static constructCsv(results: RuleResult[]): string {
		// If the results were just an empty string, we can return it.
		if (results.length === 0) {
			return '';
		}

		const engine = PmdEngine.NAME;

		// Gradually build our CSV, starting with these columns.
		let csvString = '"Problem","File","Severity","Line","Column","Description","Rule","Category","Engine"\n';
		let problemCount = 0;

		for (const result of results) {
			const fileName = result.fileName;
			for (const v of result.violations) {
				// Since we are creating CSVs, make sure we escape any commas in our violation messages.
				// Just replace with semi-colon.
				const msg = v.message.trim().replace(",", ";");
				const row = [++problemCount, fileName, v.severity, v.line, v.column, msg, v.ruleName, v.category, engine];
				csvString += '"' + row.join('","') + '"\n';
			}
		}
		return csvString;
	}
}

