import {AbstractSfgeEngine, SfgeViolation} from './AbstractSfgeEngine';
import {RuleType} from '../../Constants';
import {Rule, RuleGroup, RuleTarget, RuleViolation} from '../../types';


export class SfgeDfaEngine extends AbstractSfgeEngine {

	/**
	 * Helps make decision to run an engine or not based on the Rules, Target paths, and the Engine Options selected per
	 * run. At this point, filtering should have already happened.
	 * @override
	 */
	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public shouldEngineRun(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string,string>): boolean {
		// If the engine was requested, there's no reason not to run it.
		return true;
	}

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
