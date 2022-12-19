import {AbstractSfgeEngine, SfgeViolation} from './AbstractSfgeEngine';
import {RuleType} from '../../Constants';
import {RuleViolation} from '../../types';


export class SfgeDfaEngine extends AbstractSfgeEngine {

	protected getSubVariantName(): string {
		return `${this.getName()}-${RuleType.DFA}`;
	}

	protected getRuleType(): RuleType {
		return RuleType.DFA;
	}

	public isDfaEngine(): boolean {
		return true;
	}

	/**
	 * Convert one of GraphEngine's internal violations into a format usable by CodeAnalyzer.
	 * @override
	 * @protected
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
