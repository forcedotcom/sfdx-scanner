import fs from 'node:fs';
import {CodeAnalyzerConfig} from '@salesforce/code-analyzer-core';
import {ConfigFileWriter} from '../../../src/lib/writers/ConfigWriter';

describe('ConfigWriter implementations', () => {

	describe('ConfigFileWriter', () => {
		let writeFileSpy: jest.SpyInstance;
		let writeFileInvocations: {file: fs.PathOrFileDescriptor, contents: string | ArrayBufferView }[];

		beforeEach(() => {
			writeFileInvocations = [];
			writeFileSpy = jest.spyOn(fs, 'writeFileSync').mockImplementation((file, contents) => {
				writeFileInvocations.push({file, contents});
			});
		});

		afterEach(() => {
			jest.restoreAllMocks();
		})

		it('When no config file exists, one is created with the provided config', () => {
			// ==== SETUP ====
			jest.spyOn(fs, 'existsSync').mockImplementation(path => false);
			const configFileWriter = new ConfigFileWriter(false);
			const newConfig = CodeAnalyzerConfig.withDefaults();
			newConfig

			// ==== TESTED BEHAVIOR ====


			// ==== ASSERTIONS ====
		});

		it('When a config already exists and force=false, an error is thrown', () => {
			// ==== SETUP ====
			jest.spyOn(fs, 'existsSync').mockImplementation(path => false);

			// ==== TESTED BEHAVIOR ====


			// ==== ASSERTIONS ====
		});

		it('When a config already exists and force=true, the provided config overwrites the existing one', () => {
			// ==== SETUP ====
			jest.spyOn(fs, 'existsSync').mockImplementation(path => false);

			// ==== TESTED BEHAVIOR ====


			// ==== ASSERTIONS ====
		});
	});
});
