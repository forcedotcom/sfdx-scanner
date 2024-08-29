import ansis from 'ansis';
import {ConfigStyledYamlViewer} from '../../../src/lib/viewers/ConfigViewer';
import {DisplayEventType, SpyDisplay} from '../../stubs/SpyDisplay';
import {StubConfigModel} from '../../stubs/StubConfigModel';

describe('ConfigViewer implementations', () => {
	let spyDisplay: SpyDisplay;

	beforeEach(() => {
		spyDisplay = new SpyDisplay();
	})

	describe('ConfigStyledYamlViewer', () => {
		let viewer: ConfigStyledYamlViewer;

		beforeEach(() => {
			viewer = new ConfigStyledYamlViewer(spyDisplay);
		})

		it('When given a config, outputs it as raw YAML with a leading newline', () => {
			// ==== TEST SETUP ====
			// Instantiate the config model.
			const configModel = new StubConfigModel();

			// ==== TESTED METHOD ====
			// Display the config
			viewer.view(configModel);

			// ==== ASSERTIONS ====
			const displayEvents = spyDisplay.getDisplayEvents();
			expect(displayEvents).toHaveLength(1);
			expect(displayEvents[0].type).toEqual(DisplayEventType.LOG);
			expect(ansis.strip(displayEvents[0].data)).toEqual(`\n# This is a leading comment\nResults formatted as YAML`);
		});
	});
});
