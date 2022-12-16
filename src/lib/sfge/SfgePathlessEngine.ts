import {SfdxError, Messages} from '@salesforce/core';
import {AbstractSfgeEngine, SfgeViolation} from "./AbstractSfgeEngine";
import {Rule, RuleGroup, RuleTarget, RuleViolation, SfgeConfig} from '../../types';
import {CUSTOM_CONFIG, MissingOptionsBehavior, RuleType} from '../../Constants';
import {uxEvents, EVENTS} from '../ScannerEvents';

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'SfgeEngine');

export class SfgePathlessEngine extends AbstractSfgeEngine {
	private missingOptionsBehavior: MissingOptionsBehavior;

	/**
	 * Invokes sync/async initialization required for the engine.
	 * @override
	 */
	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		await super.init();
		this.missingOptionsBehavior = await this.config.getMissingOptionsBehavior(SfgePathlessEngine.ENGINE_ENUM);
		this.initialized = true;
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
		switch (this.missingOptionsBehavior) {
			case MissingOptionsBehavior.HALT:
				throw SfdxError.create('@salesforce/sfdx-scanner', 'SfgeEngine', 'errors.failedWithoutProjectDir', []);
			case MissingOptionsBehavior.WARN:
				uxEvents.emit(
					EVENTS.WARNING_ALWAYS,
					messages.getMessage('warnings.skippedWithoutProjectDir', [
						this.getName(),
						'missingOptionsBehavior',
						MissingOptionsBehavior.WARN,
						this.config.getConfigFilePath()
					]
				));
				return false;
			default:
				// We know the enum is valid, so the only other option is SKIP, which just means we skip silently.
				return false;
		}
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
