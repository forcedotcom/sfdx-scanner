import {ResultsProcessorFactory} from "../../../src/lib/output/ResultsProcessorFactory";
import {ResultsProcessor} from "../../../src/lib/output/ResultsProcessor";
import {Display} from "../../../src/lib/Display";
import {RunOutputOptions} from "../../../src/lib/output/RunResultsProcessor";
import {JsonReturnValueHolder} from "../../../src/lib/output/JsonReturnValueHolder";
import {Results} from "../../../src/lib/output/Results";



/**
 * This fake does zero processing, but instead gives you the ability to access the raw results that were to be processed
 */
export class RawResultsProcessor implements ResultsProcessor {
	private results: Results;

	processResults(results: Results): Promise<void> {
		this.results = results;
		return Promise.resolve();
	}

	getResults(): Results {
		return this.results;
	}
}



/**
 * This fake just passes back whatever results processor you pass in.
 */
export class FakeResultsProcessorFactory implements ResultsProcessorFactory {
	private readonly resultsProcessor: ResultsProcessor;

	constructor(resultsProcessor: ResultsProcessor) {
		this.resultsProcessor = resultsProcessor
	}

	createResultsProcessor(_d: Display, _r: RunOutputOptions, _j: JsonReturnValueHolder): ResultsProcessor {
		return this.resultsProcessor
	}
}
