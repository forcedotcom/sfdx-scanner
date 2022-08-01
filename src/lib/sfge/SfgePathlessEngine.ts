import {Logger} from '@salesforce/core';
import {BaseSfgeEngine, SfgeViolation} from './BaseSfgeEngine';
import {Controller} from '../../Controller';
import {RuleViolation} from '../../types';
import {ENGINE} from '../../Constants';


export class SfgePathlessEngine extends BaseSfgeEngine {
	private static ENGINE_ENUM: ENGINE = ENGINE.SFGE;
	private static ENGINE_NAME: string = ENGINE.SFGE.valueOf();

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
	 * Returns the name of the engine as referenced everywhere within the code.
	 * @override
	 */
	public getName(): string {
		// NOTE: Both engines can share the same name without issue.
		return SfgePathlessEngine.ENGINE_NAME;
	}

	/**
	 * @override
	 * @protected
	 */
	protected getEnum(): ENGINE {
		return SfgePathlessEngine.ENGINE_ENUM;
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
