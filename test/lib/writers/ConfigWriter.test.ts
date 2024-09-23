import fs from 'node:fs';
import path from 'node:path';
import {OutputFormat} from '../../../src/lib/models/ConfigModel';
import {ConfigFileWriter} from '../../../src/lib/writers/ConfigWriter';
import {StubConfigModel} from '../../stubs/StubConfigModel';
import {DisplayEvent, DisplayEventType, SpyDisplay} from '../../stubs/SpyDisplay';

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
			{ext: '.yaml', expectedOutput: `# This is a leading comment\nResults formatted as ${OutputFormat.RAW_YAML}`},
			{ext: '.yml', expectedOutput: `# This is a leading comment\nResults formatted as ${OutputFormat.RAW_YAML}`}
		])('Accepts and outputs valid file format: *$ext', async ({ext, expectedOutput}) => {
			const validFile = `beep${ext}`;
			const spyDisplay: SpyDisplay = new SpyDisplay(true);
			const configFileWriter = ConfigFileWriter.fromFile(validFile, spyDisplay);

			const stubbedConfig = new StubConfigModel();

			await configFileWriter.write(stubbedConfig);

			expect(spyDisplay.getDisplayEvents()).toHaveLength(0);

			expect(writeFileSpy).toHaveBeenCalled();
			expect(writeFileInvocations).toEqual([{
				file: validFile,
				contents: expectedOutput
			}]);
		});

		it.each([
			{case: 'Confirmation granted', confirmation: true, expectedCallCount: 1},
			{case: 'Confirmation denied', confirmation: false, expectedCallCount: 0}
		])('Only overwrites existing file after requesting user confirmation. Case: $case', async ({confirmation, expectedCallCount}) => {
			const validFile = path.resolve(__dirname, '..', '..', 'fixtures', 'example-workspaces', 'workspace-with-yml-config', 'code-analyzer.yml');
			const spyDisplay: SpyDisplay = new SpyDisplay(confirmation);
			const configFileWriter = ConfigFileWriter.fromFile(validFile, spyDisplay);

			const stubbedConfig = new StubConfigModel();

			await configFileWriter.write(stubbedConfig);

			const displayEvents: DisplayEvent[] = spyDisplay.getDisplayEvents();
			expect(displayEvents).toHaveLength(1);
			// The user should be prompted to confirm override.
			expect(displayEvents[0].type).toEqual(DisplayEventType.CONFIRM);
			expect(displayEvents[0].data).toContain('Overwrite');
			expect(writeFileSpy).toHaveBeenCalledTimes(expectedCallCount);
		});

		it('Rejects invalid file format: *.txt', async () => {
			const invalidFile = 'beep.txt';
			const spyDisplay: SpyDisplay = new SpyDisplay(true);
			expect(() => ConfigFileWriter.fromFile(invalidFile, spyDisplay)).toThrow(invalidFile);
		})
	});
});
