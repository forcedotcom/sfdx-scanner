import { EslintStrategy } from './BaseEslintEngine';
import {TYPESCRIPT_RULE_PREFIX, ENGINE, LANGUAGE} from '../../Constants';
import {Config} from '../util/Config';
import {Controller} from '../../Controller';
import { Logger } from '@salesforce/core';

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

export class JavascriptEslintStrategy implements EslintStrategy {
	private static THIS_ENGINE = ENGINE.ESLINT;
	private static ENGINE_NAME = JavascriptEslintStrategy.THIS_ENGINE.valueOf();
	private static LANGUAGES = [LANGUAGE.JAVASCRIPT];

	private initialized: boolean;
	protected logger: Logger;
	private config: Config;

	async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getName());
		this.config = await Controller.getConfig();
		this.initialized = true;
	}

	isEnabled(): boolean {
		return this.config.isEngineEnabled(JavascriptEslintStrategy.THIS_ENGINE);
	}

	getLanguages(): string[] {
		return JavascriptEslintStrategy.LANGUAGES;
	}

	getName(): string {
		return JavascriptEslintStrategy.ENGINE_NAME;
	}

	isRuleKeySupported(key: string): boolean {
		return !key.startsWith(TYPESCRIPT_RULE_PREFIX);
	}

	/* eslint-disable @typescript-eslint/no-explicit-any */
	getCatalogConfig(): Record<string,any> {
		return ES_CONFIG;
	}

	/* eslint-disable-next-line @typescript-eslint/no-explicit-any, no-unused-vars, @typescript-eslint/no-unused-vars, @typescript-eslint/require-await */
	async getRunConfig(engineOptions: Map<string, string>): Promise<Record<string, any>> {
		//TODO: find a way to override with eslintrc if Config asks for it
		return ES_CONFIG;
	}

	filterUnsupportedPaths(paths: string[]): string[] {
		// TODO: fill in the filtering logic - this method could be removed if we fix an issue with getTargetPatterns in TypescriptEslintStrategy
		return paths;
	}

	async getTargetPatterns(): Promise<string[]> {

		// TODO: extract target patterns from overridden config, if available
		return this.config.getTargetPatterns(ENGINE.ESLINT);
	}

	convertLintMessage(fileName: string, message: string): string {
		return message;
	}
}
