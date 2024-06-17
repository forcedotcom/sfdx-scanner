import {RunResults} from '@salesforce/code-analyzer-core';
import {OutputFileWriter} from '../../src/lib/writers/OutputFileWriter';

export class SpyOutputFileWriter implements OutputFileWriter {
	private callHistory: RunResults[] = [];

	public writeToFiles(results: RunResults): void {
		this.callHistory.push(results);
	}

	public getCallHistory(): RunResults[] {
		return this.callHistory;
	}
}
