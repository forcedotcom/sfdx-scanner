import {stubSfCommandUx} from '@salesforce/sf-plugins-core';
import {TestContext} from '@salesforce/core/lib/testSetup';
import InitCommand from '../../../src/commands/code-analyzer/init';
import {InitAction} from '../../../src/lib/actions/InitAction';

describe('`code-analyzer init` tests', () => {
	const $$ = new TestContext();
	let spy: jest.SpyInstance;
	let receivedActionInput: object;

	beforeEach(() => {
		stubSfCommandUx($$.SANDBOX);
		spy = jest.spyOn(InitAction.prototype, 'execute').mockImplementation((input) => {
			receivedActionInput = input;
		});
	});

	afterEach(() => {
		jest.restoreAllMocks();
	})

	describe('--template', () => {
		it('Accepts the value, "empty"', async () => {
			const inputValue = "empty";
			await InitCommand.run(['--template', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('template', inputValue);
		});

		it('Accepts the value, "default"', async () => {
			const inputValue = "default";
			await InitCommand.run(['--template', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('template', inputValue);
		});

		it('Defaults to a value of "empty"', async () => {
			await InitCommand.run([]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('template', 'empty');
		});

		it('Rejects all other values', async () => {
			const inputValue = "asdf";
			const executionPromise = InitCommand.run(['--template', inputValue]);
			await expect(executionPromise).rejects.toThrow(`Expected --template=${inputValue} to be one of: empty, default`);
			expect(spy).not.toHaveBeenCalled();
		});

		it('Can be supplied only once', async () => {
			const inputValue1 = 'empty';
			const inputValue2 = 'default';
			const executionPromise = InitCommand.run(['--template', inputValue1, '--template', inputValue2]);
			await expect(executionPromise).rejects.toThrow(`Flag --template can only be specified once`);
			expect(spy).not.toHaveBeenCalled();
		});

		it('Can be referenced by its shortname, -t', async () => {
			const inputValue = "empty";
			await InitCommand.run(['-t', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('template', inputValue);
		});
	});
});

