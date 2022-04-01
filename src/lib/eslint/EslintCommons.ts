import { Linter, ESLint } from 'eslint';
import * as path from 'path';
import { CUSTOM_CONFIG } from '../../Constants';
import { RuleResult, RuleViolation, ESRule, ESRuleMetadata, ESRuleConfig, ESRuleConfigValue } from '../../types';
import { FileHandler } from '../util/FileHandler';
import * as engineUtils from '../util/CommonEngineUtils';
import {stringArrayTypeGuard} from '../util/Utils';


// Defining a function signature that will be returned by EslintStrategy.processRuleViolation()
// This provides a safe way to pass around the callback function
export interface ProcessRuleViolationType { (fileName: string, ruleViolation: RuleViolation): void}

export enum RuleDefaultStatus {
	ENABLED = 'enabled',
	DISABLED = 'disabled'
}


export class StaticDependencies {
	/* eslint-disable @typescript-eslint/no-explicit-any */
	createESLint(config: Record<string, any>): ESLint {
		return new ESLint(config);
	}

	resolveTargetPath(target: string): string {
		return path.resolve(target);
	}

	getCurrentWorkingDirectory(): string {
		return process.cwd();
	}

	getFileHandler(): FileHandler {
		return new FileHandler();
	}
}

export class EslintStrategyHelper {
	/**
	 * Get the rules available in vanilla ESLint, mapped by name.
	 */
	static getBaseEslintRules(): Map<string,ESRule> {
		return new Linter().getRules();
	}

	static filterDisallowedRules(rulesByName: Map<string,ESRule>): Map<string,ESRule> {
		const filteredRules: Map<string,ESRule> = new Map();
		for (const [name, rule] of rulesByName.entries()) {
			// Keep all rules except the deprecated ones.
			if (!rule.meta.deprecated) {
				filteredRules.set(name, rule);
			}
		}
		return filteredRules;
	}

	static getDefaultStatus(recommendedConfig: ESRuleConfig, ruleName: string): RuleDefaultStatus {
		// If a rule is absent from the "recommended" configuration, then its status could be inherited from another config.
		// To represent the unknown state, we'll just return null.

		// See if this configuration has an entry for the rule in question.
		const recommendation: ESRuleConfigValue = recommendedConfig.rules[ruleName];
		if (!recommendation) {
			// If the rule is absent from the config, its status can be inherited from other configs. To represent this
			// ambiguous state, return null.
			return null;
		} else if (typeof recommendation == 'string') {
			// If the recommendation is a string, it could be "off", in which case the rule is disabled. Otherwise, it's enabled.
			return recommendation === 'off' ? RuleDefaultStatus.DISABLED : RuleDefaultStatus.ENABLED;
		} else if (stringArrayTypeGuard(recommendation) && recommendation.length === 1) {
			// If the recommendation is a length-1 string array, check whether that value is "off".
			return recommendation[0] === 'off' ? RuleDefaultStatus.DISABLED : RuleDefaultStatus.ENABLED;
		} else {
			// For any other case, the rule is some kind of enabled.
			return RuleDefaultStatus.ENABLED;
		}
	}

	static getDefaultConfig(recommendedConfig: ESRuleConfig, ruleName: string): ESRuleConfigValue {
		const recommendation: ESRuleConfigValue = recommendedConfig.rules[ruleName];
		// If there's no recommended config, then return null.
		if (!recommendation) {
			return null;
		}
		// BASE ASSUMPTION: If the config specifies a rule as "off", it is unlikely to also specify a meaningful default
		// config for that rule. So we can return null for any rules set to "off".
		if ((typeof recommendation === 'string' && recommendation === 'off') || (Array.isArray(recommendation) && recommendation.indexOf('off') === 0)) {
			return null;
		} else {
			return recommendation;
		}
	}
}

export class EslintProcessHelper {

	isCustomRun(engineOptions: Map<string, string>): boolean {
		return engineUtils.isCustomRun(CUSTOM_CONFIG.EslintConfig, engineOptions);
	}

	addRuleResultsFromReport(
		engineName: string,
		results: RuleResult[],
		esResults: ESLint.LintResult[],
		ruleMap: Map<string,ESRuleMetadata>,
		processRuleViolation: (fileName: string, ruleViolation: RuleViolation) => void): void {
		esResults.forEach(r => {
			if (r.messages && r.messages.length > 0) {
				results.push(this.toRuleResult(engineName, r.filePath, r.messages, ruleMap, processRuleViolation));
			}
		});
	}

	toRuleResult(
		engineName: string,
		fileName: string,
		messages: Linter.LintMessage[],
		ruleMap: Map<string, ESRuleMetadata>,
		processRuleViolation: (fileName: string, ruleViolation: RuleViolation) => void): RuleResult {
		return {
			engine: engineName,
			fileName,
			violations: messages.map(
				(v: Linter.LintMessage): RuleViolation => {
					const ruleMeta = ruleMap.get(v.ruleId);
					const category = ruleMeta ? ruleMeta.type : "problem";
					const url = ruleMeta ? ruleMeta.docs.url : "";
					const violation: RuleViolation = {
						line: v.line,
						column: v.column,
						severity: v.severity,
						message: v.message,
						ruleName: v.ruleId,
						category,
						url
					};

					processRuleViolation(fileName, violation);

					return violation;
				}
			)
		}
	}

}

