import {RunResults} from '@salesforce/code-analyzer-core';
import {RunSummaryViewer} from '../../src/lib/viewers/RunSummaryViewer'

export class SpyRunSummaryViewer implements RunSummaryViewer {
	private callHistory: {results: RunResults, logFile: string, outfiles: string[]}[] = [];

	public view(results: RunResults, logFile: string, outfiles: string[]): void {
		this.callHistory.push({results, logFile, outfiles});
	}

	public getCallHistory(): {results: RunResults, logFile: string, outfiles: string[]}[] {
		return this.callHistory;
	}
}
