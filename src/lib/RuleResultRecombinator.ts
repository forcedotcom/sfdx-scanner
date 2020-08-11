import {SfdxError} from '@salesforce/core';
import * as path from 'path';
import {RuleResult, RuleViolation} from '../types';
import {OUTPUT_FORMAT} from './RuleManager';
import htmlEscaper = require('html-escaper');
import * as wrap from 'word-wrap';

export class RuleResultRecombinator {

	public static recombineAndReformatResults(results: RuleResult[], format: OUTPUT_FORMAT): string | { columns; rows } {
		// We need to change the results we were given into the desired final format.
		switch (format) {
			case OUTPUT_FORMAT.JSON:
				return this.constructJson(results);
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

		/*
		 Iterate through the results to create an XML string format:
		 <results...>
		   <result...>
		     <violation severity="1" line="4" column="7" ...>Message</violation>
		     <violation severity="2" line="5" column="8" ...>Message</violation>
		     <violation severity="3" line="6" column="9" ...>Message</violation>
		     ...
		   </result>
		 </results>
		*/
		for (const result of results) {
			const from = process.cwd();
			const fileName = path.relative(from, result.fileName);
			let violations = '';
			for (const v of result.violations) {
				problemCount++;
				violations += `
            <violation severity="${v.severity}" line="${v.line}" column="${v.column}" endLine="${v.endLine}" endColumn="${v.endColumn}" rule="${v.ruleName}" category="${v.category}" url="${v.url}">
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
		// If there are no results, we can just return an empty string.
		if (!results || results.length === 0) {
			return '';
		}

		// Otherwise, we'll need to start constructing our JUnit XML. To do that, we'll need a map from file names to
		// lists of the <failure> tags generated from violations found in the corresponding file.
		const violationsByFileName = new Map<string, string[]>();

		// Iterate over all of the results, convert them to a <failure> tag, and map that tag by the file name.
		for (const result of results) {
			const {fileName, violations} = result;
			const mappedViolations: string[] = violationsByFileName.get(fileName) || [];
			for (const violation of violations) {
				mappedViolations.push(this.violationJsonToJUnitTag(fileName, violation));
			}
			violationsByFileName.set(fileName, mappedViolations);
		}

		// Use each entry in the map to construct a <testsuite> tag.
		const testsuiteTags = [];
		for (const [fileName, failures] of violationsByFileName.entries()) {
			testsuiteTags.push(`<testsuite name="${fileName}" tests="${failures.length}" errors="${failures.length}">\n${failures.join('\n')}\n</testsuite>`);
		}

		return `<testsuites>\n${testsuiteTags.join('\n')}\n</testsuites>`
	}

	private static violationJsonToJUnitTag(fileName: string, violation: RuleViolation): string {
		const {
			message,
			line,
			severity,
			category,
			ruleName,
			column,
			url
		} = violation;
		// The silliness of the triple backslashes in the .replace() calls is because we need to escape the backslash so
		// it ends up in the output, AND the single/double quotes so the code compiles.
		return `<testcase name="${fileName}">
<failure message="${fileName}: ${line} ${htmlEscaper.escape(message.trim())}" type="${severity}">
${severity}: ${message.trim()}
Category: ${category} - ${ruleName}
File: ${fileName}
Line: ${line}
Column: ${column}
URL: ${url}
</failure>
</testcase>`;
	}

	private static constructTable(results: RuleResult[]): { columns; rows } | string {
		// If the results were just an empty string, we can return it.
		if (results.length === 0) {
			return '';
		}

		const columns = ["Location", "Description", "Category", "URL"];
		const rows = [];
		for (const result of results) {
			const fileName = result.fileName;
			for (const v of result.violations) {
				const msg = v.message.trim();
				const relativeFile = path.relative(process.cwd(), fileName);
				rows.push({
					Location: `${relativeFile}:${v.line}`,
					Rule: v.ruleName,
					Description: wrap(msg),
					URL: v.url,
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

	private static constructJson(results: RuleResult[]): string {
		if (results.length === 0) {
			return '';
		}
		return JSON.stringify(results.filter(r => r.violations.length > 0));
	}

	private static constructCsv(results: RuleResult[]): string {
		// If the results were just an empty string, we can return it.
		if (results.length === 0) {
			return '';
		}

		// Gradually build our CSV, starting with these columns.
		let csvString = '"Problem","File","Severity","Line","Column","Rule","Description","URL","Category","Engine"\n';
		let problemCount = 0;

		for (const result of results) {
			const fileName = result.fileName;
			for (const v of result.violations) {
				// Since we are creating CSVs, make sure we escape any commas in our violation messages.
				// Just replace with semi-colon.
				const msg = v.message.trim().replace(",", ";");
				const row = [++problemCount, fileName, v.severity, v.line, v.column, v.ruleName, msg, v.url, v.category, result.engine];
				csvString += '"' + row.join('","') + '"\n';
			}
		}
		return csvString;
	}
}

