import {ConfigAction, ConfigDependencies, ConfigInput} from '../../../src/lib/actions/ConfigAction';
import {DummyConfigModel} from '../../../src/lib/models/ConfigModel';
import {SpyConfigViewer} from '../../stubs/SpyConfigViewer';
import {SpyConfigWriter} from '../../stubs/SpyConfigWriter';
import {SpyDisplay} from '../../stubs/SpyDisplay';

describe('ConfigAction tests', () => {
	describe('Output processing', () => {
		it('When a ConfigWriter is not provided, only the ConfigViewer is used', async () => {
			const viewer = new SpyConfigViewer();
			const dependencies: ConfigDependencies = {
				display: new SpyDisplay(),
				logEventListeners: [],
				viewer
			};

			const action = ConfigAction.createAction(dependencies);

			const input: ConfigInput = {
				'rule-selector': []
			};

			await action.execute(input);

			const viewerCallHistory = viewer.getCallHistory();
			expect(viewerCallHistory).toHaveLength(1);
			expect(viewerCallHistory[0]).toBeInstanceOf(DummyConfigModel);
		});

		it('When a ConfigWriter is provided, both it and the ConfigViewer are used', async () => {
			const viewer = new SpyConfigViewer();
			const writer = new SpyConfigWriter();
			const dependencies: ConfigDependencies = {
				display: new SpyDisplay(),
				logEventListeners: [],
				viewer,
				writer
			};

			const action = ConfigAction.createAction(dependencies);

			const input: ConfigInput = {
				'rule-selector': []
			};

			await action.execute(input);

			const viewerCallHistory = viewer.getCallHistory();
			expect(viewerCallHistory).toHaveLength(1);
			expect(viewerCallHistory[0]).toBeInstanceOf(DummyConfigModel);
			const writerCallHistory = writer.getCallHistory();
			expect(writerCallHistory).toHaveLength(1);
			expect(writerCallHistory[0]).toBeInstanceOf(DummyConfigModel);
		});
	});

	describe('Base config selection', () => {
		// TODO: THIS TEST MAY ULTIMATELY BELONG IN A HYPOTHETICAL `ConfigModelFactory.test.ts`
		xit('When no local config is available, the base config is used', () => {

		});

		// TODO: THIS TEST MAY ULTIMATELY BELONG IN A HYPOTHETICAL `ConfigModelFactory.test.ts`
		xit('When a local config is available, it is used instead of the base config', () => {

		});
	});

	describe('Config synthesis', () => {
		// TODO: THIS TEST MAY ULTIMATELY BELONG IN A HYPOTHETICAL `ConfigModelFactory.test.ts`
		xit('When no selectors or overrides are provided, the base config is used as-is', () => {

		});

		// TODO: THIS TEST MAY ULTIMATELY BELONG IN A HYPOTHETICAL `ConfigModelFactory.test.ts`
		xit('Rule selectors are properly synthesized with the base config', () => {

		});

		// TODO: THIS TEST MAY ULTIMATELY BELONG IN A HYPOTHETICAL `ConfigModelFactory.test.ts`
		xit('Workspace constraints are properly synthesized with the base config', () => {

		});
	});
})
