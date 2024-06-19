import {EnginePlugin} from '@salesforce/code-analyzer-engine-api';
import {EnginePluginsFactory} from '../../src/lib/factories/EnginePluginsFactory';
import {FunctionalStubEnginePlugin1, ThrowingStubPlugin1} from './StubEnginePlugins';

export class StubEnginePluginsFactory_withFunctionalStubEngine implements EnginePluginsFactory {
	public create(): EnginePlugin[] {
		return [
			new FunctionalStubEnginePlugin1()
		];
	}
}

export class StubEnginePluginsFactory_withPreconfiguredStubEngines implements EnginePluginsFactory {
	private readonly enginePlugins: EnginePlugin[] = [];

	public addPreconfiguredEnginePlugin(plugin: EnginePlugin): void {
		this.enginePlugins.push(plugin);
	}

	public create(): EnginePlugin[] {
		return this.enginePlugins;
	}
}

export class StubEnginePluginsFactory_withNoPlugins implements EnginePluginsFactory {
	public create(): EnginePlugin[] {
		return [];
	}
}

export class StubEnginePluginsFactory_withThrowingStubPlugin implements EnginePluginsFactory {
	public create(): EnginePlugin[] {
		return [
			new ThrowingStubPlugin1()
		];
	}
}
