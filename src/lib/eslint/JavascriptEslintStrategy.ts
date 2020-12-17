import { EslintStrategy } from './BaseEslintEngine';
import {ENGINE, LANGUAGE} from '../../Constants';
import {ESRule, ESRuleConfig, LooseObject, RuleViolation} from '../../types';
import { Logger } from '@salesforce/core';
import { EslintStrategyHelper, ProcessRuleViolationType } from './EslintCommons';
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
		// When we're building our catalog, we'll want to get any bonus configuration straight from the horse's mouth.
		// This lets us do that.
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
		return EslintStrategyHelper.isDefaultEnabled(this.recommendedConfig, name);
	}

	getDefaultConfig(ruleName: string): ESRuleConfig {
		return EslintStrategyHelper.getDefaultConfig(this.recommendedConfig, ruleName);
	}

	filterDisallowedRules(rulesByName: Map<string, ESRule>): Map<string, ESRule> {
		const filteredRules: Map<string,ESRule> = new Map();
		for (const [name, rule] of rulesByName.entries()) {
			// Keep all rules except the deprecated ones.
			if (!rule.meta.deprecated) {
				filteredRules.set(name, rule);
			}
		}
		return filteredRules;
	}

	processRuleViolation(): ProcessRuleViolationType {
		/* eslint-disable-next-line no-unused-vars, @typescript-eslint/no-unused-vars */
		return (fileName: string, ruleViolation: RuleViolation): void => {
			// Intentionally left blank
		}
	}
}
