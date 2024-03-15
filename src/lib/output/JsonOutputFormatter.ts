import {OutputFormatter, Results} from "./Results";
import {FormattedOutput, RuleResult} from "../../types";
import {ENGINE} from "../../Constants";

export class JsonOutputFormatter implements OutputFormatter {
	private readonly verboseViolations: boolean;

	public constructor(verboseViolations: boolean) {
		this.verboseViolations = verboseViolations;
	}

	public format(results: Results): Promise<FormattedOutput> {
		const ruleResults: RuleResult[] = results.getRuleResults();
		if (this.verboseViolations) {
			const resultsVerbose = JSON.parse(JSON.stringify(ruleResults)) as RuleResult[];
			for (const result of resultsVerbose) {
				if ((result.engine as ENGINE) === ENGINE.RETIRE_JS) {
					for (const violation of result.violations) {
						// in the json format we need to replace new lines in the message
						// for the first line (ending with a colon) we will replace it with a space
						// for following lines, we will replace it with a semicolon and a space
						violation.message = violation.message.replace(/:\n/g, ': ').replace(/\n/g, '; ');
					}
				}
			}
			return Promise.resolve(JSON.stringify(resultsVerbose.filter(r => r.violations.length > 0)));
		}
		return Promise.resolve(JSON.stringify(ruleResults.filter(r => r.violations.length > 0)));
	}
}
