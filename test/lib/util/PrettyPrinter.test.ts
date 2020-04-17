import {expect} from 'chai';
import * as PrettyPrinter from '../../../src/lib/util/PrettyPrinter';
import {RuleFilter, RULE_FILTER_TYPE} from '../../../src/lib/RuleManager';

describe(('PrettyPrinter tests'), () => {
	it('should print Set<string>', () => {
		const {setOfString, expectedString} = createSet();
		expect(PrettyPrinter.stringifySet(setOfString)).equals(expectedString);
	});

	it('should print Map<string, Set<string>>', () => {
		const {mapOfSet, expectedMapString} = createMapOfSet();
		expect(PrettyPrinter.stringifyMapofSets(mapOfSet)).equals(expectedMapString);
	});

	it('should print Map<string, Map<string, Set<string>>>', () => {
		const {mapOfMapOfSet, expectedMapOfMapString} = createMapOfMapOfSet();
		expect(PrettyPrinter.stringifyMapOfMaps(mapOfMapOfSet)).equals(expectedMapOfMapString);
	});

	it('should print a RuleFilter', () => {
		const {ruleFilter, expectedRuleFilterString} = createRuleFilter(RULE_FILTER_TYPE.CATEGORY);
		expect(PrettyPrinter.stringifyRuleFilter(ruleFilter)).equals(expectedRuleFilterString);
	});

	it('should print RuleFilter array', () => {
		const {ruleFilters, expectedRuleFiltersString} = createRuleFilters();
		expect(PrettyPrinter.stringifyRuleFilters(ruleFilters)).equals(expectedRuleFiltersString);
	});

	it('should print a Rule', () => {
		const {rule, expectedRuleString} = createRule();
		expect(PrettyPrinter.stringifyRule(rule)).equals(expectedRuleString);
	});

	it('should print a Rule array', () => {
		const {rules, expectedRulesString} = createRules();
		expect(PrettyPrinter.stringifyRules(rules)).equals(expectedRulesString);
	});
});

function createRuleFilter(filterType: RULE_FILTER_TYPE) {
	const expectedRuleFilterString = `RuleFilter[filterType=${filterType}, filterValues=Rule1,Rule2]`;
	const ruleFilter = new RuleFilter(filterType, ['Rule1', 'Rule2']);
	return {ruleFilter, expectedRuleFilterString};
}

function createRuleFilters() {
	const ruleFilter1 = createRuleFilter(RULE_FILTER_TYPE.RULENAME);
	const ruleFilter2 = createRuleFilter(RULE_FILTER_TYPE.LANGUAGE);
	const ruleFilters: RuleFilter[] = [ruleFilter1.ruleFilter, ruleFilter2.ruleFilter];
	const expectedRuleFiltersString = `[${ruleFilter1.expectedRuleFilterString},${ruleFilter2.expectedRuleFilterString}]`;
	return {ruleFilters, expectedRuleFiltersString};
}

function createRule() {
	const rule = {
		name: 'RuleName1',
		description: 'rule description',
		categories: ['Category1', 'Category2'],
		rulesets: ['Ruleset1', 'Ruleset2'],
		languages: ['apex', 'javascript'],
		sourcepackage: '/some/path/to/a/package.jar'
	};
	const expectedRuleString = 'Rule[name: RuleName1, description: rule description, categories: Category1,Category2, rulesets: Ruleset1,Ruleset2, languages: apex,javascript, sourcepackage: /some/path/to/a/package.jar]';
	return {rule, expectedRuleString};
}

function createRules() {
	const rule1 = createRule();
	const rule2 = createRule();
	const rules = [rule1.rule, rule2.rule];
	const expectedRulesString = `[${rule1.expectedRuleString},${rule2.expectedRuleString}]`;
	return {rules, expectedRulesString};
}

function createSet() {
	const setOfString = new Set<string>();
	setOfString.add('value1');
	setOfString.add('value2');
	setOfString.add('value3');
	const expectedString = '[value1,value2,value3]';
	return {setOfString, expectedString};
}

function createMapOfSet() {
	const mapOfSet = new Map<string, Set<string>>();
	const {setOfString, expectedString} = createSet();
	mapOfSet.set('key1', setOfString);
	mapOfSet.set('key2', setOfString);
	const expectedMapString = `{key1 => ${expectedString}},{key2 => ${expectedString}}`;
	return {mapOfSet, expectedMapString};
}

function createMapOfMapOfSet() {
	const mapOfMapOfSet = new Map<string, Map<string, Set<string>>>();
	const {mapOfSet, expectedMapString} = createMapOfSet();
	mapOfMapOfSet.set('topKey1', mapOfSet);
	const expectedMapOfMapString = `{topKey1 => ${expectedMapString}}`
	return {mapOfMapOfSet, expectedMapOfMapString};
}

