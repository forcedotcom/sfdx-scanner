import {EnginePlugin} from '@salesforce/code-analyzer-engine-api';
import {EnginePluginFactory} from '../../src/lib/factories/EnginePluginFactory';
import {FunctionalStubEnginePlugin1, ThrowingStubPlugin1} from './StubEnginePlugins';

export class StubEnginePluginFactory_withFunctionalStubEngine implements EnginePluginFactory {
	public create(): EnginePlugin[] {
		return [
			new FunctionalStubEnginePlugin1()
		];
	}
}

export class StubEnginePluginFactory_withPreconfiguredStubEngines implements EnginePluginFactory {
	private readonly enginePlugins: EnginePlugin[] = [];

	public addPreconfiguredEnginePlugin(plugin: EnginePlugin): void {
		this.enginePlugins.push(plugin);
	}

	public create(): EnginePlugin[] {
		return this.enginePlugins;
	}
}

export class StubEnginePluginFactory_withNoPlugins implements EnginePluginFactory {
	public create(): EnginePlugin[] {
		return [];
	}
}

export class StubEnginePluginFactory_withThrowingStubPlugin implements EnginePluginFactory {
	public create(): EnginePlugin[] {
		return [
			new ThrowingStubPlugin1()
		];
	}
}
