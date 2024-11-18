import fs from 'node:fs';
import {OutputFormat} from '@salesforce/code-analyzer-core';
import {ResultsFileWriter, CompositeResultsWriter} from '../../../src/lib/writers/ResultsWriter';
import * as StubRunResults from '../../stubs/StubRunResults';

describe('ResultsWriter implementations', () => {
	let writeFileSpy: jest.SpyInstance;
	let writeFileInvocations: { file: fs.PathOrFileDescriptor, contents: string | ArrayBufferView }[];
	beforeEach(() => {
		writeFileInvocations = [];
		writeFileSpy = jest.spyOn(fs, 'writeFileSync').mockImplementation(async (file, contents) => {
			writeFileInvocations.push({file, contents});
		});
	});

	afterEach(() => {
		jest.restoreAllMocks();
	});

	describe('ResultsFileWriter', () => {
		it.each([
			{ext: '.csv', expectedOutput: `Results formatted as ${OutputFormat.CSV}`},
			{ext: '.html', expectedOutput: `Results formatted as ${OutputFormat.HTML}`},
			{ext: '.htm', expectedOutput: `Results formatted as ${OutputFormat.HTML}`},
			{ext: '.json', expectedOutput: `Results formatted as ${OutputFormat.JSON}`},
			//{ext: '.sarif', expectedOutput: `Results formatted as ${OutputFormat.SARIF}`},
			//{ext: '.sarif.json', expectedOutput: `Results formatted as ${OutputFormat.SARIF}`},
			{ext: '.xml', expectedOutput: `Results formatted as ${OutputFormat.XML}`}
		])('Accepts and outputs valid file format: *$ext', ({ext, expectedOutput}) => {
			const validFile = `beep${ext}`;
			const outputFileWriter = new ResultsFileWriter(validFile);
			const stubbedResults = new StubRunResults.StubNonEmptyResults();

			outputFileWriter.write(stubbedResults);

			expect(writeFileSpy).toHaveBeenCalled();
			expect(writeFileInvocations).toEqual([{
				file: validFile,
				contents: expectedOutput
			}]);
		});

		/**
		 * All of these extensions are ones we intend to support long-term, but don't yet. When we add support for one of
		 * these extensions, we should remove it from the cases array here and uncomment the corresponding line
		 * in the case array for the valid format tests.
		 */
		it.each([
			{ext: '.sarif'},
			{ext: '.sarif.json'}
		])('Throws TODO error for not-yet-supported format: *$ext', ({ext}) => {
			const notYetSupportedFile = `beep${ext}`;
			// Expect the error message to include an indication that the functionality will be implemented eventually.
			expect(() => new ResultsFileWriter(notYetSupportedFile)).toThrow('TODO');
		});

		it('Writes file even when results are empty', () => {
			const expectations = {
				file: 'beep.csv',
				contents: `Results formatted as ${OutputFormat.CSV}`
			};
			const outputFileWriter = new ResultsFileWriter(expectations.file);
			const stubbedResults = new StubRunResults.StubEmptyResults();

			outputFileWriter.write(stubbedResults);

			expect(writeFileSpy).toHaveBeenCalled();
			expect(writeFileInvocations).toEqual([expectations]);
		});

		it('Rejects invalid file format: *.txt', () => {
			const invalidFile = 'beep.txt';
			expect(() => new ResultsFileWriter(invalidFile)).toThrow(invalidFile);
		});
	});

	describe('CompositeResultsWriter', () => {
		it('Does a no-op when there are no files to write to', () => {
			const outputFileWriter = CompositeResultsWriter.fromFiles([]);
			const stubbedResults = new StubRunResults.StubNonEmptyResults();

			outputFileWriter.write(stubbedResults);

			expect(writeFileSpy).not.toHaveBeenCalled();
		});

		it('When given multiple files, outputs to all of them', () => {
			const expectations = [{
				file: 'beep.csv',
				contents: `Results formatted as ${OutputFormat.CSV}`
			}, {
				file: 'beep.xml',
				contents: `Results formatted as ${OutputFormat.XML}`
			}, {
				file: 'beep.json',
				contents: `Results formatted as ${OutputFormat.JSON}`
			}];
			const outputFileWriter = CompositeResultsWriter.fromFiles(expectations.map(i => i.file));
			const stubbedResults = new StubRunResults.StubNonEmptyResults();

			outputFileWriter.write(stubbedResults);

			expect(writeFileSpy).toHaveBeenCalledTimes(3);
			expect(writeFileInvocations).toEqual(expectations);
		});
	})
});
