import {expect, test} from '@salesforce/command/lib/test';
import * as TestOverrides from '../../../test-related-lib/TestOverrides';
import messages = require('../../../../messages/describe');

describe('scanner:rule:describe', () => {
	// Reset our controller since we are using alternate file locations
	before(() => TestOverrides.initializeTestSetup());

	describe('E2E', () => {
		it('Test Case: No matching rules', () => {
			const formattedWarning = messages.output.noMatchingRules.replace('{0}', 'DefinitelyFakeRule');
			test
				.stdout() // Adds stdout to the test's context object.
				.stderr() // Adds stderr to the test's context object.
				.command(['scanner:rule:describe', '--rulename', 'DefinitelyFakeRule'])
				.it('Correct warning is displayed', ctx => {
					expect(ctx.stderr).to.contain('WARNING: ' + formattedWarning, 'Warning message should match');
				});

				test
				.stdout()
				.stderr()
				.command(['scanner:rule:describe', '--rulename', 'DefinitelyFakeRule', '--json'])
				.it('--json flag yields correct results', ctx => {
					const ctxJson = JSON.parse(ctx.stdout);
					expect(ctxJson.result.length).to.equal(0, 'Should be no results');
					expect(ctxJson.warnings.length).to.equal(1, 'Should be one warning');
					expect(ctxJson.warnings[0]).to.equal(formattedWarning, 'Warning message should match');
				});
		});

		it('Test Case: One matching rule', () => {
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

		it('Test Case: Multiple matching rules', () => {
			// Both tests will test for the presence of this warning string in the output, so we might as well format it up here.
			const formattedWarning = messages.output.multipleMatchingRules
				.replace('{0}', '2')
				.replace('{1}', 'constructor-super');

				test
				.stdout()
				.stderr()
				.command(['scanner:rule:describe', '--rulename', 'constructor-super'])
				.it('Displayed output matches expectations', ctx => {
					// First, verify that the warning was printed at the start like it should have been.
					expect(ctx.stderr).to.contain('WARNING: ' + formattedWarning, 'Warning message should be formatted correctly');

					// Next, verify that there are two rule descriptions that are distinctly identified.
					const regex = /=== Rule #1\nname:\s+constructor-super(.*\n)*=== Rule #2\nname:\s+constructor-super/g;
					expect(ctx.stdout).to.match(regex, 'Output should contain two rules named constructor-super');
				});

			// TODO: THIS TEST IS FAILING BECAUSE THE WARNING FOR DEFINITELYFAKERULE IS BEING INCLUDED IN THE WARNINGS HERE.
			//  WHEN THE CAUSE FOR THAT IS DETERMINED AND RESOLVED, RE-ENABLE THIS TEST.
			/*
			describeTest
			  .stdout()
			  .stderr()
			  .command(['scanner:rule:describe', '--rulename', 'WhileLoopsMustUseBraces', '--json'])
			  .it('--json flag yields correct results', ctx => {
				const ctxJson = JSON.parse(ctx.stdout);

				// Verify we got the right number of rules.
				expect(ctxJson.result).to.have.lengthOf(2, 'Should be two included rules');
				// Verify that there's only one warning.
				expect(ctxJson.warnings).to.have.lengthOf(1, 'Should be only one warning, instead ' + ctx.stdout);
				// Verify that the warning looks right.
				expect(ctxJson.warnings[0]).to.equal(formattedWarning, 'Warning should match');
			  });
			 */
		});

		it('Error handling', () => {
			test
				.stdout() // Adds stdout to the test's context object.
				.stderr() // Adds stderr to the test's context object.
				.command(['scanner:rule:describe'])
				.it('Must input a rule name', ctx => {
					expect(ctx.stderr).to.contain('ERROR running scanner:rule:describe:  Missing required flag:\n -n, --rulename RULENAME');
				});
		});
	});
});
