import {SfdxError} from '@salesforce/core';
import * as path from 'path';
import {RecombinedRuleResults, RuleResult, RuleViolation} from '../types';
import {OUTPUT_FORMAT} from './RuleManager';
import * as wrap from 'word-wrap';
import htmlEscaper = require('html-escaper');
import {FileHandler} from './util/FileHandler';
import * as Mustache from 'mustache';

export class RuleResultRecombinator {

	public static async recombineAndReformatResults(results: RuleResult[], format: OUTPUT_FORMAT): Promise<RecombinedRuleResults> {
		// We need to change the results we were given into the desired final format.
		let formattedResults: string | {columns; rows} = null;
		switch (format) {
			case OUTPUT_FORMAT.JSON:
				formattedResults = this.constructJson(results);
				break;
			case OUTPUT_FORMAT.CSV:
				formattedResults = this.constructCsv(results);
				break;
			case OUTPUT_FORMAT.XML:
				formattedResults = this.constructXml(results);
				break;
			case OUTPUT_FORMAT.JUNIT:
				formattedResults = this.constructJunit(results);
				break;
			case OUTPUT_FORMAT.TABLE:
				formattedResults = this.constructTable(results);
				break;
			case OUTPUT_FORMAT.HTML:
				formattedResults = await this.constructHtml(results);
				break;
			default:
				throw new SfdxError('Unrecognized output format.');
		}
		return {minSev: this.findMinSev(results), results: formattedResults};
	}

	private static findMinSev(results: RuleResult[]): number {
		// If there are no results, then there are no errors.
		if (!results || results.length === 0) {
			return 0;
		}
		let minSev = null;
		for (const res of results) {
			for (const violation of res.violations) {
				if (!minSev || violation.severity < minSev) {
					minSev = violation.severity;
				}
			}
		}
		// After iterating through all of the results, return the minimum severity we found (or 0 if we still have a null value).
		return minSev || 0;
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
			const escapedFileName = this.safeHtmlEscape(fileName);
			testsuiteTags.push(`<testsuite name="${escapedFileName}" tests="${failures.length}" errors="${failures.length}">\n${failures.join('\n')}\n</testsuite>`);
		}

		return `<testsuites>\n${testsuiteTags.join('\n')}\n</testsuites>`
	}

	/**
	 * Html escapes a string if it has a non-zero length
	 */
	private static safeHtmlEscape(str: string): string {
		if (str && str.length > 0) {
			return htmlEscaper.escape(str);
		} else {
			return str;
		}
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
		const escapedFileName = this.safeHtmlEscape(fileName);
		const escapedMessage = this.safeHtmlEscape(message.trim());
		const escapedCategory = this.safeHtmlEscape(category);
		const escapedRuleName = this.safeHtmlEscape(ruleName);
		return `<testcase name="${escapedFileName}">
<failure message="${escapedFileName}: ${line} ${escapedMessage}" type="${severity}">
${severity}: ${escapedMessage}
Category: ${escapedCategory} - ${escapedRuleName}}
File: ${escapedFileName}
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

	private static async constructHtml(results: RuleResult[]): Promise<string> {
		// If the results were just an empty string, we can return it.
		if (results.length === 0) {
			return '';
		}

		const violations = [];
		for (const result of results) {
			for (const v of result.violations) {
				violations.push({
					engine: result.engine,
					fileName: result.fileName,
					line: v.line,
					column: v.column,
					endLine: v.endLine || null,
					endColumn: v.endColumn || null,
					severity: v.severity,
					ruleName: v.ruleName,
					category: v.category,
					url: v.url,
					message: v.message
				});
			}
		}

		// Populate the template with a JSON payload
		const fileHandler = new FileHandler();
		const template = await fileHandler.readFile(path.resolve(__dirname, '..', '..', 'html-templates', 'simple.mustache'));
		return Mustache.render(template, {violations: JSON.stringify(violations)});
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

