import { CLIEngine } from 'eslint';
import * as path from 'path';
import { CUSTOM_CONFIG } from '../../Constants';
import { RuleResult, RuleViolation, ESMessage, ESRule, ESReport } from '../../types';


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
}

export class EslintProcessHelper {
	// TODO: move to common code
	isCustomRun(engineOptions: Map<string, string>): boolean {
		return engineOptions.has(CUSTOM_CONFIG.EslintConfig);
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

					// TODO: when moving to a common logic, find a way to handle this missing step
					// this.strategy.processRuleViolation(fileName, violation);
					processRuleViolation(fileName, violation);

					return violation;
				}
			)
		};
	}

}

