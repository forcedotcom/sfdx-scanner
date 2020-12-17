import { EslintStrategy } from './BaseEslintEngine';
import {ENGINE, LANGUAGE} from '../../Constants';
import {ESRule, LooseObject, RuleViolation} from '../../types';
import { Logger } from '@salesforce/core';
import { ProcessRuleViolationType } from './EslintCommons';
import path = require('path');

const ES_CONFIG = {
	"baseConfig": {},
	"parserOptions": {
		"sourceType": "module",
		"ecmaVersion": 2018,
	},
	"ignorePatterns": [
		"node_modules/!**"
	],
	"useEslintrc": false // Will not use an external config
};

export class JavascriptEslintStrategy implements EslintStrategy {
	private static LANGUAGES = [LANGUAGE.JAVASCRIPT];

	private initialized: boolean;
	private recommendedConfig: LooseObject;
	protected logger: Logger;

	async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getEngine().valueOf());
		const pathToRecommendedConfig = require.resolve('eslint').replace(path.join('lib', 'api.js'), path.join('conf', 'eslint-recommended.js'));
		this.recommendedConfig = require(pathToRecommendedConfig);
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
		return ES_CONFIG;
	}

	filterUnsupportedPaths(paths: string[]): string[] {
		// TODO: fill in the filtering logic - this method could be removed if we fix an issue with getTargetPatterns in TypescriptEslintStrategy
		return paths;
	}

	ruleDefaultEnabled(name: string): boolean {
		const recommendation = this.recommendedConfig.rules[name];
		return recommendation && recommendation !== 'off';
	}

	getDefaultConfig(ruleName: string): LooseObject {
		return null;
	}

	filterDisallowedRules(rulesByName: Map<string, ESRule>): Map<string, ESRule> {
		return rulesByName;
	}

	processRuleViolation(): ProcessRuleViolationType {
		/* eslint-disable-next-line no-unused-vars, @typescript-eslint/no-unused-vars */
		return (fileName: string, ruleViolation: RuleViolation): void => {
			// Intentionally left blank
		}
	}
}
