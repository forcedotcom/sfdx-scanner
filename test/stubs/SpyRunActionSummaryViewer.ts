import {RunResults} from '@salesforce/code-analyzer-core';
import {RunActionSummaryViewer} from '../../src/lib/viewers/RunActionSummaryViewer'

export class SpyRunActionSummaryViewer implements RunActionSummaryViewer {
	private callHistory: {results: RunResults, logFile: string, outfiles: string[]}[] = [];

	public view(results: RunResults, logFile: string, outfiles: string[]): void {
		this.callHistory.push({results, logFile, outfiles});
	}

	public getCallHistory(): {results: RunResults, logFile: string, outfiles: string[]}[] {
		return this.callHistory;
	}
}
