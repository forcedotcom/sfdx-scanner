/* eslint-disable @typescript-eslint/no-explicit-any */
import { fail } from 'assert';
import { expect } from 'chai';
import { isRuleGroupFilter, CategoryFilter, LanguageFilter, RuleFilter,
	RulenameFilter, RulesetFilter, SourcePackageFilter, EngineFilter, isEngineFilter } from '../../src/lib/RuleFilter';
import { ENGINE, LANGUAGE } from '../../src/Constants';
import {BundleName, getMessage} from "../../src/MessageCatalog";

const POSITIVE_FILTERS = ['val1 ', ' val2'];
const NEGATIVE_FILTERS = ['!val1 ', ' !val2'];
const MIXED_FILTERS = ['!val1 ', ' val2'];

const TRIMMED_FILTERS = ['val1', 'val2'];

const assertNegativeFilterThrows = (method: () => void, filterName: string): void => {
	try {
		method();
		fail(`${filterName} should have thrown`);
	} catch(err) {
		expect(err.message).to.equal(getMessage(BundleName.Exceptions, "RuleFilter.PositiveOnly", [filterName]));
	}
};

const assertMixedFilterThrows = (method: () => void, filterName: string): void => {
	try {
		method();
		fail(`${filterName} should have thrown`);
	} catch(err) {
		expect(err.message).to.equal(getMessage(BundleName.Exceptions, "RuleFilter.MixedTypes", [filterName]));
	}
};

describe('RuleFilter', () => {
	describe('Positive Cases', () => {
		it('Values are trimmed', () => {
			const filter: RuleFilter = new CategoryFilter(POSITIVE_FILTERS, true);
			expect((filter as any).filterValues, 'Filter Values').to.eql(TRIMMED_FILTERS);
			expect((filter as any).negated, 'Negated').to.be.false;
		});

		it('Negated values are converted', () => {
			const filter: RuleFilter = new CategoryFilter(NEGATIVE_FILTERS, true);
			expect((filter as any).filterValues, 'Filter Values').to.eql(TRIMMED_FILTERS);
			expect((filter as any).negated, 'Negated').to.be.true;
		});

		it('isRuleGroupFilter', () => {
			expect(isRuleGroupFilter(new CategoryFilter([], true)), 'CategoryFilter').to.be.true;
			expect(isRuleGroupFilter(new RulesetFilter([])), 'RulesetFilter').to.be.true;
			expect(isRuleGroupFilter(new RulenameFilter([])), 'RulenameFilter').to.be.false;
		});

		it('isEngineFilter', () => {
			expect(isEngineFilter(new EngineFilter([])), 'EngineFilter').to.be.true;
			expect(isEngineFilter(new RulesetFilter([])), 'RulesetFilter').to.be.false;
		});

		describe('getRuleValues', () => {
			const baseRule = {
				engine: null,
				sourcepackage: null,
				name: null,
				description: null,
				categories: null,
				rulesets: null,
				languages: null,
				defaultEnabled: true
			};

			it('CategoryFilter',  () => {
				const rule = {
					...baseRule,
					categories: TRIMMED_FILTERS
				}
				const filter: RuleFilter = new CategoryFilter([], true);
				expect((filter as any).getRuleValues(rule), 'Category').to.eql(TRIMMED_FILTERS);
			});

			it('CategoryFilter',  () => {
				const rule = {
					...baseRule,
					engine: ENGINE.PMD
				}
				const filter: RuleFilter = new EngineFilter([]);
				expect((filter as any).getRuleValues(rule), 'Engine').to.eql([ENGINE.PMD]);
			});

			it('LanguageFilter',  () => {
				const rule = {
					...baseRule,
					languages: [LANGUAGE.APEX]
				}
				const filter: RuleFilter = new LanguageFilter([]);
				expect((filter as any).getRuleValues(rule), 'Language').to.eql([LANGUAGE.APEX]);
			});

			it('RulenameFilter',  () => {
				const ruleName = 'MyRule';
				const rule = {
					...baseRule,
					name: ruleName
				}
				const filter: RuleFilter = new RulenameFilter([]);
				expect((filter as any).getRuleValues(rule), 'Rule Name').to.eql([ruleName]);
			});

			it('SourcePackageFilter',  () => {
				const packageName = 'com.a.package';
				const rule = {
					...baseRule,
					sourcepackage: packageName
				}
				const filter: RuleFilter = new SourcePackageFilter([]);
				expect((filter as any).getRuleValues(rule), 'Source Package').to.eql([packageName]);
			});
		});

		describe('matchesRuleGroup', () => {
			describe('CategoryFilter', () => {
				const baseRuleGroup = {
					engine: null,
					paths: null
				}

				const ruleGroup1 = {
					...baseRuleGroup,
					name: TRIMMED_FILTERS[0]
				};

				const ruleGroup2 = {
					...baseRuleGroup,
					name: TRIMMED_FILTERS[1]
				};

				const ruleGroup3 = {
					...baseRuleGroup,
					name: 'no-match-1'
				};

				const ruleGroup4 = {
					...baseRuleGroup,
					name: 'no-match-2'
				};

				it ('Positive Filters', () => {
					const filter: RuleFilter = new CategoryFilter(POSITIVE_FILTERS, true);
					expect (filter.matchesRuleGroup(ruleGroup1), ruleGroup1.name).to.be.true;
					expect (filter.matchesRuleGroup(ruleGroup2), ruleGroup2.name).to.be.true;
					expect (filter.matchesRuleGroup(ruleGroup3), ruleGroup3.name).to.be.false;
					expect (filter.matchesRuleGroup(ruleGroup4), ruleGroup3.name).to.be.false;
				});

				it ('Negated Filters', () => {
					const filter: RuleFilter = new CategoryFilter(NEGATIVE_FILTERS, true);
					expect (filter.matchesRuleGroup(ruleGroup1), ruleGroup1.name).to.be.false;
					expect (filter.matchesRuleGroup(ruleGroup2), ruleGroup2.name).to.be.false;
					expect (filter.matchesRuleGroup(ruleGroup3), ruleGroup3.name).to.be.true;
					expect (filter.matchesRuleGroup(ruleGroup4), ruleGroup3.name).to.be.true;
				});
			});
		});

		describe('matchesRule', () => {
			describe('CategoryFilter', () => {
				const baseRule = {
					engine: null,
					sourcepackage: null,
					name: null,
					description: null,
					rulesets: null,
					isDfa: false,
					isPilot: false,
					languages: null,
					defaultEnabled: true
				};

				const rule1 = {
					...baseRule,
					categories: [TRIMMED_FILTERS[0]]
				};

				const rule2 = {
					...baseRule,
					defaultEnabled: false,
					categories: [TRIMMED_FILTERS[1]]
				};

				const rule3 = {
					...baseRule,
					categories: ['no-match-1']
				};

				const rule4 = {
					...baseRule,
					defaultEnabled: false,
					categories: ['no-match-2']
				};

				it ('Positive Filters', () => {
					const filter: RuleFilter = new CategoryFilter(POSITIVE_FILTERS, true);
					expect (filter.matchesRule(rule1), rule1.name).to.be.true;
					expect (filter.matchesRule(rule2), rule2.name).to.be.true;
					expect (filter.matchesRule(rule3), rule3.name).to.be.false;
					expect (filter.matchesRule(rule4), rule4.name).to.be.false;
				});

				it ('Negated Filters', () => {
					const filter: RuleFilter = new CategoryFilter(NEGATIVE_FILTERS, true);
					expect (filter.matchesRule(rule1), rule1.name).to.be.false;
					expect (filter.matchesRule(rule2), rule2.name).to.be.false;
					expect (filter.matchesRule(rule3), rule3.name).to.be.true;
					// This should not match because defaultEnabled='false'
					expect (filter.matchesRule(rule4), rule4.name).to.be.false;
				});
			});
		});
	});
	describe('Negative Cases', () => {
		it('Negated values for Positive Filter throws Exception', () => {
			assertNegativeFilterThrows(() => new EngineFilter(NEGATIVE_FILTERS), 'Engine');
			assertNegativeFilterThrows(() => new LanguageFilter(NEGATIVE_FILTERS), 'Language');
			assertNegativeFilterThrows(() => new RulenameFilter(NEGATIVE_FILTERS), 'Rule Name');
			assertNegativeFilterThrows(() => new RulesetFilter(NEGATIVE_FILTERS), 'Ruleset');
			assertNegativeFilterThrows(() => new SourcePackageFilter(NEGATIVE_FILTERS), 'Source Package');
		});

		it('Mixed types throws Exception', () => {
			assertMixedFilterThrows(() => new CategoryFilter(MIXED_FILTERS, true), 'Category');
		});
	});
});
