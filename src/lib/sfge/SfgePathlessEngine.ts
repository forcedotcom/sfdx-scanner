import {Logger} from '@salesforce/core';
import {BaseSfgeEngine, SfgeViolation} from './BaseSfgeEngine';
import {Controller} from '../../Controller';
import {RuleViolation} from '../../types';


export class SfgePathlessEngine extends BaseSfgeEngine {

	private initialized: boolean;

	/**
	 * Invokes sync/async initialization required for the engine
	 * @override
	 */
	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child("SfgePathlessEngine");
		this.config = await Controller.getConfig();
		this.initialized = true;
	}

	/**
	 * @override
	 */
	public isDfaEngine(): boolean {
		return false;
	}

	/**
	 * Convert one of SFGE's internal violations into a format
	 * that can be used in SFCA.
	 * @override
	 */
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
