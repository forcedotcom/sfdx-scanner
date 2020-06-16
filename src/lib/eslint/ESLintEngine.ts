import { BaseEslintEngine } from './BaseEslintEngine';
import {TYPESCRIPT_RULE_PREFIX} from '../../Constants';
import { Rule } from '../../types';
import {Config} from '../util/Config';
import {Controller} from '../../ioc.config';

const ES_CONFIG = {
	"extends": ["eslint:recommended"],
	"parserOptions": {
		"sourceType": "module",
		"ecmaVersion": 2018,
	},
	"ignorePatterns": [
		"node_modules/!**"
	],
	"useEslintrc": false // TODO derive from existing eslintrc if found and desired
};

export class EslintEngine extends BaseEslintEngine {
	private static ENGINE_NAME = "eslint";
	private static LANGUAGE = ["javascript", "typescript"];

	private config: Config;

	public async init(): Promise<void> {
		await super.init();
		this.config = await Controller.getConfig();
	}

	public isEnabled(): boolean {
		return this.config.isEngineEnabled(this.getName());
	}

	getLanguage(): string[] {
		return EslintEngine.LANGUAGE;
	}

	public getName(): string {
		return EslintEngine.ENGINE_NAME;
	}

	protected isRuleKeySupported(key: string): boolean {
		return !key.startsWith(TYPESCRIPT_RULE_PREFIX);
	}

	getCatalogConfig(): Object {
		return ES_CONFIG;
	}

	isRuleRelevant(rule: Rule) {
		// TODO: is this sufficient or do we need to check something else?
		return this.getName() === rule.engine;
	}

	async getRunConfig(target?: string): Promise<Object> {
		//TODO: find a way to override with eslintrc if Config asks for it
		return ES_CONFIG;
	}

	filterUnsupportedPaths(paths: string[]): string[] {
		// TODO: fill in the filtering logic
		return paths;
	}

	async getTargetPatterns(target?: string): Promise<string[]> {
		const engineConfig = this.config.getEngineConfig(this.getName());

		// TODO: extract target patterns from overridden config, if available
		return engineConfig.targetPatterns/*.concat(targetPatterns)*/;
	}
}