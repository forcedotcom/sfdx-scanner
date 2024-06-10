import {EnginePlugin} from '@salesforce/code-analyzer-engine-api';
import {SampleEnginePlugin} from '../../tmp/SampleEnginePlugin';

export interface EngineLoader {
	loadEngines(): EnginePlugin[];
}

export class EngineLoaderImpl implements EngineLoader {
	public loadEngines(): EnginePlugin[] {
		return [
			new SampleEnginePlugin()
		];
	}
}
