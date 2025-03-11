import { TestContext } from '@salesforce/core/lib/testSetup';
import { stubSfCommandUx } from '@salesforce/sf-plugins-core';
import RulesCommand from '../../../src/commands/code-analyzer/rules';
import { RulesAction, RulesDependencies, RulesInput } from '../../../src/lib/actions/RulesAction';

describe('`code-analyzer rules` tests', () => {
	const $$ = new TestContext();
	
	let executeSpy: jest.SpyInstance;
	let createActionSpy: jest.SpyInstance;
	let receivedActionInput: RulesInput;
	let receivedActionDependencies: RulesDependencies;

	beforeEach(() => {
		stubSfCommandUx($$.SANDBOX);
		executeSpy = jest.spyOn(RulesAction.prototype, 'execute').mockImplementation((input) => {
			receivedActionInput = input;
			return Promise.resolve();
		});
		const originalCreateAction = RulesAction.createAction;
		createActionSpy = jest.spyOn(RulesAction, 'createAction').mockImplementation((dependencies) => {
			receivedActionDependencies = dependencies;
			return originalCreateAction(dependencies);
		});
	});
	
	afterEach(() => {
		jest.restoreAllMocks();
	});
	
	describe('--rule-selector', () => {
		it('Can be supplied once with a single value', async () => {
			const inputValue = 'abcde';
			await RulesCommand.run(['--rule-selector', inputValue]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', [inputValue]);
		});
		
		it('Can be supplied once with multiple comma-separated values', async () => {
			const inputValue = ['abcde', 'defgh'];
			await RulesCommand.run(['--rule-selector', inputValue.join(',')]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', inputValue);
		});
		
		it('Can be supplied multiple times with one value each', async () => {
			const inputValue1 = 'abcde';
			const inputValue2 = 'defgh';
			await RulesCommand.run(['--rule-selector', inputValue1, '--rule-selector', inputValue2]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', [inputValue1, inputValue2]);
		});
		
		it('Can be supplied multiple times with multiple comma-separated values each', async () => {
			const inputValue1 = ['abcde', 'hijlk'];
			const inputValue2 = ['defgh', 'mnopq'];
			await RulesCommand.run(['--rule-selector', inputValue1.join(','), '--rule-selector', inputValue2.join(',')]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', [...inputValue1, ...inputValue2]);
		});
		
		it('Defaults to value of "Recommended"', async () => {
			await RulesCommand.run([]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', ["Recommended"]);
		})
		
		it('Can be referenced by its shortname, -r', async () => {
			const inputValue = 'abcde';
			await RulesCommand.run(['-r', inputValue]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', [inputValue]);
		});
	});
	
	describe('--config-file', () => {
		it('Accepts a real file', async () => {
			const inputValue = 'package.json';
			await RulesCommand.run(['--config-file', inputValue]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('config-file', inputValue);
		});
		
		it('Rejects non-existent file', async () => {
			const inputValue = 'definitelyFakeFile.json';
			const executionPromise = RulesCommand.run(['--config-file', inputValue]);
			await expect(executionPromise).rejects.toThrow(`No file found at ${inputValue}`);
			expect(executeSpy).not.toHaveBeenCalled();
		});
		
		it('Can only be supplied once', async () => {
			const inputValue1 = 'package.json';
			const inputValue2 = 'LICENSE';
			const executionPromise = RulesCommand.run(['--config-file', inputValue1, '--config-file', inputValue2]);
			await expect(executionPromise).rejects.toThrow(`Flag --config-file can only be specified once`);
			expect(executeSpy).not.toHaveBeenCalled();
		});
		
		it('Can be referenced by its shortname, -c', async () => {
			const inputValue = 'package.json';
			await RulesCommand.run(['-c', inputValue]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('config-file', inputValue);
		});
	});
	
	describe('--output-file', () => {
		it('Accepts one file path', async () => {
			const inputValue = 'my/rules-output.json';
			await RulesCommand.run(['--output-file', inputValue]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('output-file', inputValue);
			expect(receivedActionDependencies.writer).toBeDefined();
		});
		
		it('Can only be supplied once', async () => {
			const inputValue1 = 'rules-output1.json';
			const inputValue2 = 'rules-output1.json';
			const executionPromise = RulesCommand.run(['--output-file', inputValue1, '--output-file', inputValue2]);
			await expect(executionPromise).rejects.toThrow(`Flag --output-file can only be specified once`);
			expect(executeSpy).not.toHaveBeenCalled();
		});
		
		it('Can be referenced by its shortname, -f', async () => {
			const inputValue = 'rules-output.json';
			await RulesCommand.run(['-f', inputValue]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('output-file', inputValue);
		});

		it('Is optional', async () => {
			await RulesCommand.run([]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionDependencies.writer).toBeUndefined();
		});
	});
	
	describe('--view', () => {
		it('Accepts the value, "table"', async () => {
			const inputValue = 'table';
			await RulesCommand.run(['--view', inputValue]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('view', inputValue);
			expect(createActionSpy).toHaveBeenCalled();
			expect(receivedActionDependencies.viewer.constructor.name).toEqual('RuleTableDisplayer');
		});
		
		it('Accepts the value, "detail"', async () => {
			const inputValue = 'detail';
			await RulesCommand.run(['--view', inputValue]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('view', inputValue);
			expect(createActionSpy).toHaveBeenCalled();
			expect(receivedActionDependencies.viewer.constructor.name).toEqual('RuleDetailDisplayer');
		});
		
		it('Rejects all other values', async () => {
			const inputValue = 'beep';
			const executionPromise = RulesCommand.run(['--view', inputValue]);
			await expect(executionPromise).rejects.toThrow(`Expected --view=${inputValue} to be one of:`);
			expect(executeSpy).not.toHaveBeenCalled();
		});
		
		it('Defaults to value of "table"', async () => {
			await RulesCommand.run([]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('view', 'table');
			expect(createActionSpy).toHaveBeenCalled();
			expect(receivedActionDependencies.viewer.constructor.name).toEqual('RuleTableDisplayer');
		});
		
		it('Can be supplied only once', async () => {
			const inputValue1 = 'detail';
			const inputValue2 = 'table';
			const executionPromise = RulesCommand.run(['--view', inputValue1, '--view', inputValue2]);
			await expect(executionPromise).rejects.toThrow(`Flag --view can only be specified once`);
			expect(executeSpy).not.toHaveBeenCalled();
		});
		
		it('Can be referenced by its shortname, -v', async () => {
			// Use a non-default value, so we know that the flag's value comes from our input and not the default.
			const inputValue = 'detail';
			await RulesCommand.run(['-v', inputValue]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('view', inputValue);
			expect(createActionSpy).toHaveBeenCalled();
			expect(receivedActionDependencies.viewer.constructor.name).toEqual('RuleDetailDisplayer');
		});
	});
	
	describe('--workspace', () => {
		it('Can be supplied once with a single value', async () => {
			const inputValue = './somedirectory';
			await RulesCommand.run(['--workspace', inputValue]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('workspace', [inputValue]);
		});
		
		it('Can be supplied once with multiple comma-separated values', async () => {
			const inputValue =['./somedirectory', './someotherdirectory'];
			await RulesCommand.run(['--workspace', inputValue.join(',')]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('workspace', inputValue);
		});
		
		it('Can be supplied multiple times with one value each', async () => {
			const inputValue1 = './somedirectory';
			const inputValue2 = './someotherdirectory';
			await RulesCommand.run(['--workspace', inputValue1, '--workspace', inputValue2]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('workspace', [inputValue1, inputValue2]);
		});
		
		it('Can be supplied multiple times with multiple comma-separated values', async () => {
			const inputValue1 = ['./somedirectory', './anotherdirectory'];
			const inputValue2 = ['./someotherdirectory', './yetanotherdirectory'];
			await RulesCommand.run(['--workspace', inputValue1.join(','), '--workspace', inputValue2.join(',')]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('workspace', [...inputValue1, ...inputValue2]);
		});
		
		it('Is unused if not directly specified', async () => {
			await RulesCommand.run([]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput.workspace).toBeUndefined();
		});
		
		it('Can be referenced by its shortname, -w', async () => {
			const inputValue = './somedirectory';
			await RulesCommand.run(['-w', inputValue]);
			expect(executeSpy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('workspace', [inputValue]);
		});
	});
	
	describe('Flag interactions', () => {
		describe('--output-file and --view', () => {
			it('When --output-file and --view are both present, both are used', async () => {
				const outfileInput = 'rules-output.json';
				const viewInput = 'detail';
				await RulesCommand.run(['--output-file', outfileInput, '--view', viewInput]);
				expect(executeSpy).toHaveBeenCalled();
				expect(createActionSpy).toHaveBeenCalled();
				expect(receivedActionDependencies.writer).toBeDefined();
				expect(receivedActionDependencies.viewer.constructor.name).toEqual('RuleDetailDisplayer');
			});
			
			it('When --output-file is present and --view is not, --view defaults to "table"', async () => {
				const outfileInput= 'rules-output.json';
				await RulesCommand.run(['--output-file', outfileInput]);
				expect(executeSpy).toHaveBeenCalled();
				expect(createActionSpy).toHaveBeenCalled();
				expect(receivedActionDependencies.writer).toBeDefined();
				expect(receivedActionDependencies.viewer.constructor.name).toEqual('RuleTableDisplayer');
			});
			
			it('When --output-file and --view are both absent, writer is not set and --view defaults to "table"', async () => {
				await RulesCommand.run([]);
				expect(createActionSpy).toHaveBeenCalled();
				expect(receivedActionDependencies.writer).toBeUndefined();
				expect(receivedActionDependencies.viewer.constructor.name).toEqual('RuleTableDisplayer');
			});
		});
	});
});
