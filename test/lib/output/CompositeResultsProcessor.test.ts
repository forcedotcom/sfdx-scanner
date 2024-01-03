import {CompositeResultsProcessor, ResultsProcessor} from "../../../src/lib/output/ResultsProcessor";
import {FakeResults} from "./FakeResults";
import {Results} from "../../../lib/lib/output/Results";
import {expect} from "chai";

describe('CompositeResultsProcessor Tests', () => {
	it('Empty composite should not blow up', async () => {
		const compositeResultsProcessor: CompositeResultsProcessor = new CompositeResultsProcessor([]);
		await compositeResultsProcessor.processResults(new FakeResults());
	});

	it('Each delegate is called correctly', async () => {
		const results: Results = new FakeResults();
		const resultsProcessors: FakeResultsProcessor[] = [
			new FakeResultsProcessor(), new FakeResultsProcessor(), new FakeResultsProcessor()];
		const compositeResultsProcessor: CompositeResultsProcessor = new CompositeResultsProcessor(resultsProcessors);
		await compositeResultsProcessor.processResults(results);
		for (let i = 0; i < resultsProcessors.length; i++) {
			expect(resultsProcessors[i].lastResultsProcessed).to.equal(results);
		}
	});
});

class FakeResultsProcessor implements ResultsProcessor {
	lastResultsProcessed: Results = null;

	processResults(results: Results): Promise<void> {
		this.lastResultsProcessed = results;
		return Promise.resolve();
	}
}
