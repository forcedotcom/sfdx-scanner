import {RuleFilter} from '../RuleManager';
import {Rule} from '../../types';

/**
 * @param stringSet Set<string> to stringify
 */
export function stringifySet(stringSet: Set<string>): string {
	const returnArr = [...stringSet];
	return `[${returnArr.join(',')}]`;
}

/**
 * Typically used to print Language-RulePath mapping
 * @param mapOfSet Map<string, Set<string>> to stringify
 */
export function stringifyMapofSets(mapOfSet: Map<string, Set<string>>): string {
	const returnArr = [];
	mapOfSet.forEach((value, key) => {
		returnArr.push(`{${key} => ${stringifySet(value)}}`);
	});
	return `${returnArr.join(',')}`;
}

/**
 * Typically used to print Engine-Language-RulePath mapping
 * @param mapOfMap Map<string, Map<string, Set<string>>> to stringify
 */
export function stringifyMapOfMaps(mapOfMap: Map<string, Map<string, Set<string>>>): string {
	const returnArr = [];
	mapOfMap.forEach((value, key) => {
		returnArr.push(`{${key} => ${stringifyMapofSets(value)}}`);
	});
	return `${returnArr.join(',')}`;
}

/**
 * @param filter RuleFilter to stringify
 */
export function stringifyRuleFilter(filter: RuleFilter): string {
	return `RuleFilter[filterType=${filter.filterType}, filterValues=${filter.filterValues}]`;
}

/**
 * @param filters Array of RuleFilters to stringify
 */
export function stringifyRuleFilters(filters: RuleFilter[]): string {
	const returnArr = [];
	filters.forEach((filter) => {
		returnArr.push(stringifyRuleFilter(filter));
	});
	return `[${returnArr.join(',')}]`;
}

/**
 * @param rule Rule to stringify
 */
export function stringifyRule(rule: Rule): string {
	return `Rule[name: ${rule.name}, description: ${rule.description}, categories: ${rule.categories.join(',')}, rulesets: ${rule.rulesets.join(',')}, languages: ${rule.languages.join(',')}]`;
}

/**
 * @param rules Array of Rule to stringify
 */
export function stringifyRules(rules: Rule[]): string {
	const returnArr = [];
	rules.forEach((rule) => {
		returnArr.push(stringifyRule(rule));
	});
	return `[${returnArr.join(',')}]`;
}
