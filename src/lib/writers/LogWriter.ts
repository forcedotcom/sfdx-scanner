import * as path from 'node:path';
import * as fs from 'node:fs/promises';
import {CodeAnalyzerConfig} from '@salesforce/code-analyzer-core';

export interface LogWriter {
	writeToLog(message: string): void;
	closeLog(): void;
}

export class LogFileWriter implements LogWriter {
	private readonly writeStream: NodeJS.WritableStream;

	private constructor(writeStream: NodeJS.WritableStream) {
		this.writeStream = writeStream;
	}

	public writeToLog(message: string): void {
		this.writeStream.write(message);
	}

	public closeLog(): void {
		this.writeStream.end();
	}

	public static async fromConfig(config: CodeAnalyzerConfig): Promise<LogFileWriter> {
		const logFolder = config.getLogFolder();
		// Use the current timestamp to make sure each transaction has a unique logfile. If we want to reuse logfiles,
		// or just have one running logfile, we can change this.
		const logFile = path.join(logFolder, `sfca-${Date.now()}.log`);
		// 'w' flag causes the file to be created if it doesn't already exist.
		const fh = await fs.open(logFile, 'w');
		return new LogFileWriter(fh.createWriteStream({}));
	}
}
