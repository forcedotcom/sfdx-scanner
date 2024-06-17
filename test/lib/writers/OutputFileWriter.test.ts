import fs from 'node:fs';
import {OutputFormat} from '@salesforce/code-analyzer-core';
import {OutputFileWriterImpl} from '../../../src/lib/writers/OutputFileWriter';
import * as StubRunResults from '../../stubs/StubRunResults';

describe('OutputFileWriterImpl', () => {
	let writeFileSpy: jest.SpyInstance;
	let writeFileInvocations: {file: fs.PathOrFileDescriptor, contents: string|ArrayBufferView}[];
	beforeEach(() => {
		writeFileInvocations = [];
		writeFileSpy = jest.spyOn(fs, 'writeFileSync').mockImplementation(async (file, contents) => {
			writeFileInvocations.push({file, contents});
		});
	});

	afterEach(() => {
		jest.restoreAllMocks();
	});

	it('Does a no-op when there are no files to write to', () => {
		const outputFileWriter = new OutputFileWriterImpl([]);
		const stubbedResults = new StubRunResults.StubNonEmptyResults();

		outputFileWriter.writeToFiles(stubbedResults);

		expect(writeFileSpy).not.toHaveBeenCalled();
	});

	it.each([
		{ext: '.csv', expectedOutput: `Results formatted as ${OutputFormat.CSV}`},
		//{ext: '.html', expectedOutput: `Results formatted as ${OutputFormat.HTML}`},
		//{ext: '.htm', expectedOutput: `Results formatted as ${OutputFormat.HTML}`},
		{ext: '.json', expectedOutput: `Results formatted as ${OutputFormat.JSON}`},
		//{ext: '.junit', expectedOutput: `Results formatted as ${OutputFormat.JUNIT}`},
		//{ext: '.junit.xml', expectedOutput: `Results formatted as ${OutputFormat.JUNIT}`},
		//{ext: '.sarif', expectedOutput: `Results formatted as ${OutputFormat.SARIF}`},
		//{ext: '.sarif.json', expectedOutput: `Results formatted as ${OutputFormat.SARIF}`},
		{ext: '.xml', expectedOutput: `Results formatted as ${OutputFormat.XML}`}
	])('Accepts and outputs valid file format: *$ext', ({ext, expectedOutput}) => {
		const validFile = `beep${ext}`;
		const outputFileWriter = new OutputFileWriterImpl([validFile]);
		const stubbedResults = new StubRunResults.StubNonEmptyResults();

		outputFileWriter.writeToFiles(stubbedResults);

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
		{ext: '.html'},
		{ext: '.htm'},
		{ext: '.junit'},
		{ext: '.junit.xml'},
		{ext: '.sarif'},
		{ext: '.sarif.json'}
	])('Throws TODO error for not-yet-supported format: *$ext', ({ext}) => {
		const notYetSupportedFile = `beep${ext}`;
		// Expect the error message to include an indication that the functionality will be implemented eventually.
		expect(() => new OutputFileWriterImpl([notYetSupportedFile])).toThrow('TODO');
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
		const outputFileWriter = new OutputFileWriterImpl(expectations.map(i => i.file));
		const stubbedResults = new StubRunResults.StubNonEmptyResults();

		outputFileWriter.writeToFiles(stubbedResults);

		expect(writeFileSpy).toHaveBeenCalledTimes(3);
		expect(writeFileInvocations).toEqual(expectations);
	});

	it('Writes file even when results are empty', () => {
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
		const outputFileWriter = new OutputFileWriterImpl(expectations.map(i => i.file));
		const stubbedResults = new StubRunResults.StubEmptyResults();

		outputFileWriter.writeToFiles(stubbedResults);

		expect(writeFileSpy).toHaveBeenCalledTimes(3);
		expect(writeFileInvocations).toEqual(expectations);
	});

	it('Rejects invalid file format: *.txt', () => {
		const invalidFile = 'beep.txt';
		expect(() => new OutputFileWriterImpl([invalidFile])).toThrow(invalidFile);
	});
});
