import { EslintStrategy } from './BaseEslintEngine';
import {ENGINE, LANGUAGE} from '../../Constants';
import {RuleViolation} from '../../types';
import { Logger } from '@salesforce/core';
import { ProcessRuleViolationType } from './EslintCommons';

const ES_CONFIG = {
	"baseConfig": {
		"extends": ["eslint:recommended"]
	},
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
	private static LANGUAGES = [LANGUAGE.JAVASCRIPT];

	private initialized: boolean;
	protected logger: Logger;

	async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getEngine().valueOf());
		this.initialized = true;
	}

	getLanguages(): string[] {
		return JavascriptEslintStrategy.LANGUAGES;
	}

	getEngine(): ENGINE {
		return ENGINE.ESLINT;
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

	processRuleViolation(): ProcessRuleViolationType {
		/* eslint-disable-next-line no-unused-vars, @typescript-eslint/no-unused-vars */
		return (fileName: string, ruleViolation: RuleViolation): void => {
			// Intentionally left blank
		}
	}
}
