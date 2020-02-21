import { expect, test } from '@salesforce/command/lib/test';
import fs = require('fs');

// NOTE: When we're running npm test, the current working directory is actually going to be the top-level directory of
// the package, rather than the location of this file itself.
const messages = JSON.parse(fs.readFileSync('./messages/describe.json').toString());

describe('scanner:rule:describe', () => {
  describe('Test Case: No matching rules', () => {
    test
      .stdout() // Adds stdout to the test's context object.
      .stderr() // Adds stderr to the test's context object.
      .command(['scanner:rule:describe', '--rulename', 'DefintelyFakeRule'])
      .it('Correct warning is displayed', ctx => {
        expect(ctx.stderr).to.contain(`WARNING: ${messages.output.noMatchingRules}`);
      });

    test
      .stdout()
      .stderr()
      .command(['scanner:rule:describe', '--rulename', 'DefinitelyFakeRule', '--json'])
      .it('--json flag yields correct results', ctx => {
        const ctxJson = JSON.parse(ctx.stdout);
        expect(ctxJson.result.length).to.equal(0);
        expect(ctxJson.warnings.length).to.equal(1);
        expect(ctxJson.warnings[0]).to.equal(messages.output.noMatchingRules);
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
      });

    test
      .stdout()
      .stderr()
      .command(['scanner:rule:describe', '--rulename', 'TooManyFields', '--json'])
      .it('--json flag yields correct results', ctx => {
        expect(true).to.be.true;
      });
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
