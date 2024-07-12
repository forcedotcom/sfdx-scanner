import {EnginePlugin} from '@salesforce/code-analyzer-engine-api';
import * as RetireJSEngine from '@salesforce/code-analyzer-retirejs-engine';

export interface EnginePluginsFactory {
	create(): EnginePlugin[];
}

export class EnginePluginsFactoryImpl implements EnginePluginsFactory {
	public create(): EnginePlugin[] {
		return [
			RetireJSEngine.createEnginePlugin()
		];
	}
}
