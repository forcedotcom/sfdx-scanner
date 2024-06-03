import {stubSfCommandUx} from '@salesforce/sf-plugins-core';
import {TestContext} from '@salesforce/core/lib/testSetup';
import RulesCommand from '../../../src/commands/code-analyzer/rules';
import {RulesAction} from '../../../src/lib/actions/RulesAction';

describe('`code-analyzer rules` tests', () => {
	const $$ = new TestContext();

	let spy: jest.SpyInstance;
	let receivedActionInput: object;
	beforeEach(() => {
		stubSfCommandUx($$.SANDBOX);
		spy = jest.spyOn(RulesAction.prototype, 'execute').mockImplementation((input) => {
			receivedActionInput = input;
		});
	});

	afterEach(() => {
		jest.restoreAllMocks();
	});

	describe('--rule-selector', () => {
		it('Can be supplied once with a single value', async () => {
			const inputValue = 'abcde';
			await RulesCommand.run(['--rule-selector', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', [inputValue]);
		});

		it('Can be supplied once with multiple comma-separated values', async () => {
			const inputValue = ['abcde', 'defgh'];
			await RulesCommand.run(['--rule-selector', inputValue.join(',')]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', inputValue);
		});

		it('Can be supplied multiple times with one value each', async () => {
			const inputValue1 = 'abcde';
			const inputValue2 = 'defgh';
			await RulesCommand.run(['--rule-selector', inputValue1, '--rule-selector', inputValue2]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', [inputValue1, inputValue2]);
		});

		it('Can be supplied multiple times with multiple comma-separated values each', async () => {
			const inputValue1 = ['abcde', 'hijlk'];
			const inputValue2 = ['defgh', 'mnopq'];
			await RulesCommand.run(['--rule-selector', inputValue1.join(','), '--rule-selector', inputValue2.join(',')]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', [...inputValue1, ...inputValue2]);
		});

		it('Defaults to value of "recommended"', async () => {
			await RulesCommand.run([]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', ["recommended"]);
		})

		it('Can be referenced by its shortname, -r', async () => {
			const inputValue = 'abcde';
			await RulesCommand.run(['-r', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', [inputValue]);
		});
	});

	describe('--config-file', () => {
		it('Accepts a real file', async () => {
			const inputValue = 'package.json';
			await RulesCommand.run(['--config-file', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('config-file', inputValue);
		});

		it('Rejects non-existent file', async () => {
			const inputValue = 'definitelyFakeFile.json';
			let errorThrown: boolean;
			let message: string = '';
			try {
				await RulesCommand.run(['--config-file', inputValue]);
				errorThrown = false;
			} catch (e) {
				errorThrown = true;
				message = e.message;
			}
			expect(errorThrown).toBe(true);
			expect(message).toContain(`No file found at ${inputValue}`);
			expect(spy).not.toHaveBeenCalled();
		});

		it('Can only be supplied once', async () => {
			const inputValue1 = 'package.json';
			const inputValue2 = 'LICENSE';
			let errorThrown: boolean;
			let message: string = '';
			try {
				await RulesCommand.run(['--config-file', inputValue1, '--config-file', inputValue2]);
				errorThrown = false;
			} catch (e) {
				errorThrown = true;
				message = e.message;
			}
			expect(errorThrown).toBe(true);
			expect(message).toContain(`Flag --config-file can only be specified once`);
			expect(spy).not.toHaveBeenCalled();
		});

		it('Can be referenced by its shortname, -c', async () => {
			const inputValue = 'package.json';
			await RulesCommand.run(['-c', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('config-file', inputValue);
		});
	});

	describe('--view', () => {
		it('Accepts the value, "table"', async () => {
			const inputValue = 'table';
			await RulesCommand.run(['--view', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('view', inputValue);
		});

		it('Accepts the value, "detail"', async () => {
			const inputValue = 'detail';
			await RulesCommand.run(['--view', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('view', inputValue);
		});

		it('Rejects all other values', async () => {
			const inputValue = 'beep';
			let errorThrown: boolean;
			let message: string = '';
			try {
				await RulesCommand.run(['--view', inputValue]);
				errorThrown = false;
			} catch (e) {
				errorThrown = true;
				message = e.message;
			}
			expect(errorThrown).toBe(true);
			expect(message).toContain(`Expected --view=${inputValue} to be one of:`);
			expect(spy).not.toHaveBeenCalled();
		});

		it('Can be supplied only once', async () => {
			const inputValue1 = 'detail';
			const inputValue2 = 'table';
			let errorThrown: boolean;
			let message: string = '';
			try {
				await RulesCommand.run(['--view', inputValue1, '--view', inputValue2]);
				errorThrown = false;
			} catch (e) {
				errorThrown = true;
				message = e.message;
			}
			expect(errorThrown).toBe(true);
			expect(message).toContain(`Flag --view can only be specified once`);
			expect(spy).not.toHaveBeenCalled();
		});

		it('Can be referenced by its shortname, -v', async () => {
			const inputValue = 'table';
			await RulesCommand.run(['-v', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('view', inputValue);
		});
	});
});
