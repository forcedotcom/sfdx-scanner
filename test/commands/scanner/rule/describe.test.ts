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
        expect(ctx.stderr).to.contain(`WARNING: ${messages.output.noMatchingRules}`, 'Warning message should match');
      });

    test
      .stdout()
      .stderr()
      .command(['scanner:rule:describe', '--rulename', 'DefinitelyFakeRule', '--json'])
      .it('--json flag yields correct results', ctx => {
        const ctxJson = JSON.parse(ctx.stdout);
        expect(ctxJson.result.length).to.equal(0, 'Should be no results');
        expect(ctxJson.warnings.length).to.equal(1, 'Should be one warning');
        expect(ctxJson.warnings[0]).to.equal(messages.output.noMatchingRules, 'Warning message should match');
      });
  });

  describe('Test Case: One matching rule', () => {
    test
      .stdout()
      .stderr()
      .command(['scanner:rule:describe', '--rulename', 'TooManyFields'])
      .it('Displayed output matches expectations', ctx => {
        // Rather than compare every attribute, we'll just compare a few so we can be reasonably confident we got the right
        // rule.
        expect(ctx.stdout).to.match(/name:\s+TooManyFields/, 'Name should be \'TooManyFields\'');
        expect(ctx.stdout).to.match(/categories:\s+Design/, 'Category should be \'Design\'');
        expect(ctx.stdout).to.match(/languages:\s+apex/, 'Language should be \'apex\'');
        expect(ctx.stdout).to.match(/message:\s+Too many fields/, 'Message should match');
      });

    test
      .stdout()
      .stderr()
      .command(['scanner:rule:describe', '--rulename', 'TooManyFields', '--json'])
      .it('--json flag yields correct results', ctx => {
        const resultList = JSON.parse(ctx.stdout).result;
        expect(resultList.length).to.equal(1, 'Should be one matching rule');
        const ruleJson = resultList[0];
        // Rather than compare every attribute, we'll just compare a few so we can be reasonably confident we got the right
        // rule.
        expect(ruleJson.name).to.equal('TooManyFields');
        expect(ruleJson.languages.length).to.equal(1, 'Should be one language applied to rule');
        expect(ruleJson.languages[0]).to.equal('apex', 'Rule language should be apex');
        expect(ruleJson.message).to.equal('Too many fields', 'Rule message should match');
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
