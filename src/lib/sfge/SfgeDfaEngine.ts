import {BaseSfgeEngine, SfgeViolation} from './BaseSfgeEngine';
import {Logger} from '@salesforce/core';
import {Controller} from '../../Controller';
import {RuleViolation} from "../../types";
import {ENGINE} from '../../Constants';

export class SfgeDfaEngine extends BaseSfgeEngine {
	private static ENGINE_ENUM: ENGINE = ENGINE.SFGE_DFA;
	private static ENGINE_NAME: string = ENGINE.SFGE_DFA.valueOf();

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
	 * Returns the name of the engine as referenced everywhere within the code.
	 * @override
	 */
	public getName(): string {
		// NOTE: Both engines can share the same name without issue.
		return SfgeDfaEngine.ENGINE_NAME;
	}

	/**
	 * @override
	 * @protected
	 */
	protected getEnum(): ENGINE {
		return SfgeDfaEngine.ENGINE_ENUM;
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
