import {SfdxError} from '@salesforce/core';
import * as path from 'path';
import {EngineExecutionSummary, RecombinedData, RecombinedRuleResults, RuleResult, RuleViolation} from '../../types';
import {DfaEngineFilters, ENGINE} from '../../Constants';
import {OUTPUT_FORMAT} from '../RuleManager';
import * as wrap from 'word-wrap';
import {FileHandler} from '../util/FileHandler';
import * as Mustache from 'mustache';
import htmlEscaper = require('html-escaper');
import { stringify } from 'csv-stringify';
import { constructSarif } from './SarifFormatter';
import {isPathlessViolation} from '../util/Utils';

type BaseTableRow = {
	Rule: string;
	Description: string;
	URL: string;
	Category: string;
	Severity: number;
	Engine: string;
};

type PathlessTableRow = BaseTableRow & {
	Location: string;
	Line: number;
	Column: number;
};

type DfaTableRow = BaseTableRow & {
	"Source Location": string;
	"Source Line": number;
	"Source Column": number;
	"Sink Location": string|null;
	"Sink Line": number|null;
	"Sink Column": number|null;
};

export class RuleResultRecombinator {

	public static async recombineAndReformatResults(results: RuleResult[], format: OUTPUT_FORMAT, executedEngines: Set<string>, verboseViolations = false): Promise<RecombinedRuleResults> {
		// We need to change the results we were given into the desired final format.
		let formattedResults: string | {columns; rows} = null;
		switch (format) {
			case OUTPUT_FORMAT.CSV:
				formattedResults = await this.constructCsv(results, executedEngines);
				break;
			case OUTPUT_FORMAT.HTML:
				formattedResults = await this.constructHtml(results, executedEngines, verboseViolations);
				break;
			case OUTPUT_FORMAT.JSON:
				formattedResults = this.constructJson(results);
				break;
			case OUTPUT_FORMAT.JUNIT:
				formattedResults = this.constructJunit(results);
				break;
			case OUTPUT_FORMAT.SARIF:
				formattedResults = await constructSarif(results, executedEngines);
				break;
			case OUTPUT_FORMAT.TABLE:
				formattedResults = this.constructTable(results, executedEngines);
				break;
			case OUTPUT_FORMAT.XML:
				formattedResults = this.constructXml(results);
				break;
			default:
				throw new SfdxError('Unrecognized output format.');
		}
		return {minSev: this.findMinSev(results), results: formattedResults, summaryMap: this.generateSummaryMap(results, executedEngines)};
	}

	private static findMinSev(results: RuleResult[]): number {
		// If there are no results, then there are no errors.
		if (!results || results.length === 0) {
			return 0;
		}
		let minSev: number = null;

		// if -n or -s flag used, minSev is calculated with normal value
		for (const res of results) {
			for (const violation of res.violations) {
				const severity = (violation.normalizedSeverity === undefined)? violation.severity : violation.normalizedSeverity;
				if (!minSev || severity < minSev) {
					minSev = severity;
				}
			}
		}

		// After iterating through all of the results, return the minimum severity we found (or 0 if we still have a null value).
		return minSev || 0;
	}

	private static generateSummaryMap(results: RuleResult[], executedEngines: Set<string>): Map<string, EngineExecutionSummary> {
		const summaryMap: Map<string, EngineExecutionSummary> = new Map();
		for (const e of executedEngines.values()) {
			summaryMap.set(e, {
				fileCount: 0,
				violationCount: 0
			});
		}
		if (results && results.length > 0) {
			results.forEach(res => {
				const ees: EngineExecutionSummary = summaryMap.get(res.engine);
				ees.fileCount += 1;
				ees.violationCount += res.violations.length;
			});
		}
		return summaryMap;
	}

	private static constructXml(results: RuleResult[]): string {
		let resultXml = ``;

		// If the results were just an empty string, we can return it.
		if (results.length === 0) {
			return resultXml;
		}

		const normalizeSeverity: boolean = results[0].violations.length > 0 && !(results[0].violations[0].normalizedSeverity === undefined)

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
			const escapedFileName = this.safeHtmlEscape(fileName);
			const violations: string[] = [];
			for (const v of result.violations) {
				const escapedMessage = this.safeHtmlEscape(v.message.trim());
				const escapedCategory = this.safeHtmlEscape(v.category);
				const escapedRuleName = this.safeHtmlEscape(v.ruleName);
				const escapedUrl = this.safeHtmlEscape(v.url);

				problemCount++;
				let tagArray: string[] = [];
				tagArray.push(`severity="${v.severity}"`);
				if (normalizeSeverity) {
					tagArray.push(`normalizedSeverity="${v.normalizedSeverity}"`);
				}
				if (isPathlessViolation(v)) {
					tagArray = [...tagArray,
						`line="${v.line}"`,
						`column="${v.column}"`,
						`endLine="${v.endLine}"`,
						`endColumn="${v.endColumn}"`,
						`rule="${escapedRuleName}"`,
						`category="${escapedCategory}"`,
						`url="${escapedUrl}"`
					];
				} else  {
					const escapedSinkFileName = this.safeHtmlEscape(v.sinkFileName);
					const escapedSourceType = this.safeHtmlEscape(v.sourceType);
					const escapedSourceMethod = this.safeHtmlEscape(v.sourceMethodName)
					tagArray = [...tagArray,
						`sourceLine="${v.sourceLine}"`,
						`sourceColumn="${v.sourceColumn}"`,
						`sourceType="${escapedSourceType}"`,
						`sourceMethod="${escapedSourceMethod}"`,
						`sinkLine="${v.sinkLine}"`,
						`sinkColumn="${v.sinkColumn}"`,
						`sinkFileName="${escapedSinkFileName}"`,
						`rule="${escapedRuleName}"`,
						`category="${escapedCategory}"`,
						`url="${escapedUrl}"`
					];
				}
				violations.push(`<violation ${tagArray.join(' ')}>${escapedMessage}</violation>`);
			}
			resultXml += `
      <result file="${escapedFileName}" engine="${result.engine}">
          ${violations.join('')}
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
			severity,
			category,
			ruleName,
			url
		} = violation;
		const line = isPathlessViolation(violation) ? violation.line : violation.sourceLine;
		const column = isPathlessViolation(violation) ? violation.column : violation.sourceColumn;
		const escapedFileName = this.safeHtmlEscape(fileName);
		const escapedMessage = this.safeHtmlEscape(message.trim());
		const escapedCategory = this.safeHtmlEscape(category);
		const escapedRuleName = this.safeHtmlEscape(ruleName);

		// Any violation will have the same header information.
		const header = `<testcase name="${escapedFileName}">
<failure message="${escapedFileName}: ${line} ${escapedMessage}" type="${severity}">
${severity}: ${escapedMessage}
Category: ${escapedCategory} - ${escapedRuleName}`;
		// Pathless and DFA violations have different bodies.
		let body: string;
		if (isPathlessViolation(violation)) {
			body = `File: ${escapedFileName}
Line: ${line}
Column: ${column}
URL: ${url}`;
		} else {
			const escapedSourceType = this.safeHtmlEscape(violation.sourceType);
			const escapedSourceMethod = this.safeHtmlEscape(violation.sourceMethodName);
			const escapedSinkFileName = this.safeHtmlEscape(violation.sinkFileName);
			const {sinkLine, sinkColumn} = violation;
			body = `Source File: ${escapedFileName}
	Type: ${escapedSourceType}
	Method: ${escapedSourceMethod}
	Line: ${line}
	Column: ${column}
Sink File: ${escapedSinkFileName}
	Line: ${sinkLine}
	Column: ${sinkColumn}
URL: ${url}`;
		}

		// Any violation will have the same footer.
		const footer = `</failure>
</testcase>`;
		// Put it all together to get the tag.
		return `${header}\n${body}\n${footer}`;
	}

	private static constructTable(results: RuleResult[], executedEngines: Set<string>): RecombinedData {
		// If the results were just an empty string, we can return it.
		if (results.length === 0) {
			return '';
		}

		// If any of the engines are DFA engines, we should use the DFA columns. Otherwise, we should use the static columns.
		// NOTE: This code is predicated on the assumption that DFA and Pathless engines will not be run concurrently.
		// If that assumption is ever invalidated, then this code has to change.
		const columns = DfaEngineFilters.some(e => executedEngines.has(e))
			? ['Source Location', 'Sink Location', 'Description', 'Category', 'URL']
			: ['Location', 'Description', 'Category', 'URL'];

		// Build the rows.
		const rows: (PathlessTableRow|DfaTableRow)[] = [];
		for (const result of results) {
			const fileName = result.fileName;
			for (const violation of result.violations) {
				const message = violation.message.trim();
				const relativeFile = path.relative(process.cwd(), fileName);
				// Instantiate our Row object.
				const baseRow: BaseTableRow = {
					Rule: violation.ruleName,
					Description: wrap(message),
					URL: violation.url,
					Category: violation.category,
					Severity: violation.severity,
					Engine: result.engine
				};
				// A pathless violation can be trivially converted into a row.
				if (isPathlessViolation(violation)) {
					rows.push({
						...baseRow,
						Location: `${relativeFile}:${violation.line}`,
						Line: violation.line,
						Column: violation.column
					});
				} else if (!(violation.sinkFileName)) {
					// If the violation is path-based but has no sink file, then the violation indicates an error of some
					// kind. So use the source information we were given, but use null for everything else.
					rows.push({
						...baseRow,
						"Source Location": `${relativeFile}:${violation.sourceLine}`,
						"Source Line": violation.sourceLine,
						"Source Column": violation.sourceColumn,
						"Sink Location": "",
						"Sink Line": null,
						"Sink Column": null
					});
				} else {
					const relativeSinkFile = path.relative(process.cwd(), violation.sinkFileName);
					rows.push({
						...baseRow,
						"Source Location": `${relativeFile}:${violation.sourceLine}`,
						"Source Line": violation.sourceLine,
						"Source Column": violation.sourceColumn,
						"Sink Location": `${relativeSinkFile}:${violation.sinkLine}`,
						"Sink Line": violation.sinkLine,
						"Sink Column": violation.sinkColumn
					});
				}
			}
		}
		return {columns, rows};
	}

	private static constructJson(results: RuleResult[]): string {
		if (results.length === 0) {
			return '';
		}
		return JSON.stringify(results.filter(r => r.violations.length > 0));
	}

	private static async constructHtml(results: RuleResult[], executedEngines: Set<string>, verboseViolations = false): Promise<string> {
		// If the results were just an empty string, we can return it.
		if (results.length === 0) {
			return '';
		}

		const normalizeSeverity: boolean = results[0].violations.length > 0 && !(results[0].violations[0].normalizedSeverity === undefined);
		const isDfa = DfaEngineFilters.some(e => executedEngines.has(e));


		const violations = [];
		for (const result of results) {
			for (const v of result.violations) {
				let htmlFormattedViolation;
				if (isPathlessViolation(v)) {
					htmlFormattedViolation = {
						engine: result.engine,
						fileName: result.fileName,
						severity: (normalizeSeverity ? v.normalizedSeverity : v.severity),
						ruleName: v.ruleName,
						category: v.category,
						url: v.url,
						message: verboseViolations && result.engine === ENGINE.RETIRE_JS ? v.message.replace(/\n/g, '<br>') : v.message, // <br> used for line breaks in html
						line: v.line,
						column: v.column,
						endLine: v.endLine || null,
						endColumn: v.endColumn || null
					}
				} else {
					htmlFormattedViolation = {
						engine: result.engine,
						fileName: result.fileName,
						severity: (normalizeSeverity ? v.normalizedSeverity : v.severity),
						ruleName: v.ruleName,
						category: v.category,
						url: v.url,
						message: v.message,
						line: v.sourceLine,
						column: v.sourceColumn,
						sinkFileName: v.sinkFileName,
						sinkLine: v.sinkLine,
						sinkColumn: v.sinkColumn
					};
				}
				violations.push(htmlFormattedViolation);
			}
		}

		// Populate the template with a JSON payload of the violations
		const fileHandler = new FileHandler();
		const templateName = isDfa ? 'dfa-simple.mustache' : 'simple.mustache';
		const template = await fileHandler.readFile(path.resolve(__dirname, '..', '..', '..', 'html-templates', templateName));
		const args = ['sfdx', process.argv[2]];
		for (const arg of process.argv.slice(3)) {
			if (arg.startsWith('-')) {
				// Pass flags as-is
				args.push(arg);
			} else {
				// Wrag flag parameters in quotes
				args.push(`"${arg}"`);
			}
		}
		const templateData = {
			violations: JSON.stringify(violations),
			workingDirectory: process.cwd(),
			commandLine: args.join(' ')
		};

		return Mustache.render(template, templateData);
	}

	private static async constructCsv(results: RuleResult[], executedEngines: Set<string>): Promise<string> {
		// If the results were just an empty string, we can return it.
		if (results.length === 0) {
			return '';
		}
		const isDfa = DfaEngineFilters.some(e => executedEngines.has(e));
		const normalizeSeverity: boolean = results[0].violations.length > 0 && !(results[0].violations[0].normalizedSeverity === undefined)

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
		for (const result of results) {
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
