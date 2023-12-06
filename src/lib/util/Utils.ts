import {RuleViolation, PathlessRuleViolation} from '../../types';

/**
 * Create a deep copy of an object by converting it to a JSON string and then parsing it.
 */
const deepCopy = <T>(obj: T): T => {
	return JSON.parse(JSON.stringify(obj)) as T;
}

const stringArrayTypeGuard = (object): object is string[] => {
	if (object == null || !Array.isArray(object)) {
		return false;
	}
	return !object.some(entry => typeof entry !== 'string');
}

const isPathlessViolation = (v: RuleViolation): v is PathlessRuleViolation => {
	return 'line' in v;
}

export { deepCopy, stringArrayTypeGuard, isPathlessViolation };
