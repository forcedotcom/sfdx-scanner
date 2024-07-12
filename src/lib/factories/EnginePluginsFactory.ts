import {EnginePlugin} from '@salesforce/code-analyzer-engine-api';
import * as ESLintEngine from '@salesforce/code-analyzer-eslint-engine';
import * as RetireJSEngine from '@salesforce/code-analyzer-retirejs-engine';

export interface EnginePluginsFactory {
	create(): EnginePlugin[];
}

export class EnginePluginsFactoryImpl implements EnginePluginsFactory {
	public create(): EnginePlugin[] {
		return [
			ESLintEngine.createEnginePlugin(),
			RetireJSEngine.createEnginePlugin()
		];
	}
}
