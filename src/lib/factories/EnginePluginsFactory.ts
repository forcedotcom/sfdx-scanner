import {EnginePlugin} from '@salesforce/code-analyzer-engine-api';
import * as ESLintEngineModule from '@salesforce/code-analyzer-eslint-engine';
import * as RetireJSEngineModule from '@salesforce/code-analyzer-retirejs-engine';
import * as RegexEngineModule from '@salesforce/code-analyzer-regex-engine';

export interface EnginePluginsFactory {
	create(): EnginePlugin[];
}

export class EnginePluginsFactoryImpl implements EnginePluginsFactory {
	public create(): EnginePlugin[] {
		return [
			ESLintEngineModule.createEnginePlugin(),
			RetireJSEngineModule.createEnginePlugin(),
			RegexEngineModule.createEnginePlugin()
		];
	}
}
