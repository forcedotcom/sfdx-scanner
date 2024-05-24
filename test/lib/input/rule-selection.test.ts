import {SfCommand} from '@salesforce/sf-plugins-core';
import {expect} from 'chai';
import {RULE_SELECTOR, flags as ruleSelectorFlags, RuleSelectorInput} from '../../../src/lib/input/rule-selection';

describe('rule-selection.ts', () => {
	describe('flags', () => {
		// Declare a dummy Command that we can use to validate the functionality of the flags.
		class TestCommand extends SfCommand<string[]> {
			public static readonly flags = {
				...ruleSelectorFlags
			};

			public async run(): Promise<string[]> {
				const parsedFlags: RuleSelectorInput = (await this.parse(TestCommand)).flags;
				return parsedFlags[RULE_SELECTOR];
			}
		}

		describe('-r/--rule-selector', () => {
			describe('Long name: --rule-selector', () => {
				it('Flag can be supplied once with a single value', async () => {
					const inputValue = 'abcde';
					const output = await TestCommand.run(['--rule-selector', inputValue]);
					expect(output).to.deep.equal([inputValue]);
				});

				it('Flag can be supplied once with multiple values', async () => {
					const inputValue =["abcde", "degfh"];
					const output = await TestCommand.run(['--rule-selector', inputValue.join(',')]);
					expect(output).to.deep.equal(inputValue);
				});

				it('Flag can be supplied multiple times with one value each', async () => {
					const inputValue1 = 'abcde';
					const inputValue2 = 'defgh';
					const output = await TestCommand.run(['--rule-selector', inputValue1, '--rule-selector', inputValue2]);
					expect(output).to.deep.equal([inputValue1, inputValue2]);
				});

				it('Flag can be supplied multiple times with multiple value each', async () => {
					const inputValue1 =["abcde", "degfh"];
					const inputValue2 = ["hijkl", "lmnop"];
					const output = await TestCommand.run(['--rule-selector', inputValue1.join(','), '--rule-selector', inputValue2.join(',')]);
					expect(output).to.deep.equal([...inputValue1, ...inputValue2]);
				});
			});

			describe('Short name: -r', () => {
				it('Flag can be supplied once with a single value', async () => {
					const inputValue = 'abcde';
					const output = await TestCommand.run(['-r', inputValue]);
					expect(output).to.deep.equal([inputValue]);
				});

				it('Flag can be supplied once with multiple values', async () => {
					const inputValue =["abcde", "degfh"];
					const output = await TestCommand.run(['-r', inputValue.join(',')]);
					expect(output).to.deep.equal(inputValue);
				});

				it('Flag can be supplied multiple times with one value each', async () => {
					const inputValue1 = 'abcde';
					const inputValue2 = 'defgh';
					const output = await TestCommand.run(['-r', inputValue1, '-r', inputValue2]);
					expect(output).to.deep.equal([inputValue1, inputValue2]);
				});

				it('Flag can be supplied multiple times with multiple value each', async () => {
					const inputValue1 =["abcde", "degfh"];
					const inputValue2 = ["hijkl", "lmnop"];
					const output = await TestCommand.run(['-r', inputValue1.join(','), '-r', inputValue2.join(',')]);
					expect(output).to.deep.equal([...inputValue1, ...inputValue2]);
				});
			});
		})
	});
});
