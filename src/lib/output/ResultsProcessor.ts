import {Results} from "./Results";

/**
 * Interface to process run results
 */
export interface ResultsProcessor {
	processResults(results: Results): Promise<void>;
}

/**
 * A composite results processor
 */
export class CompositeResultsProcessor implements ResultsProcessor {
	private readonly delegates: ResultsProcessor[];

	public constructor(delegateResultsProcessors: ResultsProcessor[]) {
		this.delegates = delegateResultsProcessors;
	}

	async processResults(results: Results): Promise<void> {
		for (const delegate of this.delegates) {
			await delegate.processResults(results);
		}
	}
}
