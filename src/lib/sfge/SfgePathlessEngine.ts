import {SfdxError} from '@salesforce/core';
import {AbstractSfgeEngine, SfgeViolation} from "./AbstractSfgeEngine";
import {Rule, RuleGroup, RuleTarget, RuleViolation, SfgeConfig} from '../../types';
import {CUSTOM_CONFIG, RuleType} from '../../Constants';
import * as EngineUtils from "../util/CommonEngineUtils";


export class SfgePathlessEngine extends AbstractSfgeEngine {
	/**
	 * Helps decide if an instance of this engine should be included in a run/cataloging based on the values
	 * provided in the --engine filter and Engine Options.
	 * @override
	 */
	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public isEngineRequested(filterValues: string[], engineOptions: Map<string,string>): boolean {
		// The non-DFA variant must be explicitly requested via `--engine sfge`. Otherwise,
		// it should be excluded from cataloging and running.
		// NOTE: This was an intentional divergence from the DFA variant's behavior, as a consequence
		//       of the in-progress nature of many/most non-DFA rules. When we're more confident
		//       in the state of the engine, this method should be changed so the engine counts
		//       as requested-by-default the way its DFA cousin does.
		return EngineUtils.isValueInFilter(this.getName(), filterValues);
	}

	/**
	 * Helps make decision to run an engine or not based on the Rules, Target paths, and the Engine Options selected per
	 * run. At this point, filtering should have already happened.
	 * @override
	 */
	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public shouldEngineRun(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string,string>): boolean {
		// For the non-DFA Graph Engine variant, we need to make sure that we have the
		// necessary info to run the engine, since the relevant flags aren't required
		// for `scanner:run`.
		if (engineOptions.has(CUSTOM_CONFIG.SfgeConfig)) {
			const sfgeConfig: SfgeConfig = JSON.parse(engineOptions.get(CUSTOM_CONFIG.SfgeConfig)) as SfgeConfig;
			if (sfgeConfig.projectDirs && sfgeConfig.projectDirs.length > 0) {
				// If we've got a config with projectDirs, we're set.
				return true;
			}
		}
		// If we're here, it's because we're missing the necessary info to run this engine.
		// We should throw an error indicating this.
		throw SfdxError.create('@salesforce/sfdx-scanner', 'SfgeEngine', 'errors.failedWithoutProjectDir', []);
	}

	protected getSubVariantName(): string {
		return `${this.getName()}-${RuleType.PATHLESS}`;
	}

	protected getRuleType(): RuleType {
		return RuleType.PATHLESS;
	}

	public isDfaEngine(): boolean {
		return false;
	}

	protected convertViolation(sfgeViolation: SfgeViolation): RuleViolation {
		return {
			ruleName: sfgeViolation.ruleName,
			message: sfgeViolation.message,
			severity: sfgeViolation.severity,
			category: sfgeViolation.category,
			url: sfgeViolation.url,
			line: sfgeViolation.sourceLineNumber,
			column: sfgeViolation.sourceColumnNumber
		};
	}
}
