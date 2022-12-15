import {AbstractSfgeEngine, SfgeViolation} from "./AbstractSfgeEngine";
import {RuleViolation} from '../../types';
import {RuleType} from '../../Constants';

export class SfgePathlessEngine extends AbstractSfgeEngine {

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
