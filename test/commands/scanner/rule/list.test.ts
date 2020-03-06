import { expect } from '@salesforce/command/lib/test';
import test from '../../scannerbasetest';
import {Rule} from '../../../../src/types';
import fs = require('fs');
import os = require('os');
import path = require('path');

function getCatalogJson() : {rules: Rule[]} {
  const catalogPath = path.join(os.homedir(), '.sfdx-scanner', 'PmdCatalog.json');
  expect(fs.existsSync(catalogPath)).to.equal(true, 'Catalog file should exist');
  return JSON.parse(fs.readFileSync(catalogPath).toString());
}

describe('scanner:rule:list', () => {
  describe('Test Case: No filters applied', () => {
    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list'])
      .it('All rules are returned', ctx => {
        // Rather than painstakingly check all of the rules, we'll just make sure that we got the right number of rules,
        // compared to the number of rules in the catalog.
        const catalog = getCatalogJson();
        const totalRuleCount = catalog.rules.length;

        // Split the output table by newline and throw out the first two rows, since they just contain header information. That
        // should leave us with the actual data.
        const rows = ctx.stdout.trim().split('\n');
        rows.shift();
        rows.shift();

        expect(rows).to.have.lengthOf(totalRuleCount, 'All rules should have been returned');
      });

    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list', '--json'])
      .it('--json flag yields expected JSON', ctx => {
        // Rather than painstakingly check all of the rules, we'll just make sure that we got the right number of rules,
        // compared to the number of rules in the catalog.
        const catalog = getCatalogJson();
        const totalRuleCount = catalog.rules.length;

        // Parse the output back into a JSON, then make sure it has the same number of rules as the catalog did.
        const outputJson = JSON.parse(ctx.stdout);

        expect(outputJson.result).to.have.lengthOf(totalRuleCount, 'All rules should have been returned');
      });
  });

  describe('Test Case: Filtering by category only', () => {
    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list', '--category', 'Best Practices', '--json'])
      .it('Filtering by one category returns only the rules in that category', ctx => {
        // Rather than painstakingly checking everything about all the rules, we'll just make sure that the number of rules
        // returned is the same as the number of rules in the target category, and that every rule returned is actually
        // a member of that category.
        // The first step is to identify how many satisfactory rules are in the catalog.
        const catalog = getCatalogJson();
        const targetRuleCount = catalog.rules.filter(rule => rule.categories.includes('Best Practices')).length;

        // Then, we parse the output back into a JSON, make sure it has the right number of rules, and make sure that each
        // rule is the right type.
        const outputJson = JSON.parse(ctx.stdout);

        expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules in the specified category should have been returned');
        outputJson.result.forEach((rule: Rule) => {
          expect(rule.categories).to.contain('Best Practices', `Rule ${rule.name} was included despite being in the wrong category`);
        });
      });

    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list', '--category', 'Best Practices,Design', '--json'])
      .it('Filtering by multiple categories returns any rule in either category', ctx => {
        // Count how many rules in the catalog fit the criteria.
        const catalog = getCatalogJson();
        const targetRuleCount = catalog.rules.filter(rule => rule.categories.includes('Best Practices') || rule.categories.includes('Design')).length;

        // Parse the output back into a JSON, and make sure it has the right number of rules.
        const outputJson = JSON.parse(ctx.stdout);
        expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules in either category should be returned');
        // Make sure that only rules in the right categories were returned.
        outputJson.result.forEach((rule: Rule) => {
          expect(rule).to.satisfy((rule) => {
            return rule.categories.includes('Best Practices') || rule.categories.includes('Design')},
            `Rule ${rule.name} was included despite being in the wrong category`
          );
        });
      });
  });

  describe('Test Case: Filtering by ruleset only', () => {
    test
      .stdout()
      .stderr()
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

    test
      .stdout()
      .stderr()
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
            return rule.rulesets.includes('Braces') || rule.rulesets.includes('ApexUnit')},
            `Rule ${rule.name} was included despite being in the wrong ruleset`
          );
        });
      });
  });

  describe('Test Case: Filtering by language only', () => {
    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list', '--language', 'apex', '--json'])
      .it('Filtering by a single language returns only rules applied to that language', ctx => {
        // Count how many rules in the catalog fit the criteria.
        const targetRuleCount = getCatalogJson().rules.filter(rule => rule.languages.includes('apex')).length;

        // Parse the output back into a JSON and make sure it has the right number of rules.
        const outputJson = JSON.parse(ctx.stdout);
        expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules of the desired language should be returned');
        // Make sure that only the right rules were returned.
        outputJson.result.forEach((rule) => {expect(rule.languages).to.contain('apex', `Rule ${rule.name} was included despite targeting the wrong language`)});
      });

    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list', '--language', 'apex,javascript', '--json'])
      .it('Filtering by multiple languages returns any rule for either language', ctx => {
        // Count how many rules in the catalog fit the criteria.
        const targetRuleCount = getCatalogJson().rules.filter(rule => rule.languages.includes('apex') || rule.languages.includes('javascript')).length;

        // Parse the output back into a JSON and make sure it has the right number of rules.
        const outputJson = JSON.parse(ctx.stdout);
        expect(outputJson.result).to.have.lengthOf(targetRuleCount, 'All rules of the desired languages should be returned');
        // Make sure that only the right rules were returned.
        outputJson.result.forEach((rule: Rule) => {
          expect(rule).to.satisfy((rule) => {
            return rule.languages.includes('apex') || rule.languages.includes('javascript')},
            `Rule ${rule.name} was included despite targeting neither desired language`
          );
        });
      });
  });

  describe('Test Case: Applying multiple filter types', () => {
    test
      .stdout()
      .stderr()
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
    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list', '--category', 'Beebleborp'])
      .it('Without --json flag, an empty table is printed', ctx => {
        // Split the result by newline, and make sure there are two rows.
        const rows = ctx.stdout.trim().split('\n');
        expect(rows).to.have.lengthOf(2, 'Only the header rows should have been printed');
      });

    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list', '--category', 'Beebleborp', '--json'])
      .it('With the --json flag, the results are empty', ctx => {
        // Parse the results back into a JSON and make sure it has an empty list.
        const outputJson = JSON.parse(ctx.stdout);
        expect(outputJson.result).to.have.lengthOf(0, 'No results should be included');
      });
  });
});
