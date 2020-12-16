import { EslintStrategy } from './BaseEslintEngine';
import {ENGINE, LANGUAGE} from '../../Constants';
import {ESRule, RuleViolation} from '../../types';
import { Logger } from '@salesforce/core';
import { ProcessRuleViolationType } from './EslintCommons';

const ES_CONFIG = {
	"parser": "babel-eslint",
	"plugins": ["@lwc/eslint-plugin-lwc"],
	"baseConfig": {
		"extends": [
			"eslint:recommended",
			"@salesforce/eslint-config-lwc/base"
		]
	},
	"ignorePatterns": [
		"node_modules/!**"
	],
	"useEslintrc": false, // Will not use an external config
	"resolvePluginsRelativeTo": __dirname, // Use the plugins found in the sfdx scanner installation directory
	"cwd": __dirname // Use the parser found in the sfdx scanner installation
};

export class LWCEslintStrategy implements EslintStrategy {
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
		return LWCEslintStrategy.LANGUAGES;
	}

	getEngine(): ENGINE {
		return ENGINE.ESLINT_LWC;
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

	filterDisallowedRules(rulesByName: Map<string, ESRule>): Map<string, ESRule> {
		return rulesByName;
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
		}
		
	}
}
