import { EslintStrategy } from './BaseEslintEngine';
import {ENGINE, LANGUAGE, HARDCODED_RULES} from '../../Constants';
import {ESRule, ESRuleConfig, LooseObject, RuleViolation} from '../../types';
import { Logger } from '@salesforce/core';
import {EslintStrategyHelper, ProcessRuleViolationType, RuleDefaultStatus} from './EslintCommons';
import { rules } from '@lwc/eslint-plugin-lwc';

const ES_CONFIG = {
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
		"plugins": ["@lwc/eslint-plugin-lwc"],
		"ignorePatterns": [
			"node_modules/!**"
		]
	},
	"useEslintrc": false, // Will not use an external config
	"resolvePluginsRelativeTo": __dirname, // Use the plugins found in the sfdx scanner installation directory
	"cwd": __dirname // Use the parser found in the sfdx scanner installation
};

const RULE_PREFIX = "@lwc/lwc";

export class LWCEslintStrategy implements EslintStrategy {
	private static LANGUAGES = [LANGUAGE.JAVASCRIPT];

	private initialized: boolean;
	protected logger: Logger;
	private recommendedConfig: LooseObject;

	async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getEngine().valueOf());
		const pathToRecommendedConfig = require.resolve('@salesforce/eslint-config-lwc')
			.replace('index.js', 'base.js');
		this.recommendedConfig = require(pathToRecommendedConfig);
		this.initialized = true;
	}

	getLanguages(): string[] {
		return LWCEslintStrategy.LANGUAGES;
	}

	getEngine(): ENGINE {
		return ENGINE.ESLINT_LWC;
	}

	/* eslint-disable-next-line @typescript-eslint/no-explicit-any, no-unused-vars, @typescript-eslint/no-unused-vars, @typescript-eslint/require-await */
	async getRunConfig(engineOptions: Map<string, string>): Promise<Record<string, any>> {
		return ES_CONFIG;
	}

	filterUnsupportedPaths(paths: string[]): string[] {
		// TODO: fill in the filtering logic - this method could be removed if we fix an issue with getTargetPatterns in TypescriptEslintStrategy
		return paths;
	}

	filterDisallowedRules(rulesByName: Map<string, ESRule>): Map<string, ESRule> {
		return EslintStrategyHelper.filterDisallowedRules(rulesByName);
	}

	ruleDefaultEnabled(name: string): boolean {
		return EslintStrategyHelper.getDefaultStatus(this.recommendedConfig, name) === RuleDefaultStatus.ENABLED;
	}

	getDefaultConfig(ruleName: string): ESRuleConfig {
		return EslintStrategyHelper.getDefaultConfig(this.recommendedConfig, ruleName);
	}

	getRuleMap(): Map<string, ESRule> {
		const unfilteredRules: Map<string,ESRule> = EslintStrategyHelper.getBaseEslintRules();

		// Add all LWC-specific rules to the map.
		for (const key of Object.keys(rules)) {
			// NOTE: LWC rules have no `type` attribute by default. We're going to default them to `problem` for now.
			const typedRule = {...rules[key]};
			typedRule.meta.type = 'problem';
			unfilteredRules.set(`${RULE_PREFIX}/${key}`, typedRule);
		}

		return this.filterDisallowedRules(unfilteredRules);
	}

	// TODO: Submit PR against elsint-plugin-lwc
	processRuleViolation(): ProcessRuleViolationType {
		return (fileName: string, ruleViolation: RuleViolation): void => {
			const url: string = ruleViolation.url || '';
			const repoName = 'eslint-plugin-lwc';

			if (url.includes(repoName)) {
				// The meta.docs.url parameter for eslint-lwc is incorrect. It contains '.git' in the command line and is missing the 'v' for version
				// https://github.com/salesforce/eslint-plugin-lwc.git/blob/0.10.0/docs/rules/no-unknown-wire-adapters.md to
				// https://github.com/salesforce/eslint-plugin-lwc/blob/v0.10.0/docs/rules/no-unknown-wire-adapters.md

				// Remove the .git
				let newUrl = url.replace(/eslint-plugin-lwc\.git/, repoName);
				// Convert the append 'v' to the version if it is missing
				newUrl = newUrl.replace(/\/blob\/([0-9])/, '/blob/v$1');
				ruleViolation.url = newUrl;
			}
			if (ruleViolation.message.startsWith('Parsing error:')) {
				ruleViolation.ruleName = HARDCODED_RULES.FILES_MUST_COMPILE.name;
				ruleViolation.category = HARDCODED_RULES.FILES_MUST_COMPILE.category;
				ruleViolation.message = ruleViolation.message.split('\n')[0];
			}
		}
	}
}
