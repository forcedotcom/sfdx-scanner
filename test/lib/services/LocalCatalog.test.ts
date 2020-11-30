import Sinon = require('sinon');
import * as TestOverrides from '../../test-related-lib/TestOverrides';
import * as TestUtils from '../../TestUtils';
import { RuleCatalog } from '../../../src/lib/services/RuleCatalog';
import { CategoryFilter, EngineFilter, LanguageFilter, RuleFilter, RulesetFilter } from '../../../src/lib/RuleFilter';
import { ENGINE, LANGUAGE } from '../../../src/Constants';
import { Rule, RuleGroup } from '../../../src/types';
import LocalCatalog from '../../../src/lib/services/LocalCatalog';
import { expect } from 'chai';

TestOverrides.initializeTestSetup();

describe('LocalCatalog', () => {
	let catalog: RuleCatalog;

	beforeEach(async () => {
		TestUtils.stubCatalogFixture()
		catalog = new LocalCatalog();
		await catalog.init();
	});

	afterEach(() => {
		Sinon.restore();
	});

	describe('getRuleGroupsMatchingFilters', () => {
        const LANGUAGE_ECMASCRIPT = 'ecmascript';
        /**
         * Return a map of key=<engine.name>:<ruleGroup.name>, value=RuleGroup
         */
		const mapRuleGroups = (ruleGroups: RuleGroup[]): Map<string, RuleGroup> => {
			const map = new Map<string, RuleGroup>();

			ruleGroups.forEach(ruleGroup => {
				const key = `${ruleGroup.engine}:${ruleGroup.name}`;
				expect(map.has(key), key).to.be.false;
				map.set(key, ruleGroup);
			});

			return map;
        }

		const validatePmdRuleGroup = (mappedRuleGroups: Map<string, RuleGroup>, name: string, languages: string[], type: string): void => {
			const ruleGroup = mappedRuleGroups.get(`${ENGINE.PMD}:${name}`);
			expect(ruleGroup).to.not.be.undefined;
            expect(ruleGroup.name).to.equal(name);
            expect(ruleGroup.engine).to.equal(ENGINE.PMD);
            expect(ruleGroup.paths, TestUtils.prettyPrint(ruleGroup.paths)).to.be.lengthOf(languages.length);
            const paths = [];
            for (const language of languages) {
				const fileName = name.toLowerCase().replace(' ', '');
                paths.push(`${type}/${language}/${fileName}.xml`);
			}
			// Not concerned about order
            expect(ruleGroup.paths, TestUtils.prettyPrint(ruleGroup.paths)).to.have.members(paths);
		}

		const validatePmdCategory = (mappedRuleGroups: Map<string, RuleGroup>, name: string, languages: string[]): void => {
			validatePmdRuleGroup(mappedRuleGroups, name, languages, 'category');
        }

        const validatePmdRuleset = (mappedRuleGroups: Map<string, RuleGroup>, name: string, languages: string[]): void => {
			validatePmdRuleGroup(mappedRuleGroups, name, languages, 'rulesets');
        }

		describe('RulesetFilter', () => {
			it ('Single Value', async () => {
				const filter: RuleFilter = new RulesetFilter(['Braces']);
				const ruleGroups: RuleGroup[] = catalog.getRuleGroupsMatchingFilters([filter]);
				expect(ruleGroups, TestUtils.prettyPrint(ruleGroups)).to.be.lengthOf(1);
				const mappedRuleGroups = mapRuleGroups(ruleGroups);
                validatePmdRuleset(mappedRuleGroups, 'Braces', [LANGUAGE_ECMASCRIPT, LANGUAGE.APEX]);
			});

			it ('Multiple Values', async () => {
				const filter: RuleFilter = new RulesetFilter(['Security', 'Braces']);
				const ruleGroups: RuleGroup[] = catalog.getRuleGroupsMatchingFilters([filter]);
				expect(ruleGroups, TestUtils.prettyPrint(ruleGroups)).to.be.lengthOf(2);
				const mappedRuleGroups = mapRuleGroups(ruleGroups);
                validatePmdRuleset(mappedRuleGroups, 'Braces', [LANGUAGE_ECMASCRIPT, LANGUAGE.APEX]);
                validatePmdRuleset(mappedRuleGroups, 'Security', [LANGUAGE.APEX, LANGUAGE.VISUALFORCE]);
			});

            // Multiple Filters: Not tested
            // The #getRuleGroupsMatchingFilters method does not prevent the caller from passing multiple
            // instances of RulesetFilters. However in practice this does not occur.
		});

		describe('CategoryFilter', () => {
			const validateEslintBestPractices = (mappedRuleGroups: Map<string, RuleGroup>): void => {
				for (const engine of [ENGINE.ESLINT, ENGINE.ESLINT_TYPESCRIPT]) {
					const ruleGroup = mappedRuleGroups.get(`${engine}:Best Practices`);
					expect(ruleGroup.name).to.equal('Best Practices');
					expect(ruleGroup.engine).to.equal(engine);
					expect(ruleGroup.paths, TestUtils.prettyPrint(ruleGroup.paths)).to.be.lengthOf(2);
					expect(ruleGroup.paths).to.eql(['https://eslint.org/docs/rules/no-implicit-globals', 'https://eslint.org/docs/rules/no-implicit-coercion']);
				}
			}

			const validateEslintPossibleErrors = (mappedRuleGroups: Map<string, RuleGroup>): void => {
				for (const engine of [ENGINE.ESLINT, ENGINE.ESLINT_TYPESCRIPT]) {
					const ruleGroup = mappedRuleGroups.get(`${engine}:Possible Errors`);
					expect(ruleGroup).to.not.be.undefined;
					expect(ruleGroup.name).to.equal('Possible Errors');
					expect(ruleGroup.engine).to.equal(engine);
					expect(ruleGroup.paths, TestUtils.prettyPrint(ruleGroup.paths)).to.be.lengthOf(1);
					expect(ruleGroup.paths).to.eql(['https://eslint.org/docs/rules/no-inner-declarations']);
				}
			}

			describe ('Positive', () => {
				it ('Single Value', async () => {
					const filter: RuleFilter = new CategoryFilter(['Best Practices']);
					const ruleGroups: RuleGroup[] = catalog.getRuleGroupsMatchingFilters([filter]);
					expect(ruleGroups, TestUtils.prettyPrint(ruleGroups)).to.be.lengthOf(3);
					const mappedRuleGroups = mapRuleGroups(ruleGroups);

					validateEslintBestPractices(mappedRuleGroups);
					validatePmdCategory(mappedRuleGroups, 'Best Practices', [LANGUAGE_ECMASCRIPT, LANGUAGE.APEX]);
				});

				it ('Multiple Values', async () => {
					const filter: RuleFilter = new CategoryFilter(['Best Practices', 'Possible Errors']);
					const ruleGroups: RuleGroup[] = catalog.getRuleGroupsMatchingFilters([filter]);
					expect(ruleGroups, 'Rule Groups').to.be.lengthOf(5);
					const mappedRuleGroups = mapRuleGroups(ruleGroups);
					validateEslintPossibleErrors(mappedRuleGroups);
					validateEslintBestPractices(mappedRuleGroups);
					validatePmdCategory(mappedRuleGroups, 'Best Practices', [LANGUAGE.APEX, LANGUAGE_ECMASCRIPT]);
				});

                // Multiple Filters: Not tested
                // The #getRuleGroupsMatchingFilters method does not prevent the caller from passing multiple
                // instances of CategoryFilter. However in practice this does not occur.
            });

            // RulesetFilter and CategoryFilter Combination: Not tested
            // There is a known issue with filtering by Category and Ruleset. The two are mutually exclusive,
            // invoking #getRuleGroupsMatchingFilters with this combination results in no rules being returned.
            // It is on the roadmap to remove Ruleset support and only support categories.

			describe ('Negated', () => {
				it ('Single Value', async () => {
					const filter: RuleFilter = new CategoryFilter(['!Best Practices']);
					const ruleGroups: RuleGroup[] = catalog.getRuleGroupsMatchingFilters([filter]);
					expect(ruleGroups, TestUtils.prettyPrint(ruleGroups)).to.be.lengthOf(4);
					const mappedRuleGroups = mapRuleGroups(ruleGroups);

					validateEslintPossibleErrors(mappedRuleGroups);
					validatePmdCategory(mappedRuleGroups, 'Design', [LANGUAGE.APEX, LANGUAGE_ECMASCRIPT]);
					validatePmdCategory(mappedRuleGroups, 'Error Prone', [LANGUAGE.APEX, LANGUAGE_ECMASCRIPT]);
				});

				it ('Multiple Values', async () => {
					const filter: RuleFilter = new CategoryFilter(['!Best Practices', '!Design']);
					const ruleGroups: RuleGroup[] = catalog.getRuleGroupsMatchingFilters([filter]);
					expect(ruleGroups, TestUtils.prettyPrint(ruleGroups)).to.be.lengthOf(3);
					const mappedRuleGroups = mapRuleGroups(ruleGroups);

					validateEslintPossibleErrors(mappedRuleGroups);
					validatePmdCategory(mappedRuleGroups, 'Error Prone', [LANGUAGE.APEX, LANGUAGE_ECMASCRIPT]);
				});

                // Multiple Filters: Not tested
                // The #getRuleGroupsMatchingFilters method does not prevent the caller from passing multiple
                // instances of CategoryFilter. However in practice this does not occur.
			});
		});
	});

	describe ('getRulesMatchingFilters', () => {
		const mapRules = (rules: Rule[]): Map<string, Rule> => {
			const map = new Map<string, Rule>();

			rules.forEach(rule => {
				const key = `${rule.engine}:${rule.name}`;
				expect(map.has(key), key).to.be.false;
				map.set(key, rule);
			});

			return map;
		}

		const validateRule = (mappedRules: Map<string, Rule>, names: string[], categories: string[], languages: string[], engines: ENGINE[]): void => {
			for (const engine of engines) {
				for (const name of names) {
					const rule = mappedRules.get(`${engine}:${name}`);
					expect(rule).to.not.be.undefined;
					expect(rule.name).to.equal(name);
					expect(rule.engine).to.equal(engine);
					expect(rule.categories).to.have.members(categories);
					expect(rule.languages).to.have.members(languages);
				}
			}
		}

		const validatePmdRule = (mappedRules: Map<string, Rule>, names: string[], categories: string[], languages: string[]): void => {
			validateRule(mappedRules, names, categories, languages, [ENGINE.PMD]);
		}

		const validateEslintRule = (mappedRules: Map<string, Rule>, names: string[], categories: string[], engines=[ENGINE.ESLINT, ENGINE.ESLINT_TYPESCRIPT]): void => {
			for (const engine of engines) {
				const languages = engine === ENGINE.ESLINT ? [LANGUAGE.JAVASCRIPT] : [LANGUAGE.TYPESCRIPT];
				validateRule(mappedRules, names, categories, languages, [engine]);
			}
		}

		describe('CategoryFilter', () => {
			describe ('Positive', () => {
				it ('Single Value', async () => {
					const filter: RuleFilter = new CategoryFilter(['Possible Errors']);
					const rules: Rule[] = catalog.getRulesMatchingFilters([filter]);
					expect(rules, TestUtils.prettyPrint(rules)).to.be.lengthOf(4);
					const mappedRules = mapRules(rules);

					validateEslintRule(mappedRules, ['no-unreachable', 'no-inner-declarations'], ['Possible Errors']);
				});

				it ('Multiple Values', async () => {
					const filter: RuleFilter = new CategoryFilter(['Possible Errors', 'Design']);
					const rules: Rule[] = catalog.getRulesMatchingFilters([filter]);
					expect(rules, TestUtils.prettyPrint(rules)).to.be.lengthOf(6);
					const mappedRules = mapRules(rules);

					validateEslintRule(mappedRules, ['no-unreachable', 'no-inner-declarations'], ['Possible Errors']);
					validatePmdRule(mappedRules, ['AvoidDeeplyNestedIfStmts', 'ExcessiveClassLength'], ['Design'], [LANGUAGE.APEX]);
				});

                // Multiple Filters: Not tested
                // The #getRuleGroupsMatchingFilters method does not prevent the caller from passing multiple
                // instances of CategoryFilter. However in practice this does not occur.
			});

			describe ('Negative', () => {
				it ('Single Value', async () => {
					const filter: RuleFilter = new CategoryFilter(['!Possible Errors']);
					const rules: Rule[] = catalog.getRulesMatchingFilters([filter]);
					expect(rules, TestUtils.prettyPrint(rules)).to.be.lengthOf(7);
					const mappedRules = mapRules(rules);

					validatePmdRule(mappedRules, ['AvoidDeeplyNestedIfStmts', 'ExcessiveClassLength'], ['Design'], [LANGUAGE.APEX]);
					validatePmdRule(mappedRules, ['AvoidWithStatement', 'ConsistentReturn'], ['Best Practices'], [LANGUAGE.JAVASCRIPT]);
					validatePmdRule(mappedRules, ['ForLoopsMustUseBraces', 'IfElseStmtsMustUseBraces', 'IfStmtsMustUseBraces'], ['Code Style'], [LANGUAGE.JAVASCRIPT]);
				});

				it ('Multiple Values', async () => {
					const filter: RuleFilter = new CategoryFilter(['!Possible Errors', '!Code Style']);
					const rules: Rule[] = catalog.getRulesMatchingFilters([filter]);
					expect(rules, TestUtils.prettyPrint(rules)).to.be.lengthOf(4);
					const mappedRules = mapRules(rules);

					validatePmdRule(mappedRules, ['AvoidDeeplyNestedIfStmts', 'ExcessiveClassLength'], ['Design'], [LANGUAGE.APEX]);
					validatePmdRule(mappedRules, ['AvoidWithStatement', 'ConsistentReturn'], ['Best Practices'], [LANGUAGE.JAVASCRIPT]);
				});
			});
		});

		describe('Multiple Heterogenous Filters', () => {
			describe('Positive', () => {
				/**
				 * User's can specify multiple filter parameters. A rule must match at least one parameter from each
				 * filter in order to be returned. The #getRulesMatchingFilters method would accept multiple filters of
				 * the same type, but in practice this could not happen. Each CLI flag is converted into a single filter.
				 *
				 * For example:
				 * sfdx scanner:rule:list --language 'apex,javascript' --engine 'eslint,pmd' --category 'Best Practices,Security'
				 *
				 * Results in the expression:
				 * (rule.language === 'apex' || rule.language === 'javascript') &&
				 * (rule.engine === 'eslint' || rule.engine === 'pmd') &&
				 * (rule.categories.includes('Best Practices') || rule.categories.includes('Security'))
				 */
				describe('Rules must match a parameter from each filter when multiple filters are specified', () => {
					it ('Two Filters - Single Parameter', async () => {
						const categoryFilter: RuleFilter = new CategoryFilter(['Best Practices']);
						const engineFilter: RuleFilter = new EngineFilter([ENGINE.PMD]);
						const rules: Rule[] = catalog.getRulesMatchingFilters([categoryFilter, engineFilter]);
						expect(rules, TestUtils.prettyPrint(rules)).to.be.lengthOf(2);
						const mappedRules = mapRules(rules);

						validatePmdRule(mappedRules, ['AvoidWithStatement', 'ConsistentReturn'], ['Best Practices'], [LANGUAGE.JAVASCRIPT]);
					});

					it ('Two Filters - Single/Multiple Parameters', async () => {
						const categoryFilter: RuleFilter = new CategoryFilter(['Best Practices']);
						const engineFilter: RuleFilter = new EngineFilter([ENGINE.ESLINT, ENGINE.PMD]);
						const rules: Rule[] = catalog.getRulesMatchingFilters([categoryFilter, engineFilter]);
						expect(rules, TestUtils.prettyPrint(rules)).to.be.lengthOf(4);
						const mappedRules = mapRules(rules);

						validateEslintRule(mappedRules, ['no-implicit-coercion', 'no-implicit-globals'], ['Best Practices'], [ENGINE.ESLINT]);
						validatePmdRule(mappedRules, ['AvoidWithStatement', 'ConsistentReturn'], ['Best Practices'], [LANGUAGE.JAVASCRIPT]);
					});

					it ('Two Filters - Multiple/Multiple Parameters', async () => {
						const categoryFilter: RuleFilter = new CategoryFilter(['Best Practices', 'Design']);
						const engineFilter: RuleFilter = new EngineFilter([ENGINE.ESLINT, ENGINE.PMD]);
						const rules: Rule[] = catalog.getRulesMatchingFilters([categoryFilter, engineFilter]);
						expect(rules, TestUtils.prettyPrint(rules)).to.be.lengthOf(6);
						const mappedRules = mapRules(rules);

						validateEslintRule(mappedRules, ['no-implicit-coercion', 'no-implicit-globals'], ['Best Practices'], [ENGINE.ESLINT]);
						validatePmdRule(mappedRules, ['AvoidWithStatement', 'ConsistentReturn'], ['Best Practices'], [LANGUAGE.JAVASCRIPT]);
						validatePmdRule(mappedRules, ['AvoidDeeplyNestedIfStmts', 'ExcessiveClassLength'], ['Design'], [LANGUAGE.APEX]);
					});

					it ('Three Filters - Multiple/Multiple/Single Parameters', async () => {
						const categoryFilter: RuleFilter = new CategoryFilter(['Best Practices', 'Design']);
						const engineFilter: RuleFilter = new EngineFilter([ENGINE.ESLINT, ENGINE.PMD]);
						// Missing LANGUAGE.APEX will exclude the PMD Design category
						const languageFilter: RuleFilter = new LanguageFilter([LANGUAGE.JAVASCRIPT]);
						const rules: Rule[] = catalog.getRulesMatchingFilters([categoryFilter, engineFilter, languageFilter]);
						expect(rules, TestUtils.prettyPrint(rules)).to.be.lengthOf(4);
						const mappedRules = mapRules(rules);

						validateEslintRule(mappedRules, ['no-implicit-coercion', 'no-implicit-globals'], ['Best Practices'], [ENGINE.ESLINT]);
						validatePmdRule(mappedRules, ['AvoidWithStatement', 'ConsistentReturn'], ['Best Practices'], [LANGUAGE.JAVASCRIPT]);
					});

					it ('Three Filters - Multiple/Multiple/Single Parameters', async () => {
						const categoryFilter: RuleFilter = new CategoryFilter(['Best Practices', 'Design']);
						const engineFilter: RuleFilter = new EngineFilter([ENGINE.ESLINT, ENGINE.PMD]);
						// Adding LANGUAGE.APEX will add the PMD Design category
						const languageFilter: RuleFilter = new LanguageFilter([LANGUAGE.JAVASCRIPT, LANGUAGE.APEX]);
						const rules: Rule[] = catalog.getRulesMatchingFilters([categoryFilter, engineFilter, languageFilter]);
						expect(rules, TestUtils.prettyPrint(rules)).to.be.lengthOf(6);
						const mappedRules = mapRules(rules);

						validateEslintRule(mappedRules, ['no-implicit-coercion', 'no-implicit-globals'], ['Best Practices'], [ENGINE.ESLINT]);
						validatePmdRule(mappedRules, ['AvoidWithStatement', 'ConsistentReturn'], ['Best Practices'], [LANGUAGE.JAVASCRIPT]);
						validatePmdRule(mappedRules, ['AvoidDeeplyNestedIfStmts', 'ExcessiveClassLength'], ['Design'], [LANGUAGE.APEX]);
					});
				});
			});
		});

		describe('Mixed Positive and Negative', () => {
			it ('Multiple Positive and Negative Values', async () => {
				const categoryFilter: RuleFilter = new CategoryFilter(['!Possible Errors', '!Code Style']);
				const languageFilter: RuleFilter = new LanguageFilter([LANGUAGE.APEX]);
				const rules: Rule[] = catalog.getRulesMatchingFilters([categoryFilter, languageFilter]);
				expect(rules, TestUtils.prettyPrint(rules)).to.be.lengthOf(2);
				const mappedRules = mapRules(rules);

				validatePmdRule(mappedRules, ['AvoidDeeplyNestedIfStmts', 'ExcessiveClassLength'], ['Design'], [LANGUAGE.APEX]);
			});
		});
	});
});
