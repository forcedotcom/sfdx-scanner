import {Results} from "../../../src/lib/output/Results";
import {EngineExecutionSummary, FormattedOutput, RuleResult} from "../../../src/types";
import {OutputFormat} from "../../../src/lib/output/OutputFormat";

export class FakeResults implements Results {
	private minSev: number = 0;
	private summaryMap: Map<string, EngineExecutionSummary>;
	private formattedOutput: FormattedOutput;

	withMinSev(minSev: number): FakeResults {
		this.minSev = minSev;
		return this;
	}

	withSummaryMap(summaryMap: Map<string, EngineExecutionSummary>): FakeResults {
		this.summaryMap = summaryMap;
		return this;
	}

	withFormattedOutput(formattedOutput: FormattedOutput): FakeResults {
		this.formattedOutput = formattedOutput;
		return this;
	}

	getExecutedEngines(): Set<string> {
		throw new Error("Not implemented");
	}

	getMinSev(): number {
		return this.minSev;
	}

	getRuleResults(): RuleResult[] {
		throw new Error("Not implemented");
	}

	getSummaryMap(): Map<string, EngineExecutionSummary> {
		return this.summaryMap;
	}

	isEmpty(): boolean {
		throw new Error("Not implemented");
	}

	violationsAreDfa(): boolean {
		throw new Error("Not implemented");
	}

	toFormattedOutput(_format: OutputFormat, _verboseViolations: boolean): Promise<FormattedOutput> {
		return Promise.resolve(this.formattedOutput);
	}
}
