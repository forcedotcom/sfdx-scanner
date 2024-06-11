import {EnginePlugin} from '@salesforce/code-analyzer-engine-api';
import {SampleEnginePlugin} from '../../tmp/SampleEnginePlugin';

export interface EnginePluginFactory {
	create(): EnginePlugin[];
}

export class EnginePluginFactoryImpl implements EnginePluginFactory {
	public create(): EnginePlugin[] {
		return [
			new SampleEnginePlugin()
		];
	}
}
