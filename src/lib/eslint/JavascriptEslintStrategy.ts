import { EslintStrategy } from './BaseEslintEngine';
import {ENGINE, LANGUAGE, HARDCODED_RULES} from '../../Constants';
import {ESRule, ESRuleConfig, LooseObject, RuleViolation} from '../../types';
import { Logger } from '@salesforce/core';
import {EslintStrategyHelper, ProcessRuleViolationType, RuleDefaultStatus} from './EslintCommons';
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
		// Since Vanilla ESLint only has one configuration to consult, if that config doesn't explicitly enable the rule,
		// it's treated as disabled.
		return EslintStrategyHelper.getDefaultStatus(this.recommendedConfig, name) === RuleDefaultStatus.ENABLED;
	}

	getDefaultConfig(ruleName: string): ESRuleConfig {
		return EslintStrategyHelper.getDefaultConfig(this.recommendedConfig, ruleName);
	}

	filterDisallowedRules(rulesByName: Map<string, ESRule>): Map<string, ESRule> {
		return EslintStrategyHelper.filterDisallowedRules(rulesByName);
	}


	processRuleViolation(): ProcessRuleViolationType {
		/* eslint-disable-next-line no-unused-vars, @typescript-eslint/no-unused-vars */
		return (fileName: string, ruleViolation: RuleViolation): void => {
			if (ruleViolation.message.startsWith('Parsing error:')) {
				ruleViolation.ruleName = HARDCODED_RULES.FILES_MUST_COMPILE.name;
				ruleViolation.category = HARDCODED_RULES.FILES_MUST_COMPILE.category;
			}
		}
	}
}
