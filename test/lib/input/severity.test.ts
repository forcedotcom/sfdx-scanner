import {SfCommand} from '@salesforce/sf-plugins-core';
import {expect} from 'chai';
import {SEVERITY_THRESHOLD, flags as severityFlags, SeverityInput} from '../../../src/lib/input/severity';


describe('severity.ts', () => {
	describe('flags', () => {
		// Declare a dummy Command that we can use to validate the functionality of the flags.
		class TestCommand extends SfCommand<string> {
			public static readonly flags = {
				...severityFlags
			};

			public async run(): Promise<string> {
				const parsedFlags: SeverityInput = (await this.parse(TestCommand)).flags;
				return parsedFlags[SEVERITY_THRESHOLD];
			}
		}

		describe('-s/--severity-threshold', () => {
			describe('Long name: --severity-threshold', () => {
				it('Accepts integer between 1 and 5', async () => {
					const inputValue = "3";
					const output = await TestCommand.run(['--severity-threshold', inputValue]);
					expect(output).to.equal(inputValue);
				});

				it('Accepts lower-case severity string', async () => {
					const inputValue = "critical";
					const output = await TestCommand.run(['--severity-threshold', inputValue]);
					expect(output).to.equal(inputValue);
				});

				it('Rejects numbers less than 1', async () => {
					const inputValue = "0";
					let errorThrown: boolean;
					let message: string;
					try {
						await TestCommand.run(['--severity-threshold', inputValue]);
						errorThrown = false;
					} catch (e) {
						errorThrown = true;
						message = e.message;
					}
					expect(errorThrown).to.equal(true, 'Error should be thrown');
					expect(message).to.contain(`Expected --severity-threshold=${inputValue} to be one of`);
				});

				it('Rejects numbers above 5', async () => {
					const inputValue = "7";
					let errorThrown: boolean;
					let message: string;
					try {
						await TestCommand.run(['--severity-threshold', inputValue]);
						errorThrown = false;
					} catch (e) {
						errorThrown = true;
						message = e.message;
					}
					expect(errorThrown).to.equal(true, 'Error should be thrown');
					expect(message).to.contain(`Expected --severity-threshold=${inputValue} to be one of`);
				});

				it('Rejects improperly-cased severities', async () => {
					const inputValue = "CRITICAL";
					let errorThrown: boolean;
					let message: string;
					try {
						await TestCommand.run(['--severity-threshold', inputValue]);
						errorThrown = false;
					} catch (e) {
						errorThrown = true;
						message = e.message;
					}
					expect(errorThrown).to.equal(true, 'Error should be thrown');
					expect(message).to.contain(`Expected --severity-threshold=${inputValue} to be one of`);
				});

				it('Rejects non-standard values', async () => {
					const inputValue = "crit";
					let errorThrown: boolean;
					let message: string;
					try {
						await TestCommand.run(['--severity-threshold', inputValue]);
						errorThrown = false;
					} catch (e) {
						errorThrown = true;
						message = e.message;
					}
					expect(errorThrown).to.equal(true, 'Error should be thrown');
					expect(message).to.contain(`Expected --severity-threshold=${inputValue} to be one of`);
				});

				it('Accepts exactly one value', async () => {
					const inputValue1 = "3";
					const inputValue2 = "4";
					let errorThrown: boolean;
					let message: string;
					try {
						await TestCommand.run(['--severity-threshold', inputValue1, '--severity-threshold', inputValue2]);
						errorThrown = false;
					} catch (e) {
						errorThrown = true;
						message = e.message;
					}
					expect(errorThrown).to.equal(true, 'Error should be thrown');
					expect(message).to.contain(`Flag --severity-threshold can only be specified once`);
				});
			})
		});
	})
});
