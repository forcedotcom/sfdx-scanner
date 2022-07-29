import {BaseSfgeEngine, SfgeViolation} from './BaseSfgeEngine';
import {Logger} from '@salesforce/core';
import {Controller} from '../../Controller';
import {RuleViolation} from "../../types";

export class SfgeDfaEngine extends BaseSfgeEngine {

	private initialized: boolean;

	/**
	 * Invokes sync/async initialization required for the engine
	 * @override
	 */
	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child("SfgeDfaEngine");
		this.config = await Controller.getConfig();
		this.initialized = true;
	}

	/**
	 * @override
	 */
	public isDfaEngine(): boolean {
		return true;
	}

	/**
	 * Convert one of SFGE's internal violations into a format
	 * that can be used in SFCA.
	 * @override
	 */
	protected convertViolation(sfgeViolation: SfgeViolation): RuleViolation {
		return {
			ruleName: sfgeViolation.ruleName,
			severity: sfgeViolation.severity,
			message: sfgeViolation.message,
			category: sfgeViolation.category,
			url: sfgeViolation.url,
			sinkLine: sfgeViolation.sinkLineNumber || null,
			sinkColumn: sfgeViolation.sinkColumnNumber || null,
			sinkFileName: sfgeViolation.sinkFileName || "",
			sourceLine: sfgeViolation.sourceLineNumber,
			sourceColumn: sfgeViolation.sourceColumnNumber,
			sourceType: sfgeViolation.sourceType,
			sourceMethodName: sfgeViolation.sourceVertexName
		};
	}
}
