import {expect} from '@salesforce/command/lib/test';
import {setupCommandTest} from '../../../TestUtils';
import {Rule} from '../../../../src/types';
import {CATALOG_FILE, ENGINE} from '../../../../src/Constants';
import fs = require('fs');
import path = require('path');
import { Controller } from '../../../../src/Controller';
import { Messages } from '@salesforce/core';

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'list');

function getCatalogJson(): { rules: Rule[] } {
	const sfdxScannerPath = Controller.getSfdxScannerPath();
	const catalogPath = path.join(sfdxScannerPath, CATALOG_FILE);
	expect(fs.existsSync(catalogPath), catalogPath).to.equal(true, 'Catalog file should exist');
	return JSON.parse(fs.readFileSync(catalogPath).toString());
}

function listContentsOverlap<T>(list1: T[], list2: T[]): boolean {
	return list1.some(x => list2.includes(x));
}

/**
 * Rather than painstakingly check all of the rules, we'll just make sure that we got the right number of rules,
 * compared to the number of rules in the catalog. This method filters the catalogs rules.
 *
 * @param includeDefaultDisabled - if true, include rule when Rule.defaultEnabled is false. if false, include rule when Rule.defaultEnabled is true
 * @param includedCategories - include rule if any of its categories overlaps with this array
 * @param excludeCategories- exclude rule if any of its categories overlaps with this array
 *
 * @return the number of rules returned
 */
async function getRulesFilteredByCategoryCount(includeDefaultDisabled: boolean, includedCategories: string[]=undefined, excludeCategories: string[]=undefined): Promise<number> {
	const catalog = getCatalogJson();
	const enabledEngineNames = (await Controller.getEnabledEngines()).map(e => e.getName());
	let rules: Rule[] = catalog.rules.filter(r => (includeDefaultDisabled || r.defaultEnabled) && enabledEngineNames.includes(r.engine));
	if (includedCategories?.length > 0) {
		rules = rules.filter((r: Rule) => includedCategories.some(c => r.categories.includes(c)));
	}
	if (excludeCategories?.length > 0) {
		rules = rules.filter((r: Rule) => !excludeCategories.some(c => r.categories.includes(c)));
	}
	expect(rules.length).to.be.above(0, 'Expected rule count cannot be zero. Test invalid');
	return rules.length;
}

describe('scanner:rule:list', () => {

	describe('E2E', () => {
		describe('Test Case: No filters applied', () => {
			setupCommandTest
				.command(['scanner:rule:list'])
				.it('All rules for enabled engines are returned', async ctx => {
					const totalRuleCount = await getRulesFilteredByCategoryCount(false);

					// Split the output table by newline and throw out the first two rows, since they just contain header information. That
					// should leave us with the actual data.
					const rows = ctx.stdout.trim().split('\n');
					rows.shift();
					rows.shift();
					expect(rows).to.have.lengthOf(totalRuleCount, 'All rules should have been returned');
				});

			setupCommandTest
				.command(['scanner:rule:list', '--json'])
				.it('--json flag yields expected JSON', async ctx => {
					const totalRuleCount = await getRulesFilteredByCategoryCount(false);

					// Parse the output back into a JSON, and make sure it has the right number of rules.
					const outputJson = JSON.parse(ctx.stdout);
					expect(outputJson.result).to.have.lengthOf(totalRuleCount, 'All rules should have been returned');
				});
		});

		describe('Test Case: Filtering by category only', () => {
			const positiveCategories = ['Best Practices', 'Design'];
			// Add a preceding '!' to each category
			const negatedCategories = positiveCategories.map(c => `!${c}`);

			setupCommandTest
				.command(['scanner:rule:list', '--category', positiveCategories[0], '--json'])
				.it('Filtering by one category returns only the rules in that category for enabled engines', async ctx => {
					const targetRuleCount = await getRulesFilteredByCategoryCount(true, [positiveCategories[0]]);

					// Parse the output back into a JSON, and make sure it has the right number of rules.
					const outputJson = JSON.parse(ctx.stdout);
					expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules in the specified category should have been returned');

					// Make sure that each rule overlaps with the expected categories
					outputJson.result.forEach((rule: Rule) => {
						expect(rule.categories).to.contain(positiveCategories[0], `Rule ${rule.name} was included despite being in the wrong category`);
					});
				});

			setupCommandTest
				.command(['scanner:rule:list', '--category', positiveCategories.join(','), '--json'])
				.it('Filtering by multiple categories returns any rule in either category', async ctx => {
					const targetRuleCount = await getRulesFilteredByCategoryCount(true, positiveCategories);

					// Parse the output back into a JSON, and make sure it has the right number of rules.
					const outputJson = JSON.parse(ctx.stdout);
					expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules in either category should be returned');

					// Make sure that each rule overlaps with the expected categories
					outputJson.result.forEach((rule: Rule) => {
						expect(rule).to.satisfy((rule) => {
								return listContentsOverlap(rule.categories, positiveCategories)
							},
							`Rule ${rule.name} was included despite being in the wrong category`
						);
					});
			});

			setupCommandTest
				.command(['scanner:rule:list', '--category', negatedCategories[0], '--json'])
				.it('Excluding by one category excludes all rules from that category', async ctx => {
					const targetRuleCount = await getRulesFilteredByCategoryCount(false, [], [positiveCategories[0]]);

					// Parse the output back into a JSON, and make sure it has the right number of rules.
					const outputJson = JSON.parse(ctx.stdout);
					expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules in the specified category should have been returned');

					// Ensure that all of the returned rules have excluded the single category
					outputJson.result.forEach((rule: Rule) => {
						expect(rule.categories).to.not.contain(positiveCategories[0], `Rule ${rule.name} with categories ${rule.categories} was included despite being in the wrong category`);
					});
				});

			setupCommandTest
				.command(['scanner:rule:list', '--category', negatedCategories.join(','), '--json'])
				.it('Excluding by multiple categories excludes all rules from those categories', async ctx => {
					const targetRuleCount = await getRulesFilteredByCategoryCount(false, [], positiveCategories);

					// Parse the output back into a JSON, and make sure it has the right number of rules.
					const outputJson = JSON.parse(ctx.stdout);
					expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules in the specified category should have been returned');

					// Ensure that all of the returned rules have excluded both categories
					outputJson.result.forEach((rule: Rule) => {
						expect(rule).to.satisfy((rule) => { return !positiveCategories.some(c => rule.categories.includes(c)) },
							`Rule ${rule.name} with categories ${rule.categories} was included despite being in the wrong category`
						);
					});
				});
		});

		describe('Test Case: Filtering by ruleset only', () => {

			setupCommandTest
				.command(['scanner:rule:list', '--ruleset', 'Braces'])
				.it('--ruleset option shows deprecation warning', ctx => {
					expect(ctx.stderr).contains(messages.getMessage('rulesetDeprecation'));
				});

			setupCommandTest
				.command(['scanner:rule:list', '--ruleset', 'Braces', '--json'])
				.it('Filtering by a single ruleset returns only the rules in that ruleset', ctx => {
					// Count how many rules in the catalog fit the criteria.
					const targetRuleCount = getCatalogJson().rules.filter(rule => rule.rulesets.includes('Braces')).length;

					// Parse the output back into a JSON, and make sure it has the right number of rules.
					const outputJson = JSON.parse(ctx.stdout);
					expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules in the desired ruleset should be returned');

					// Make sure that only rules in the right ruleset were returned.
					outputJson.result.forEach((rule: Rule) => {
						expect(rule.rulesets).to.contain('Braces', 'Only rules in the desired ruleset should have been returned');
					});
				});

			setupCommandTest
				.command(['scanner:rule:list', '--ruleset', 'ApexUnit,Braces', '--json'])
				.it('Filtering by multiple rulesets returns any rule in either ruleset', ctx => {
					// Count how many rules in the catalog fit the criteria.
					const targetRuleCount = getCatalogJson().rules.filter(rule => rule.rulesets.includes('Braces') || rule.rulesets.includes('ApexUnit')).length;

					// Parse the output back into a JSON, and make sure it has the right number of rules.
					const outputJson = JSON.parse(ctx.stdout);
					expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules in both sets should have been returned');
					// Make sure that only rules in the desired sets were returned.
					outputJson.result.forEach((rule: Rule) => {
						expect(rule).to.satisfy((rule) => {
								return rule.rulesets.includes('Braces') || rule.rulesets.includes('ApexUnit')
							},
							`Rule ${rule.name} was included despite being in the wrong ruleset`
						);
					});
				});
		});

		describe('Test Case: Filtering by language only', () => {
			const filteredLanguages = ['apex', 'javascript'];
			setupCommandTest
				.command(['scanner:rule:list', '--language', filteredLanguages[0], '--json'])
				.it('Filtering by a single language returns only rules applied to that language', ctx => {
					// Count how many rules in the catalog fit the criteria.
					const targetRuleCount = getCatalogJson().rules.filter(rule => rule.languages.includes(filteredLanguages[0])).length;
					expect(targetRuleCount).to.be.above(0, 'Expected rule count cannot be zero. Test invalid');

					// Parse the output back into a JSON and make sure it has the right number of rules.
					const outputJson = JSON.parse(ctx.stdout);
					expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules of the desired language should be returned');
					// Make sure that only the right rules were returned.
					outputJson.result.forEach((rule) => {
						expect(rule.languages).to.contain(filteredLanguages[0], `Rule ${rule.name} was included despite targeting the wrong language`)
					});
				});

			setupCommandTest
				.command(['scanner:rule:list', '--language', filteredLanguages.join(','), '--json'])
				.it('Filtering by multiple languages returns any rule for either language', async ctx => {
					// Count how many rules in the catalog fit the criteria.
					const enabledEngines = (await Controller.getEnabledEngines()).map(e => e.getName());
					const filterFunction: (Rule) => boolean =
						(r: Rule) => listContentsOverlap(r.languages, filteredLanguages) && enabledEngines.includes(r.engine);
					const targetRuleCount = getCatalogJson().rules.filter(filterFunction).length;
					expect(targetRuleCount).to.be.above(0, 'Expected rule count cannot be zero. Test invalid');

					// Parse the output back into a JSON and make sure it has the right number of rules.
					const outputJson = JSON.parse(ctx.stdout);
					expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules of the desired languages should be returned');
					// Make sure that only the right rules were returned.
					outputJson.result.forEach((rule: Rule) => {
						expect(rule).to.satisfy((rule) => {
								return listContentsOverlap(rule.languages, filteredLanguages)
							},
							`Rule ${rule.name} was included despite targeting neither desired language`
						);
					});
				});
		});

		describe('Test Case: Filtering by engine only', () => {
			setupCommandTest
				.command(['scanner:rule:list', '--engine', ENGINE.PMD, '--json'])
				.it('Filtering by a single engine returns only rules applied to that engine', ctx => {
					// Count how many rules in the catalog fit the criteria.
					const targetRuleCount = getCatalogJson().rules.filter(rule => rule.engine.includes(ENGINE.PMD)).length;

					// Parse the output back into a JSON and make sure it has the right number of rules.
					const outputJson = JSON.parse(ctx.stdout);
					expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules of the desired engine should be returned');
					// Make sure that only the right rules were returned.
					outputJson.result.forEach((rule) => {
						expect(rule.engine).to.equal(ENGINE.PMD, `Rule ${rule.name} was included despite targeting the wrong engine`)
					});
				});

			setupCommandTest
				.command(['scanner:rule:list', '--engine', ENGINE.ESLINT_LWC, '--json'])
				.it('Filtering by a disabled engine returns rules', ctx => {
					// Count how many rules in the catalog fit the criteria.
					const targetRuleCount = getCatalogJson().rules.filter(rule => rule.engine.includes(ENGINE.ESLINT_LWC)).length;

					// Parse the output back into a JSON and make sure it has the right number of rules.
					const outputJson = JSON.parse(ctx.stdout);
					expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules of the desired engine should be returned');
					// Make sure that only the right rules were returned.
					outputJson.result.forEach((rule) => {
						expect(rule.engine).to.equal(ENGINE.ESLINT_LWC, `Rule ${rule.name} was included despite targeting the wrong engine`)
					});
				});

			const engines: string[] = [ENGINE.PMD, ENGINE.ESLINT_LWC];
			setupCommandTest
				.command(['scanner:rule:list', '--engine', engines.join(','), '--json'])
				.it('Filtering by multiple engines returns any rule for either engine', ctx => {
					// Count how many rules in the catalog fit the criteria.
					const targetRuleCount = getCatalogJson().rules.filter(rule => (engines.indexOf(rule.engine) >= 0)).length;

					// Parse the output back into a JSON and make sure it has the right number of rules.
					const outputJson = JSON.parse(ctx.stdout);
					expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules of the desired engines should be returned');
					// Make sure that only the right rules were returned.
					outputJson.result.forEach((rule: Rule) => {
						expect(rule).to.satisfy((rule) => {
								return (engines.indexOf(rule.engine) >= 0)
							},
							`Rule ${rule.name} was included despite targeting neither desired engine`
						);
					});
				});
		});

		describe('Test Case: Applying multiple filter types', () => {
			setupCommandTest
				.command(['scanner:rule:list', '--category', 'Best Practices', '--language', 'apex', '--json'])
				.it('Filtering on multiple columns only returns rows that satisfy BOTH filters', ctx => {
					// Count how many rules in the catalog fit all criteria.
					const targetRuleCount = getCatalogJson().rules.filter(rule => rule.categories.includes('Best Practices') && rule.languages.includes('apex')).length;

					// Parse the output back into a JSON and make sure it has the right number of rules.
					const outputJson = JSON.parse(ctx.stdout);
					expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules matching criteria should have been returned');

					// Make sure that only the right rules were returned.
					outputJson.result.forEach((rule: Rule) => {
						expect(rule.languages).to.contain('apex', `Rule ${rule.name} was included despite targeting the wrong language`);
						expect(rule.categories).to.contain('Best Practices', `Rule ${rule.name} was included despite being in the wrong category`);
					});
				});
		});

		describe('Edge Case: No rules match criteria', () => {
			setupCommandTest
				.command(['scanner:rule:list', '--category', 'Beebleborp'])
				.it('Without --json flag, an empty table is printed', ctx => {
					// Split the result by newline, and make sure there are two rows.
					const rows = ctx.stdout.trim().split('\n');
					expect(rows).to.have.lengthOf(2, 'Only the header rows should have been printed');
				});

			setupCommandTest
				.command(['scanner:rule:list', '--category', 'Beebleborp', '--json'])
				.it('With the --json flag, the results are empty', ctx => {
					// Parse the results back into a JSON and make sure it has an empty list.
					const outputJson = JSON.parse(ctx.stdout);
					expect(outputJson.result).to.have.lengthOf(0, 'No results should be included');
				});
		});
	});
});
