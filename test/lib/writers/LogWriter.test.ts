import * as path from 'node:path';
import * as fs from 'node:fs/promises';
import * as tmp from 'tmp';
import {CodeAnalyzerConfig} from '@salesforce/code-analyzer-core';
import {LogFileWriter} from '../../../src/lib/writers/LogWriter';
import {Clock} from '../../../src/lib/utils/DateTimeUtils';

describe('LogWriter implementations', () => {

	describe('LogFileWriter', () => {
		let tmpLogFolder: string;
		beforeEach(async () => {

			tmpLogFolder = await new Promise((res, rej) => {
				tmp.setGracefulCleanup();
				tmp.dir({unsafeCleanup: true}, (err, name) => {
					if (!err) {
						res(name);
					} else {
						rej(err);
					}
				});
			});

			jest.spyOn(CodeAnalyzerConfig.prototype, 'getLogFolder').mockImplementation(() => {
				return tmpLogFolder;
			});
		});

		afterEach(async () => {
			jest.restoreAllMocks();
		})

		it('Writes properly-named file to config-specified folder', async () => {
			// ==== TEST SETUP ====
			const config = CodeAnalyzerConfig.withDefaults();
			const fixedDate: Date = new Date(2025, 1, 20, 14, 30, 18, 14);
			const logWriter = await LogFileWriter.fromConfig(config, new FixedClock(fixedDate));

			// ==== TESTED BEHAVIOR ====
			logWriter.writeToLog('beep');
			logWriter.writeToLog('boop');
			logWriter.writeToLog('bop');

			// ==== ASSERTIONS ====
			const logFolderContents = await fs.readdir(tmpLogFolder);
			expect(logFolderContents).toHaveLength(1);
			const logFilePath = path.join(tmpLogFolder, logFolderContents[0]);
			expect(path.basename(logFilePath)).toEqual('sfca-2025_02_20_14_30_18_014.log');
			const logFileContents = await fs.readFile(logFilePath, {encoding: 'utf-8'});
			expect(logFileContents).toContain('beep');
			expect(logFileContents).toContain('boop');
			expect(logFileContents).toContain('bop');
		});
	});
});

class FixedClock implements Clock {
	private readonly fixedDate: Date;

	public constructor(fixedDate: Date) {
		this.fixedDate = fixedDate;
	}

	public now(): Date {
		return this.fixedDate;
	}
}
