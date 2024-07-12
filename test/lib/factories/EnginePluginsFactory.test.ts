import {EnginePluginsFactoryImpl} from '../../../src/lib/factories/EnginePluginsFactory';


describe('EnginePluginsFactoryImpl', () => {
	it('Loads expected engines', () => {
		const pluginsFactory = new EnginePluginsFactoryImpl();
		const enginePlugins = pluginsFactory.create();

		expect(enginePlugins).toHaveLength(1);
		expect(enginePlugins[0].getAvailableEngineNames()).toEqual(['retire-js']);
	});
});
