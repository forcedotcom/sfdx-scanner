import {expect} from 'chai';
import {CategoryFilter, RuleFilter, RulenameFilter} from '../../../src/lib/RuleFilter';
import * as PrettyPrinter from '../../../src/lib/util/PrettyPrinter';
import {Rule} from '../../../src/types';


function createRulenameFilter(): {ruleFilter: RuleFilter; expectedRuleFilterString: string} {
	const expectedRuleFilterString = `RuleFilter[filterType=RulenameFilter, filterValues=Rule1,Rule2, negated=false]`;
	const ruleFilter = new RulenameFilter(['Rule1', 'Rule2']);
	return {ruleFilter, expectedRuleFilterString};
}

function createCategoryFilter(): {ruleFilter: RuleFilter; expectedRuleFilterString: string} {
	const expectedRuleFilterString = `RuleFilter[filterType=CategoryFilter, filterValues=Rule3,Rule4, negated=false]`;
	const ruleFilter = new CategoryFilter(['Rule3', 'Rule4'], true);
	return {ruleFilter, expectedRuleFilterString};
}


function createRuleFilters(): {ruleFilters: RuleFilter[]; expectedRuleFiltersString: string} {
	const ruleFilter1 = createRulenameFilter();
	const ruleFilter2 = createCategoryFilter();
	const ruleFilters: RuleFilter[] = [ruleFilter1.ruleFilter, ruleFilter2.ruleFilter];
	const expectedRuleFiltersString = `[${ruleFilter1.expectedRuleFilterString},${ruleFilter2.expectedRuleFilterString}]`;
	return {ruleFilters, expectedRuleFiltersString};
}

function createRule(): {rule: Rule; expectedRuleString: string} {
	const rule: Rule = {
		name: "RuleName1",
		description: "rule description",
		categories: ["Category1", "Category2"],
		rulesets: ["Ruleset1", "Ruleset2"],
		languages: ["apex", "javascript"],
		engine: "pmd",
		isPilot: false,
		isDfa: false,
		sourcepackage: "/some/path/to/a/package.jar",
		defaultEnabled: true
	};
	const expectedRuleString = "Rule[name: RuleName1, description: rule description, categories: Category1,Category2, rulesets: Ruleset1,Ruleset2, languages: apex,javascript, engine: pmd, sourcepackage: /some/path/to/a/package.jar]";
	return {rule, expectedRuleString};
}

function createRules(): {rules: Rule[]; expectedRulesString: string} {
	const rule1 = createRule();
	const rule2 = createRule();
	const rules = [rule1.rule, rule2.rule];
	const expectedRulesString = `[${rule1.expectedRuleString},${rule2.expectedRuleString}]`;
	return {rules, expectedRulesString};
}

function createSet(): {setOfString: Set<string>; expectedString: string} {
	const setOfString = new Set<string>();
	setOfString.add('value1');
	setOfString.add('value2');
	setOfString.add('value3');
	const expectedString = '[value1,value2,value3]';
	return {setOfString, expectedString};
}

function createMapOfSet(): {mapOfSet: Map<string, Set<string>>; expectedMapString: string} {
	const mapOfSet = new Map<string, Set<string>>();
	const {setOfString, expectedString} = createSet();
	mapOfSet.set('key1', setOfString);
	mapOfSet.set('key2', setOfString);
	const expectedMapString = `{key1 => ${expectedString}},{key2 => ${expectedString}}`;
	return {mapOfSet, expectedMapString};
}

function createMapOfMapOfSet(): {mapOfMapOfSet: Map<string,Map<string,Set<string>>>; expectedMapOfMapString: string} {
	const mapOfMapOfSet = new Map<string, Map<string, Set<string>>>();
	const {mapOfSet, expectedMapString} = createMapOfSet();
	mapOfMapOfSet.set('topKey1', mapOfSet);
	const expectedMapOfMapString = `{topKey1 => ${expectedMapString}}`
	return {mapOfMapOfSet, expectedMapOfMapString};
}

describe(('PrettyPrinter tests'), () => {
	it('should print Set<string>', () => {
		const {setOfString, expectedString} = createSet();
		expect(PrettyPrinter.stringifySet(setOfString)).equals(expectedString);
	});

	it('should print Map<string, Set<string>>', () => {
		const {mapOfSet, expectedMapString} = createMapOfSet();
		expect(PrettyPrinter.stringifyMapOfSets(mapOfSet)).equals(expectedMapString);
	});

	it('should print Map<string, Map<string, Set<string>>>', () => {
		const {mapOfMapOfSet, expectedMapOfMapString} = createMapOfMapOfSet();
		expect(PrettyPrinter.stringifyMapOfMaps(mapOfMapOfSet)).equals(expectedMapOfMapString);
	});

	it('should print a RuleFilter', () => {
		const {ruleFilter, expectedRuleFilterString} = createCategoryFilter();
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
