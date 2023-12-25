import {OutputFormatter, RunResults} from "./Results";
import {FormattedOutput, RuleResult, RuleViolation} from "../../types";
import {isPathlessViolation} from "../util/Utils";
import {safeHtmlEscape} from "./XmlOutputFormatter";

export class JunitOutputFormatter implements OutputFormatter {
	public async format(results: RunResults): Promise<FormattedOutput> {
		const ruleResults: RuleResult[] = results.getRuleResults();

		// Otherwise, we'll need to start constructing our JUnit XML. To do that, we'll need a map from file names to
		// lists of the <failure> tags generated from violations found in the corresponding file.
		const violationsByFileName = new Map<string, string[]>();

		// Iterate over all of the results, convert them to a <failure> tag, and map that tag by the file name.
		for (const result of ruleResults) {
			const {fileName, violations} = result;
			const mappedViolations: string[] = violationsByFileName.get(fileName) || [];
			for (const violation of violations) {
				mappedViolations.push(violationJsonToJUnitTag(fileName, violation));
			}
			violationsByFileName.set(fileName, mappedViolations);
		}

		// Use each entry in the map to construct a <testsuite> tag.
		const testsuiteTags = [];
		for (const [fileName, failures] of violationsByFileName.entries()) {
			const escapedFileName = safeHtmlEscape(fileName);
			testsuiteTags.push(`<testsuite name="${escapedFileName}" tests="${failures.length}" errors="${failures.length}">\n${failures.join('\n')}\n</testsuite>`);
		}

		return `<testsuites>\n${testsuiteTags.join('\n')}\n</testsuites>`
	}
}


function violationJsonToJUnitTag(fileName: string, violation: RuleViolation): string {
	const {
		message,
		severity,
		category,
		ruleName,
		url
	} = violation;
	const line = isPathlessViolation(violation) ? violation.line : violation.sourceLine;
	const column = isPathlessViolation(violation) ? violation.column : violation.sourceColumn;
	const escapedFileName = safeHtmlEscape(fileName);
	const escapedMessage = safeHtmlEscape(message.trim());
	const escapedCategory = safeHtmlEscape(category);
	const escapedRuleName = safeHtmlEscape(ruleName);

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
		const escapedSourceType = safeHtmlEscape(violation.sourceType);
		const escapedSourceMethod = safeHtmlEscape(violation.sourceMethodName);
		const escapedSinkFileName = safeHtmlEscape(violation.sinkFileName);
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
