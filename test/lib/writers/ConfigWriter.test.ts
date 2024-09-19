import path from 'node:path';
import fs from 'node:fs';
import {OutputFormat} from '../../../src/lib/models/ConfigModel';
import {ConfigFileWriter} from '../../../src/lib/writers/ConfigWriter';
import {StubConfigModel} from '../../stubs/StubConfigModel';

describe('ConfigWriter implementations', () => {
	describe('ConfigWriterImpl', () => {

		let writeFileSpy: jest.SpyInstance;
		let writeFileInvocations: {file: fs.PathOrFileDescriptor, contents: String|ArrayBufferView}[];
		let copyFileSpy: jest.SpyInstance;
		let copyFileInvocations: {src: fs.PathLike, dest: fs.PathLike}[];
		beforeEach(() => {
			writeFileInvocations = [];
			writeFileSpy = jest.spyOn(fs, 'writeFileSync').mockImplementation((file, contents) => {
				writeFileInvocations.push({file, contents});
			});
			copyFileInvocations = [];
			copyFileSpy = jest.spyOn(fs, 'copyFileSync').mockImplementation((src, dest) => {
				copyFileInvocations.push({src, dest});
			});
		});

		afterEach(() => {
			jest.restoreAllMocks();
		})

		it.each([
			{ext: '.yaml', expectedOutput: `# This is a leading comment\nResults formatted as ${OutputFormat.RAW_YAML}`},
			{ext: '.yml', expectedOutput: `# This is a leading comment\nResults formatted as ${OutputFormat.RAW_YAML}`}
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
			expect(copyFileSpy).not.toHaveBeenCalled();
		});

		it('When told to overwrite an existing file, creates a backup first', () => {
			const existingConfigFile = path.resolve(__dirname, '..', '..', 'fixtures', 'example-workspaces', 'workspace-with-yml-config', 'code-analyzer.yml');

			const configFileWriter = ConfigFileWriter.fromFile(existingConfigFile);

			const stubbedConfigModel = new StubConfigModel();

			configFileWriter.write(stubbedConfigModel);

			expect(copyFileSpy).toHaveBeenCalled();
			expect(copyFileInvocations).toHaveLength(1);
			expect(copyFileInvocations[0].src).toEqual(existingConfigFile);
			expect(copyFileInvocations[0].dest).toMatch(new RegExp(`${existingConfigFile}-\\d+.bak`));
			expect(writeFileSpy).toHaveBeenCalled();
			expect(writeFileInvocations).toEqual([{
				file: existingConfigFile,
				contents: `# This is a leading comment\nResults formatted as ${OutputFormat.RAW_YAML}`
			}]);
		});

		it('Rejects invalid file format: *.txt', () => {
			const invalidFile = 'beep.txt';
			expect(() => ConfigFileWriter.fromFile(invalidFile)).toThrow(invalidFile);
			expect(copyFileSpy).not.toHaveBeenCalled();
			expect(writeFileSpy).not.toHaveBeenCalled();
		})
	});
});
