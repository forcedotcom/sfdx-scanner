import {LogWriter} from '../../src/lib/writers/LogWriter';

export class SpyLogWriter implements LogWriter {
	private log: string;

	public constructor() {
		this.log = '';
	}

	public writeToLog(message: string): void {
		this.log += message;
	}

	public getWrittenLog(): string {
		return this.log;
	}

}
