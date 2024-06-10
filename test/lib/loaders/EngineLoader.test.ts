import {EngineLoaderImpl} from '../../../src/lib/loaders/EngineLoader';


describe('EngineLoaderImpl', () => {
	it('Loads expected engines', () => {
		const engineLoader = new EngineLoaderImpl();
		const enginePlugins = engineLoader.loadEngines();

		expect(enginePlugins).toHaveLength(1);
		expect(enginePlugins[0].getAvailableEngineNames()).toEqual(['SampleEngine1', 'SampleEngine2']);
	});
});
