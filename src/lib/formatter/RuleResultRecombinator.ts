import {FormattedOutput, RecombinedRuleResults, RuleResult} from '../../types';
import {OUTPUT_FORMAT} from '../RuleManager';
import {Results} from "../output/Results";
export class RuleResultRecombinator {

	public static async recombineAndReformatResults(ruleResults: RuleResult[], format: OUTPUT_FORMAT, executedEngines: Set<string>, verboseViolations = false): Promise<RecombinedRuleResults> {
		const results: Results = new Results(ruleResults, executedEngines);
		const formattedOutput: FormattedOutput = await results.toFormattedOutput(format, verboseViolations);
		return {minSev: results.getMinSev(), results: formattedOutput, summaryMap: results.getSummaryMap()};
	}
}
