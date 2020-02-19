import { expect, test } from '@salesforce/command/lib/test';

describe('scanner:rule:describe', () => {
  describe('Test Case: No matching rules', () => {
    test
      .stdout() // Adds stdout to the test's context object.
      .stderr() // Adds stderr to the test's context object.
      .command(['scanner:rule:describe', '--rulename', 'DefintelyFakeRule'])
      .it('Correct warning is displayed', ctx => {
        // TODO: Add the message once it's available.
        expect(ctx.stderr).to.contain('WARNING:');
      });

    test
      .stdout()
      .stderr()
      .command(['scanner:rule:describe', '--rulename', 'DefinitelyFakeRule', '--json'])
      .it('--json flag yields correct results', ctx => {
        // TODO: Implement this test fully.
        expect(true).to.be.true;
      });
  });

  describe('Test Case: One matching rule', () => {
    test
      .stdout()
      .stderr()
      .command(['scanner:rule:describe', '--rulename', 'TooManyFields'])
      .it('Displayed output matches expectations', ctx => {
        // TODO: Implement this test.
        expect(true).to.be.true;
      })
  });

  describe('Test Case: Multiple matching rules', () => {
    // TODO: The setup for these tests can't be done until we add support for custom rules and/or make it so the catalog
    //  isn't completely rebuilt every time.
  });

  describe('Error handling', () => {
    test
      .stdout() // Adds stdout to the test's context object.
      .stderr() // Adds stderr to the test's context object.
      .command(['scanner:rule:describe'])
      .it('Must input a rule name', ctx => {
        expect(ctx.stderr).to.contain('ERROR running scanner:rule:describe:  Missing required flag:\n -n, --rulename RULENAME');
      });
  });
});
