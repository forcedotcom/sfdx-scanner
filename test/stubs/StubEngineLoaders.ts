import {EnginePlugin} from '@salesforce/code-analyzer-engine-api';
import {EngineLoader} from '../../src/lib/loaders/EngineLoader';
import {FunctionalStubEnginePlugin1, ThrowingStubPlugin1} from './StubEnginePlugins';

export class StubEngineLoader_withFunctionalStubEngine implements EngineLoader {
	public loadEngines(): EnginePlugin[] {
		return [
			new FunctionalStubEnginePlugin1()
		];
	}
}

export class StubEngineLoader_withNoPlugins implements EngineLoader {
	public loadEngines(): EnginePlugin[] {
		return [];
	}
}

export class StubEngineLoader_withThrowingStubPlugin implements EngineLoader {
	public loadEngines(): EnginePlugin[] {
		return [
			new ThrowingStubPlugin1()
		];
	}
}
