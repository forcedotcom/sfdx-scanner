import {ConfigRawYamlViewer} from '../../../src/lib/viewers/ConfigViewer';
import {DisplayEventType, SpyDisplay} from '../../stubs/SpyDisplay';
import {StubConfigModel} from '../../stubs/StubConfigModel';

describe('ConfigViewer implementations', () => {
	let spyDisplay: SpyDisplay;

	beforeEach(() => {
		spyDisplay = new SpyDisplay();
	})

	describe('ConfigRawYamlViewer', () => {
		let viewer: ConfigRawYamlViewer;

		beforeEach(() => {
			viewer = new ConfigRawYamlViewer(spyDisplay);
		})

		it('When given a config, outputs it as raw YAML', () => {
			// ==== TEST SETUP ====
			// Instantiate the config model.
			const configModel = new StubConfigModel();

			// ==== TESTED METHOD ====
			// Display the config
			viewer.view(configModel);

			// ==== ASSERTIONS ====
			const displayEvents = spyDisplay.getDisplayEvents();
			expect(displayEvents).toHaveLength(1);
			expect(displayEvents).toEqual([{
				type: DisplayEventType.LOG,
				data: `Results formatted as YAML`
			}]);
		});
	});
});
