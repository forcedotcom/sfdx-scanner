import { expect, test } from '@salesforce/command/lib/test';

describe('scanner:rule:list', () => {
  describe('Test Case: No filters applied only', () => {
    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list'])
      .it('All rules are returned', ctx => {
        // TODO: Implement the actual checks.
        expect(true).to.be.true;
      });

    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list', '--json'])
      .it('--json flag yields expected JSON', ctx => {
        // TODO: Implement the actual checks.
        expect(true).to.be.true;
      });
  });

  describe('Test Case: Filtering by category only', () => {
    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list', '--category', '"Best Practices"'])
      .it('Filtering by one category returns only the rules in that category', ctx => {
        // TODO: Implement the actual checks.
        expect(true).to.be.true;
      });

    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list', '--category', '"Best Practices;Design"'])
      .it('Filtering by multiple categories returns any rule in either category', ctx => {
        // TODO: Implement the actual checks.
        expect(true).to.be.true;
      });
  });

  describe('Test Case: Filtering by ruleset only', () => {
    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list', '--ruleset', '"ApexUnit"'])
      .it('Filtering by a single ruleset returns only the rules in that ruleset', ctx => {
        // TODO: Implement the actual checks.
        expect(true).to.be.true;
      });

    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list', '--ruleset', '"ApexUnit;Braces"'])
      .it('Filtering by multiple rulesets returns any rule in either ruleset', ctx => {
        // TODO: Implement the actual checks.
        expect(true).to.be.true;
      });
  });

  describe('Test Case: Filtering by language only', () => {
    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list', '--language', 'apex'])
      .it('Filtering by a single language returns only rules applied to that language', ctx => {
        // TODO: Implement the actual checks.
        expect(true).to.be.true;
      });

    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list', '--language', '"apex;javascript"'])
      .it('Filtering by multiple languages returns any rule for either language', ctx => {
        // TODO: Implement the actual checks.
        expect(true).to.be.true;
      });
  });

  describe('Test Case: Applying multiple filter types', () => {
    test
      .stdout()
      .stderr()
      .command(['scanner:rule:list', '--category', '"Best Practices"', '--language', '"apex"'])
      .it('Filtering on multiple columns only returns rows that satisfy both filters', ctx => {
        // TODO: Implement the actual checks.
        expect(true).to.be.true;
      });
  });
});
