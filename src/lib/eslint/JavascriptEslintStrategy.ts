import { EslintStrategy } from './BaseEslintEngine';
import {ENGINE, LANGUAGE, HARDCODED_RULES} from '../../Constants';
import {ESRule, ESRuleConfigValue, ESRuleConfig, RuleViolation} from '../../types';
import { Logger } from '@salesforce/core';
import {EslintStrategyHelper, ProcessRuleViolationType, RuleDefaultStatus} from './EslintCommons';
import {ESLint} from 'eslint';
import {configs} from '@eslint/js';

const ES_CONFIG: ESLint.Options = {
	"baseConfig": {},
	"overrideConfig": {
		"parser": "@babel/eslint-parser",
		"parserOptions": {
			"requireConfigFile": false,
			"babelOptions": {
				"parserOpts": {
					"plugins": ["classProperties", ["decorators", {"decoratorsBeforeExport": false}]]
				}
			}
		},
		"ignorePatterns": [
			"node_modules/!**"
		]
	},
	"useEslintrc": false, // Will not use an external config
	"resolvePluginsRelativeTo": __dirname, // Use the plugins found in the sfdx scanner installation directory
	"cwd": __dirname // Use the parser found in the sfdx scanner installation
};

export class JavascriptEslintStrategy implements EslintStrategy {
	private static LANGUAGES = [LANGUAGE.JAVASCRIPT];

	private initialized: boolean;
	private recommendedConfig: ESRuleConfig;
	protected logger: Logger;

	async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getEngine().valueOf());
		// When we're building our catalog, we'll want to get any bonus configuration straight from the horse's mouth.
		// This lets us do that.
		// eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
		this.recommendedConfig = configs.recommended as ESRuleConfig;
		this.initialized = true;
	}

	getLanguages(): string[] {
		return JavascriptEslintStrategy.LANGUAGES;
	}

	getEngine(): ENGINE {
		return ENGINE.ESLINT;
	}

	/* eslint-disable-next-line @typescript-eslint/no-unused-vars */
	getRunOptions(engineOptions: Map<string, string>): Promise<ESLint.Options> {
		return Promise.resolve(ES_CONFIG);
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

	getDefaultConfig(ruleName: string): ESRuleConfigValue {
		return EslintStrategyHelper.getDefaultConfig(this.recommendedConfig, ruleName);
	}

	filterDisallowedRules(rulesByName: Map<string, ESRule>): Map<string, ESRule> {
		return EslintStrategyHelper.filterDisallowedRules(rulesByName);
	}

	getRuleMap(): Map<string, ESRule> {
		return EslintStrategyHelper.filterDisallowedRules(EslintStrategyHelper.getBaseEslintRules());
	}

	processRuleViolation(): ProcessRuleViolationType {
		/* eslint-disable-next-line @typescript-eslint/no-unused-vars */
		return (fileName: string, ruleViolation: RuleViolation): void => {
			if (ruleViolation.message.startsWith('Parsing error:')) {
				ruleViolation.ruleName = HARDCODED_RULES.FILES_MUST_COMPILE.name;
				ruleViolation.category = HARDCODED_RULES.FILES_MUST_COMPILE.category;
			}
		}
	}
}
