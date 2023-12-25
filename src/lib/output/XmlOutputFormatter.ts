import {OutputFormatter, RunResults} from "./Results";
import {FormattedOutput, RuleResult} from "../../types";
import * as path from "path";
import {isPathlessViolation} from "../util/Utils";
import htmlEscaper = require('html-escaper');

export class XmlOutputFormatter implements OutputFormatter {
	public async format(results: RunResults): Promise<FormattedOutput> {
		const ruleResults: RuleResult[] = results.getRuleResults();

		let resultXml = ``;

		const normalizeSeverity: boolean = ruleResults[0]?.violations.length > 0 && !(ruleResults[0]?.violations[0].normalizedSeverity === undefined)

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
		for (const result of ruleResults) {
			const from = process.cwd();
			const fileName = path.relative(from, result.fileName);
			const escapedFileName = safeHtmlEscape(fileName);
			const violations: string[] = [];
			for (const v of result.violations) {
				const escapedMessage = safeHtmlEscape(v.message.trim());
				const escapedCategory = safeHtmlEscape(v.category);
				const escapedRuleName = safeHtmlEscape(v.ruleName);
				const escapedUrl = safeHtmlEscape(v.url);

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
					const escapedSinkFileName = safeHtmlEscape(v.sinkFileName);
					const escapedSourceType = safeHtmlEscape(v.sourceType);
					const escapedSourceMethod = safeHtmlEscape(v.sourceMethodName)
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

		return `<results total="${ruleResults.length}" totalViolations="${problemCount}">
        ${resultXml}
</results>`;
	}
}

/**
 * Html escapes a string if it has a non-zero length
 */
export function safeHtmlEscape(str: string): string {
	if (str && str.length > 0) {
		return htmlEscaper.escape(str);
	} else {
		return str;
	}
}
