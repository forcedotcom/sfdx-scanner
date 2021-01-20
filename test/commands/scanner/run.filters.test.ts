import {expect} from '@salesforce/command/lib/test';
import {setupCommandTest} from '../../TestUtils';
import {Messages} from '@salesforce/core';
import path = require('path');
import * as TestUtils from '../../TestUtils';

Messages.importMessagesDirectory(__dirname);
const exceptionMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'Exceptions');

/**
 * IMPORTANT. The oclif CLI test framework requires the .it to come after the .command
 */
describe('scanner:run tests that result in the use of RuleFilters', function () {
	describe('--engine flag', () => {
		setupCommandTest
			.command(['scanner:run',
				'--target', path.join('test', 'code-fixtures', 'lwc'),
				'--format', 'csv',
				'--engine', 'eslint-lwc'
			])
			.it('LWC Engine Successfully parses LWC code', ctx => {
				expect(ctx.stdout).to.contain('No rule violations found.');
			});

		setupCommandTest
			.command(['scanner:run',
				'--target', path.join('test', 'code-fixtures', 'invalid-lwc'),
				'--format', 'json',
				'--engine', 'eslint-lwc'
			])
			.it('LWC Engine detects LWC errors', ctx => {
				const stdout = ctx.stdout;
				const results = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
				expect(results, `results does not have expected length. ${results.map(r => r.fileName).join(',')}`)
					.to.be.an('Array').that.has.length(1);
				const messages = results[0].violations.map(v => v.message);
				const expectedMessages = ['Invalid public property initialization for "foo". Boolean public properties should not be initialized to "true", consider initializing the property to "false".',
					`'Foo' is defined but never used.`];
				for (const expectedMessage of expectedMessages) {
					expect(messages).to.contain(expectedMessage);
				}
			});
	});

	describe('--category and --engine flag', () => {
		describe('Eslint Javascript Engine --category flag', () => {
			const category = 'Possible Errors';
			const expectedViolationCount = 4;
			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'projects', 'app', 'force-app', 'main', 'default', 'aura', 'dom_parser', 'dom_parserController.js'),
					'--format', 'json',
					'--engine', 'eslint',
					'--category', category
				])
				.it('Only correct categories are returned', ctx => {
					const stdout = ctx.stdout;
					const output = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
					expect(output.length).to.equal(1, 'Should only be violations from one file');
					expect(output[0].engine).to.equal('eslint');
					expect(output[0].violations, TestUtils.prettyPrint(output[0].violations)).to.be.lengthOf(expectedViolationCount);

					// Make sure only violations are returned for the requested category
					for (const v of output[0].violations) {
						expect(v.category, TestUtils.prettyPrint(v)).to.equal(category);
					}
				});
		});

		describe('Eslint LWC Engine --category flag', () => {
			const category = 'Best Practices';
			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'projects', 'app', 'force-app', 'main', 'default', 'aura', 'dom_parser', 'dom_parserController.js'),
					'--format', 'json',
					'--engine', 'eslint-lwc',
					'--category', category
				])
				.it('Only correct categories are returned', ctx => {
					const stdout = ctx.stdout;
					const output = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
					expect(output.length).to.equal(1, 'Should only be violations from one file');
					expect(output[0].engine).to.equal('eslint-lwc');
					expect(output[0].violations, TestUtils.prettyPrint(output[0].violations)).to.be.lengthOf(13);

					// Make sure only violations are returned for the requested category
					for (const v of output[0].violations) {
						expect(v.category, TestUtils.prettyPrint(v)).to.equal(category);
					}
				});

		});

		describe('Eslint Typescript Engine --category flag', () => {
			const category = 'Possible Errors';
			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'projects', 'ts', 'src', 'simpleYetWrong.ts'),
					'--tsconfig', path.join('test', 'code-fixtures', 'projects', 'tsconfig.json'),
					'--format', 'json',
					'--engine', 'eslint-typescript',
					'--category', category
				])
				.it('Only correct categories are returned', ctx => {
					const stdout = ctx.stdout;
					const output = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
					expect(output.length).to.equal(1, 'Should only be violations from one file');
					expect(output[0].engine).to.equal('eslint-typescript');
					expect(output[0].violations, TestUtils.prettyPrint(output[0].violations)).to.be.lengthOf(2);

					// Make sure only violations are returned for the requested category
					for (const v of output[0].violations) {
						expect(v.category, TestUtils.prettyPrint(v)).to.equal(category);
					}
				});
		});

		describe('PMD Engine --category flag', () => {
			const category = 'Code Style';
			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex'),
					'--format', 'json',
					'--engine', 'pmd',
					'--category', category
				])
				.it('Only correct categories are returned', ctx => {
					const stdout = ctx.stdout;
					const output = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
					expect(output.length).to.equal(1, 'Should only be violations from one file');
					expect(output[0].engine).to.equal('pmd');
					expect(output[0].violations, TestUtils.prettyPrint(output[0].violations)).to.be.lengthOf(2);

					// Make sure only violations are returned for the requested category
					for (const v of output[0].violations) {
						expect(v.category, TestUtils.prettyPrint(v)).to.equal(category);
					}
				});
		});
	});

	describe('Negated --category', () => {
		describe('Single --category', () => {
			const category = '!Code Style';
			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex'),
					'--format', 'json',
					'--category', category
				])
				.it('Only correct categories are returned', ctx => {
					const stdout = ctx.stdout;
					const output = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
					expect(output.length, TestUtils.prettyPrint(output)).to.equal(4);
					for (const file of output) {
						expect(file.engine).to.equal('pmd');
						for (const violation of file.violations) {
							expect(violation.category, TestUtils.prettyPrint(violation)).to.not.equal(category);
						}
					}
				});
		});

		describe('Multiple --category', () => {
			const category = '!Code Style,!Security';
			const expectedCategories: Set<string> = new Set<string>();
			expectedCategories.add('Code Style').add('Security');
			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex'),
					'--format', 'json',
					'--category', category
				])
				.it('Only correct categories are returned', ctx => {
					const stdout = ctx.stdout;
					const output = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
					expect(output.length, TestUtils.prettyPrint(output)).to.equal(4);
					for (const file of output) {
						expect(file.engine).to.equal('pmd');
						for (const violation of file.violations) {
							expect(expectedCategories.has(violation.category), TestUtils.prettyPrint(violation)).to.be.false;
						}
					}
				});
		});

		describe('Invalid --category', () => {
			// Positive and negative categories can't be mixed
			const category = '!Code Style,Security';
			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex'),
					'--format', 'json',
					'--category', category
				])
				.it('Positive and Negative Combinations throws an error', ctx => {
					expect(ctx.stderr).to.contain(exceptionMessages.getMessage('RuleFilter.MixedTypes', ['Category']));
				});
		});

		describe('Invalid --engine', () => {
			// Ruleset doesn't support negation
			const ruleset = '!some-value';
			setupCommandTest
				.command(['scanner:run',
					'--target', path.join('test', 'code-fixtures', 'apex'),
					'--format', 'json',
					'--ruleset', ruleset
				])
				.it('Negative filters that are not supported throws an error', ctx => {
					expect(ctx.stderr).to.contain(exceptionMessages.getMessage('RuleFilter.PositiveOnly', ['Ruleset']));
				});
		});
	});
});
