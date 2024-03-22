import {expect} from 'chai';
import {runCommand} from '../../TestUtils';
import path = require('path');
import * as TestUtils from '../../TestUtils';
import {BundleName, getMessage} from "../../../src/MessageCatalog";

describe('scanner run tests that result in the use of RuleFilters', function () {
	describe('Single filter', () => {
		describe('Positive constraints', () => {
			it('Case: --engine eslint-lwc against clean LWC code', () => {
				const output = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'projects', 'lwc')} --format csv --engine eslint-lwc`);
				const stdout = output.shellOutput.stdout;
				// If there's a summary, then it'll be separated from the CSV by an empty line. Throw it away.
				const [csv, _] = stdout.trim().split(/\n\r?\n/);

				// Confirm there are no violations.
				// Since it's a CSV, the rows themselves are separated by newline characters.
				// The header should not have any newline characters after it. There should be no violation rows.
				expect(csv.indexOf('\n')).to.equal(-1, "Should be no violations detected");
			});

			it('Case: --engine eslint-lwc against unclean LWC code', () => {
				const output = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'invalid-lwc')} --format json --engine eslint-lwc`);
				const stdout = output.shellOutput.stdout;
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

			it('Case: --engine pmd-appexchange against clean code', () => {
				const output = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'projects', 'pmd-appexchange-test-app', 'objects', 'clean.object')} --format csv --engine pmd-appexchange`);
				const stdout = output.shellOutput.stdout;
				// If there's a summary, then it'll be separated from the CSV by an empty line. Throw it away.
				const [csv, _] = stdout.trim().split(/\n\r?\n/);

				// Confirm there are no violations.
				// Since it's a CSV, the rows themselves are separated by newline characters.
				// The header should not have any newline characters after it. There should be no violation rows.
				expect(csv.indexOf('\n')).to.equal(-1, "Should be no violations detected");
			});

			// Currently this test fails because the pmd-appexchange jar files depend on classes that only exist in PMD6
			// TODO: Turn this test back on as soon as we get the new jar files for pmd-appexchange that work with PMD7
			xit('Case: --engine pmd-appexchange against unclean code', () => {
				const output = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'projects', 'pmd-appexchange-test-app', 'objects', 'unclean.object')} --format json --engine pmd-appexchange`);
				const stdout = output.shellOutput.stdout;
				const results = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
				expect(results, `results does not have expected length. ${results.map(r => r.fileName).join(',')}`)
					.to.be.an('Array').that.has.length(1);
				const messages = results[0].violations.map(v => v.message.trim());
				const expectedMessages = ['Use Lightning Message Channel with isExposed set to false.'];
				for (const expectedMessage of expectedMessages) {
					expect(messages).to.contain(expectedMessage);
				}
			});
		});

		describe('Negative constraints', () => {
			it('Case: Negate one category', () => {
				const category = 'Code Style';
				const output = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'apex')} --format json --category "!${category}"`);
				const stdout = output.shellOutput.stdout;
				const results = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));

				expect(results.length).greaterThan(0);
				for (const result of results) {
					for (const violation of result.violations) {
						expect(violation.category, TestUtils.prettyPrint(violation)).to.not.equal(category);
					}
				}
			});

			it('Case: Negate multiple categories', () => {
				const category = `!Code Style,!Security`;
				const nonExpectedCategories: Set<string> = new Set<string>();
				nonExpectedCategories.add('Code Style').add('Security');

				const output = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'apex')} --format json --category "${category}"`);
				const stdout = output.shellOutput.stdout;
				const results = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
				expect(results.length).greaterThan(0);

				for (const result of results) {
					for (const violation of result.violations) {
						expect(nonExpectedCategories.has(violation.category), TestUtils.prettyPrint(violation)).to.be.false;
					}
				}
			});

			it('Case: Negate non-negatable filter (--ruleset)', () => {
				const ruleset = `'!some-value'`;
				const output = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'apex')} --format json --ruleset ${ruleset}`);
				expect(output.shellOutput.stderr).to.contain(getMessage(BundleName.Exceptions, 'RuleFilter.PositiveOnly', ['Ruleset']), '--ruleset should not be negateable');
			});
		});

		it('Case: Mixing positive and negative constraints', () => {
			const category = `!Code Style,Security`;
			const output = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'apex')} --format json --category "${category}"`);
			expect(output.shellOutput.stderr).to.contain(getMessage(BundleName.Exceptions, 'RuleFilter.MixedTypes', ['Category']), 'Cannot mix positive and negative constraints');
		});
	});

	describe('Multiple filters', () => {
		const pathToDomParserController = path.join('test', 'code-fixtures', 'projects', 'app', 'force-app', 'main', 'default', 'aura', 'dom_parser', 'dom_parserController.js');
		it('Case: --engine eslint --category problem', () => {
			const category = 'problem';
			const expectedViolationCount = 3;
			const commandOutput = runCommand(`scanner run --target ${pathToDomParserController} --format json --engine eslint --category ${category}`);
			const stdout = commandOutput.shellOutput.stdout;
			const output = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
			expect(output.length).to.equal(1, 'Should only be violations from one file');
			expect(output[0].engine).to.equal('eslint');
			expect(output[0].violations, TestUtils.prettyPrint(output[0].violations)).to.be.lengthOf(expectedViolationCount);

			// Make sure only violations are returned for the requested category
			for (const v of output[0].violations) {
				expect(v.category, TestUtils.prettyPrint(v)).to.equal(category);
			}
		});

		it('Case: --engine eslint-lwc --category suggestion', () => {
			const category = 'suggestion';
			const expectedViolationCount = 35;
			const commandOutput = runCommand(`scanner run --target ${pathToDomParserController} --format json --engine eslint-lwc --category ${category}`)
			const stdout = commandOutput.shellOutput.stdout;
			const output = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
			expect(output.length).to.equal(1, 'Should only be violations from one file');
			expect(output[0].engine).to.equal('eslint-lwc');
			expect(output[0].violations, TestUtils.prettyPrint(output[0].violations)).to.be.lengthOf(expectedViolationCount);

			// Make sure only violations are returned for the requested category
			for (const v of output[0].violations) {
				expect(v.category, TestUtils.prettyPrint(v)).to.equal(category);
			}
		});

		it('Case: --engine eslint-typescript --category problem', () => {
			const category = 'problem';
			const target = path.join('test', 'code-fixtures', 'projects', 'ts', 'src', 'simpleYetWrong.ts');
			const config = path.join('test', 'code-fixtures', 'projects', 'tsconfig.json');
			const expectedViolationCount = 4;
			const commandOutput = runCommand(`scanner run --target ${target} --tsconfig ${config} --format json --engine eslint-typescript --category ${category}`);
			const stdout = commandOutput.shellOutput.stdout;
			const output = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
			expect(output.length).to.equal(1, 'Should only be violations from one file');
			expect(output[0].engine).to.equal('eslint-typescript');
			expect(output[0].violations, TestUtils.prettyPrint(output[0].violations)).to.be.lengthOf(expectedViolationCount);

			// Make sure only violations are returned for the requested category
			for (const v of output[0].violations) {
				expect(v.category, TestUtils.prettyPrint(v)).to.equal(category);
			}
		});

		it('Case: --engine pmd --category \'Code Style\'', () => {
			const category = 'Code Style';
			const commandOutput = runCommand(`scanner run --target ${path.join('test', 'code-fixtures', 'apex')} --format json --engine pmd --category "${category}"`);
			const stdout = commandOutput.shellOutput.stdout;
			const output = JSON.parse(stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1));
			expect(output.length).to.equal(1, 'Should only be violations from one file');
			expect(output[0].engine).to.equal('pmd');
			expect(output[0].violations, TestUtils.prettyPrint(output[0].violations)).to.be.lengthOf(1);

			// Make sure only violations are returned for the requested category
			for (const v of output[0].violations) {
				expect(v.category, TestUtils.prettyPrint(v)).to.equal(category);
			}
		});
	});
});
