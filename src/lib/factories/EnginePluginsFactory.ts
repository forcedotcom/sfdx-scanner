import {EnginePlugin} from '@salesforce/code-analyzer-engine-api';
import {SampleEnginePlugin} from '../../tmp/SampleEnginePlugin';

export interface EnginePluginsFactory {
	create(): EnginePlugin[];
}

export class EnginePluginsFactoryImpl implements EnginePluginsFactory {
	public create(): EnginePlugin[] {
		return [
			new SampleEnginePlugin()
		];
	}
}
