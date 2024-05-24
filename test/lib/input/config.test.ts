import {SfCommand} from '@salesforce/sf-plugins-core';
import {expect} from 'chai';
import {CONFIG_FILE, flags as configFlags, ConfigInput} from '../../../src/lib/input/config';

describe('config.ts', () => {
	describe('flags', () => {
		// Declare a dummy Command that we can use to validate the functionality of the flags.
		class TestCommand extends SfCommand<string> {
			public static readonly flags = {
				...configFlags
			};

			public async run(): Promise<string> {
				const parsedFlags: ConfigInput = (await this.parse(TestCommand)).flags;
				return parsedFlags[CONFIG_FILE];
			}
		}

		describe('-c/--config-file', () => {
			describe('Long name: --config-file', () => {
				it('Rejects non-existent file', async () => {
					const inputValue = 'definitely-not-a-real-file.txt';
					let errorThrown: boolean;
					let message: string;
					try {
						await TestCommand.run(['--config-file', inputValue]);
						errorThrown = false;
					} catch (e) {
						errorThrown = true;
						message = e.message;
					}
					expect(errorThrown).to.equal(true, 'Error should be thrown');
					expect(message).to.contain(`No file found at ${inputValue}`);
				});

				it('Accepts existent file', async () => {
					const inputValue = 'package.json';
					const output = await TestCommand.run(['--config-file', inputValue]);
					expect(output).to.equal(inputValue);
				});

				it('Accepts exactly one value', async () => {
					const inputValue1 = 'package.json';
					const inputValue2 = 'LICENSE';
					let errorThrown: boolean;
					let message: string;
					try {
						await TestCommand.run(['--config-file', inputValue1, '--config-file', inputValue2]);
						errorThrown = false;
					} catch (e) {
						errorThrown = true;
						message = e.message;
					}
					expect(errorThrown).to.equal(true, 'Error should be thrown');
					expect(message).to.contain(`Flag --config-file can only be specified once`);
				});
			});

			describe('Short name: -c', () => {
				it('Rejects non-existent file', async () => {
					const inputValue = 'definitely-not-a-real-file.txt';
					let errorThrown: boolean;
					let message: string;
					try {
						await TestCommand.run(['-c', inputValue]);
						errorThrown = false;
					} catch (e) {
						errorThrown = true;
						message = e.message;
					}
					expect(errorThrown).to.equal(true, 'Error should be thrown');
					expect(message).to.contain(`No file found at ${inputValue}`);
				});

				it('Accepts existent file', async () => {
					const inputValue = 'package.json';
					const output = await TestCommand.run(['-c', inputValue]);
					expect(output).to.equal(inputValue);
				});

				it('Accepts exactly one value', async () => {
					const inputValue1 = 'package.json';
					const inputValue2 = 'LICENSE';
					let errorThrown: boolean;
					let message: string;
					try {
						await TestCommand.run(['-c', inputValue1, '-c', inputValue2]);
						errorThrown = false;
					} catch (e) {
						errorThrown = true;
						message = e.message;
					}
					expect(errorThrown).to.equal(true, 'Error should be thrown');
					expect(message).to.contain(`Flag --config-file can only be specified once`);
				});
			});
		});
	});
});
