import {EnginePluginsFactoryImpl} from '../../../src/lib/factories/EnginePluginsFactory';


describe('EnginePluginsFactoryImpl', () => {
	it('Loads expected engines', () => {
		const pluginsFactory = new EnginePluginsFactoryImpl();
		const enginePlugins = pluginsFactory.create();

		expect(enginePlugins).toHaveLength(2);
		expect(enginePlugins[0].getAvailableEngineNames()).toEqual(['eslint']);
		expect(enginePlugins[1].getAvailableEngineNames()).toEqual(['retire-js']);
	});
});
