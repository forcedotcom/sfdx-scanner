import {CodeAnalyzerConfig, RunResults} from '@salesforce/code-analyzer-core';
import {RunSummaryViewer} from '../../src/lib/viewers/RunSummaryViewer'

export class SpyRunSummaryViewer implements RunSummaryViewer {
	private callHistory: {results: RunResults, config: CodeAnalyzerConfig, outfiles: string[]}[] = [];

	public view(results: RunResults, config: CodeAnalyzerConfig, outfiles: string[]): void {
		this.callHistory.push({results, config, outfiles});
	}

	public getCallHistory(): {results: RunResults, config: CodeAnalyzerConfig, outfiles: string[]}[] {
		return this.callHistory;
	}
}
