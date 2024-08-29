import fs from 'node:fs';
import {OutputFormat} from '../../../src/lib/models/ConfigModel';
import {ConfigFileWriter} from '../../../src/lib/writers/ConfigWriter';
import {StubConfigModel} from '../../stubs/StubConfigModel';

describe('ConfigWriter implementations', () => {
	describe('ConfigWriterImpl', () => {

		let writeFileSpy: jest.SpyInstance;
		let writeFileInvocations: {file: fs.PathOrFileDescriptor, contents: String|ArrayBufferView}[];
		beforeEach(() => {
			writeFileInvocations = [];
			writeFileSpy = jest.spyOn(fs, 'writeFileSync').mockImplementation((file, contents) => {
				writeFileInvocations.push({file, contents});
			});
		});

		afterEach(() => {
			jest.restoreAllMocks();
		})

		it.each([
			{ext: '.yaml', expectedOutput: `# This is a leading comment\nResults formatted as ${OutputFormat.YAML}`},
			{ext: '.yml', expectedOutput: `# This is a leading comment\nResults formatted as ${OutputFormat.YAML}`}
		])('Accepts and outputs valid file format: *$ext', ({ext, expectedOutput}) => {
			const validFile = `beep${ext}`;
			const configFileWriter = ConfigFileWriter.fromFile(validFile);

			const stubbedConfig = new StubConfigModel();

			configFileWriter.write(stubbedConfig);

			expect(writeFileSpy).toHaveBeenCalled();
			expect(writeFileInvocations).toEqual([{
				file: validFile,
				contents: expectedOutput
			}]);
		});

		it('Rejects invalid file format: *.txt', () => {
			const invalidFile = 'beep.txt';
			expect(() => ConfigFileWriter.fromFile(invalidFile)).toThrow(invalidFile);
		})
	});
});
