import { CLIEngine } from 'eslint';
import * as path from 'path';
import { CUSTOM_CONFIG } from '../../Constants';
import { LooseObject, RuleResult, RuleViolation, ESMessage, ESRule, ESRuleConfig, ESReport } from '../../types';
import { FileHandler } from '../util/FileHandler';
import * as engineUtils from '../util/CommonEngineUtils';


// Defining a function signature that will be returned by EslintStrategy.processRuleViolation()
// This provides a safe way to pass around the callback function
export interface ProcessRuleViolationType { (fileName: string, ruleViolation: RuleViolation): void}

export enum RuleDefaultStatus {
	ENABLED = 'enabled',
	DISABLED = 'disabled'
}


export class StaticDependencies {
	/* eslint-disable @typescript-eslint/no-explicit-any */
	createCLIEngine(config: Record<string, any>): CLIEngine {
		return new CLIEngine(config);
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

	static getDefaultStatus(recommendedConfig: LooseObject, ruleName: string): RuleDefaultStatus {
		// If a rule is absent from the "recommended" configuration, then its status could be inherited from another config.
		// To represent the unknown state, we'll just return null.

		// See if this configuration has an entry for the rule in question.
		const recommendation: ESRuleConfig = recommendedConfig.rules[ruleName];
		if (!recommendation) {
			// If the rule is absent from the config, its status can be inherited from other configs. To represent this
			// ambiguous state, return null.
			return null;
		}
		// If there's a recommendation, then we'll treat the rule as default-enabled unless the recommended severity is
		// "off".
		return recommendation.indexOf('off') === 0 ? RuleDefaultStatus.DISABLED : RuleDefaultStatus.ENABLED;
	}

	static getDefaultConfig(recommendedConfig: LooseObject, ruleName: string): ESRuleConfig {
		const recommendation: ESRuleConfig = recommendedConfig.rules[ruleName];
		// BASE ASSUMPTION: If the config specifies a rule as "off", it is unlikely to also specify a meaningful default
		// config for that rule. So we can return null for any rules set to "off".
		if (!recommendation || recommendation.indexOf('off') === 0) {
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
		report: ESReport, 
		ruleMap: Map<string, ESRule>,
		processRuleViolation: (fileName: string, ruleViolation: RuleViolation) => void): void {
			for (const r of report.results) {
			// Only add report entries that have actual violations to report.
			if (r.messages && r.messages.length > 0) {
				results.push(this.toRuleResult(engineName, r.filePath, r.messages, ruleMap, processRuleViolation));
			}
		}
	}

	toRuleResult(
		engineName: string,
		fileName: string, 
		messages: ESMessage[], 
		ruleMap: Map<string, ESRule>,
		processRuleViolation: (fileName: string, ruleViolation: RuleViolation) => void): RuleResult {
		return {
			engine: engineName,
			fileName,
			violations: messages.map(
				(v): RuleViolation => {
					const rule = ruleMap.get(v.ruleId);
					const category = rule ? rule.meta.docs.category : "";
					const url = rule ? rule.meta.docs.url : "";
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
		};
	}

}

