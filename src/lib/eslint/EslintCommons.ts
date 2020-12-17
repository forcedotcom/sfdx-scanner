import { CLIEngine } from 'eslint';
import * as path from 'path';
import { CUSTOM_CONFIG } from '../../Constants';
import { LooseObject, RuleResult, RuleViolation, ESMessage, ESRule, ESRuleConfig, ESReport } from '../../types';
import { FileHandler } from '../util/FileHandler';
import * as engineUtils from '../util/CommonEngineUtils';


// Defining a function signature that will be returned by EslintStrategy.processRuleViolation()
// This provides a safe way to pass around the callback function
export interface ProcessRuleViolationType { (fileName: string, ruleViolation: RuleViolation): void}


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
	static isDefaultEnabled(recommendedConfig: LooseObject, ruleName: string): boolean {
		// ESLint rules are considered "enabled by default" as long as they're present in the "recommended" config with
		// a severity of anything other than "off".
		const recommendation: ESRuleConfig = recommendedConfig.rules[ruleName];
		return recommendation && !(recommendation.indexOf('off') === 0);
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

