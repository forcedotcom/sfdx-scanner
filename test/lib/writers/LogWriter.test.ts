import * as path from 'node:path';
import * as fs from 'node:fs/promises';
import * as tmp from 'tmp';
import {CodeAnalyzerConfig} from '@salesforce/code-analyzer-core';
import {LogFileWriter} from '../../../src/lib/writers/LogWriter';

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

		it('Writes to file specified by config', async () => {
			// ==== TEST SETUP ====
			const config = CodeAnalyzerConfig.withDefaults();
			const logWriter = await LogFileWriter.fromConfig(config);

			// ==== TESTED BEHAVIOR ====
			logWriter.writeToLog('beep');
			logWriter.writeToLog('boop');
			logWriter.writeToLog('bop');

			// ==== ASSERTIONS ====
			const logFolderContents = await fs.readdir(tmpLogFolder);
			expect(logFolderContents).toHaveLength(1);
			const logFilePath = path.join(tmpLogFolder, logFolderContents[0]);
			const logFileContents = await fs.readFile(logFilePath, {encoding: 'utf-8'});
			expect(logFileContents).toContain('beep');
			expect(logFileContents).toContain('boop');
			expect(logFileContents).toContain('bop');
		});
	});
});
