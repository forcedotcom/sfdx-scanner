import {EnginePluginFactoryImpl} from '../../../src/lib/factories/EnginePluginFactory';


describe('EnginePluginFactoryImpl', () => {
	it('Loads expected engines', () => {
		const engineFactory = new EnginePluginFactoryImpl();
		const enginePlugins = engineFactory.create();

		expect(enginePlugins).toHaveLength(1);
		expect(enginePlugins[0].getAvailableEngineNames()).toEqual(['SampleEngine1', 'SampleEngine2']);
	});
});
