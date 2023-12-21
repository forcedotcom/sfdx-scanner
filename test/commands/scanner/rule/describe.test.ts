import {expect} from 'chai';
// @ts-ignore
import {runCommand} from '../../../TestUtils';
import {getMessage, Bundle} from '../../../../src/MessageCatalog';

describe('scanner rule describe', () => {
	describe('E2E', () => {
		describe('Test Case: No matching rules', () => {
			const formattedWarning = getMessage(Bundle.Describe, 'output.noMatchingRules', ['DefinitelyFakeRule']);
			it('Correct warning is displayed', () => {
				const output = runCommand(`scanner rule describe --rulename DefinitelyFakeRule`);
				expect(output.shellOutput.stderr.toLowerCase()).to.contain(`WARNING: ${formattedWarning}`.toLowerCase(), 'Warning message should match');
			});

			it('--json flag yields correct results', () => {
				const output = runCommand(`scanner rule describe --rulename DefinitelyFakeRule --json`);
				const ctxJson = output.jsonOutput;
				expect((ctxJson.result as any[]).length).to.equal(0, 'Should be no results');
				expect(ctxJson.warnings.length).to.equal(2, 'Incorrect warning count');
				expect(ctxJson.warnings[1]).to.equal(formattedWarning, 'Warning message should match');
			});
		});

		describe('Test Case: One matching rule', () => {
			it('Displayed output matches expectations', () => {
				const output = runCommand(`scanner rule describe --rulename TooManyFields`);
				// Rather than compare every attribute, we'll just compare a few, so we can be reasonably confident we got the right
				// rule.
				const ctx = output.shellOutput;
				expect(ctx.stdout).to.match(/name:\s+TooManyFields/, 'Name should be \'TooManyFields\'');
				expect(ctx.stdout).to.match(/engine:\s+pmd/, 'Engine should be PMD');
				expect(ctx.stdout).to.match(/enabled:\s+true/, 'Rule is enabled');
				expect(ctx.stdout).to.match(/categories:\s+Design/, 'Category should be \'Design\'');
				expect(ctx.stdout).to.match(/languages:\s+apex/, 'Language should be \'apex\'');
				expect(ctx.stdout).to.match(/message:\s+Too many fields/, 'Message should match');
			});

			it('--json flag yields correct results', () => {
				const output = runCommand(`scanner rule describe --rulename TooManyFields --json`);
				const resultList = output.jsonOutput.result as any[];
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
			// Both tests will test for the presence of this warning string in the output, so we might as well format it up here.
			const formattedWarning = getMessage(Bundle.Describe, 'output.multipleMatchingRules', ['3', 'constructor-super']);

			it('Displayed output matches expectations', () => {
				const output = runCommand(`scanner rule describe --rulename constructor-super`);
				const ctx = output.shellOutput;
				// First, verify that the warning was printed at the start like it should have been.
				expect(ctx.stderr.toLowerCase()).to.contain(`WARNING: ${formattedWarning}`.toLowerCase(), 'Warning message should be formatted correctly');

				// Next, verify that there are rule descriptions that are distinctly identified.
				const regex = /=== Rule #1\n\nname:\s+constructor-super(.*\n)*=== Rule #2\n\nname:\s+constructor-super(.*\n)*=== Rule #3\n\nname:\s+constructor-super/g;
				expect(ctx.stdout).to.match(regex, 'Output should contain three rules named constructor-super for each eslint based engine');
			});

			it('--json flag yields correct results', () => {
				const output = runCommand(`scanner rule describe --rulename constructor-super --json`);
				const ctxJson = output.jsonOutput;

				// Verify we got the right number of rules.
				expect(ctxJson.result).to.have.lengthOf(3, 'Should be three included rules');
				// There are two warnings, but only the second is relevant.
				expect(ctxJson.warnings).to.have.lengthOf(2, 'Should be two warnings');
				expect(ctxJson.warnings[1]).to.equal(formattedWarning, 'Warning should match');
			});
		});

		describe('Error handling', () => {
			it('Must input a rule name', () => {
				const output = runCommand(`scanner rule describe`);
				expect(output.shellOutput.stderr).to.contain(`The following error occurred:
  Missing required flag rulename`);
			});
		});
	});
});
