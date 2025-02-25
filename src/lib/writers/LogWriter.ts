import * as path from 'node:path';
import * as fs from 'node:fs/promises';
import {CodeAnalyzerConfig} from '@salesforce/code-analyzer-core';
import {Clock, RealClock, formatToDateTimeString} from '../utils/DateTimeUtils';

export interface LogWriter {
	writeToLog(message: string): void;
	getLogDestination(): string;
	closeLog(): void;
}

export class LogFileWriter implements LogWriter {
	private readonly writeStream: NodeJS.WritableStream;
	private readonly destination: string;

	private constructor(writeStream: NodeJS.WritableStream, destination: string) {
		this.writeStream = writeStream;
		this.destination = destination;
	}

	public writeToLog(message: string): void {
		this.writeStream.write(message);
	}

	public getLogDestination(): string {
		return this.destination;
	}

	public closeLog(): void {
		this.writeStream.end();
	}

	public static async fromConfig(config: CodeAnalyzerConfig, clock: Clock = new RealClock()): Promise<LogFileWriter> {
		const logFolder = config.getLogFolder();
		// Use the current timestamp to make sure each transaction has a unique logfile. If we want to reuse logfiles,
		// or just have one running logfile, we can change this.
		const logFile = path.join(logFolder, `sfca-${formatToDateTimeString(clock.now())}.log`);
		// 'w' flag causes the file to be created if it doesn't already exist.
		const fh = await fs.open(logFile, 'w');
		return new LogFileWriter(fh.createWriteStream({}), logFile);
	}
}
