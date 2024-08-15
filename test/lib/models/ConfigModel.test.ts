import {DummyConfigModel, DUMMY_CONFIG, OutputFormat} from '../../../src/lib/models/ConfigModel';

describe('ConfigModel implementations', () => {
	describe('DummyConfigModel', () => {
		it('Outputs the Dummy Config', () => {
			expect(new DummyConfigModel().toFormattedOutput(OutputFormat.YAML)).toEqual(DUMMY_CONFIG);
		});
	});
});
