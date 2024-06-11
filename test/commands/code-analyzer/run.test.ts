import {stubSfCommandUx} from '@salesforce/sf-plugins-core';
import {TestContext} from '@salesforce/core/lib/testSetup';
import RunCommand from '../../../src/commands/code-analyzer/run';
import {RunAction} from '../../../src/lib/actions/RunAction';

describe('`code-analyzer run` tests', () => {
	const $$ = new TestContext();
	let spy: jest.SpyInstance;
	let receivedActionInput: object;
	beforeEach(() => {
		stubSfCommandUx($$.SANDBOX);
		spy = jest.spyOn(RunAction.prototype, 'execute').mockImplementation((input) => {
			receivedActionInput = input;
		});
	});

	afterEach(() => {
		jest.restoreAllMocks();
	});

	describe('--workspace', () => {
		it('Can be supplied once with a single value', async () => {
			const inputValue = './somedirectory';
			await RunCommand.run(['--workspace', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('workspace', [inputValue]);
		});

		it('Can be supplied once with multiple comma-separated values', async () => {
			const inputValue =['./somedirectory', './someotherdirectory'];
			await RunCommand.run(['--workspace', inputValue.join(',')]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('workspace', inputValue);
		});

		it('Can be supplied multiple times with one value each', async () => {
			const inputValue1 = './somedirectory';
			const inputValue2 = './someotherdirectory';
			await RunCommand.run(['--workspace', inputValue1, '--workspace', inputValue2]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('workspace', [inputValue1, inputValue2]);
		});

		it('Can be supplied multiple times with multiple comma-separated values', async () => {
			const inputValue1 = ['./somedirectory', './anotherdirectory'];
			const inputValue2 = ['./someotherdirectory', './yetanotherdirectory'];
			await RunCommand.run(['--workspace', inputValue1.join(','), '--workspace', inputValue2.join(',')]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('workspace', [...inputValue1, ...inputValue2]);
		});

		it('Defaults to value of "."', async () => {
			await RunCommand.run([]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('workspace', ['.']);
		});

		it('Can be referenced by its shortname, -w', async () => {
			const inputValue = './somedirectory';
			await RunCommand.run(['-w', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('workspace', [inputValue]);
		});
	});

	describe('--path-start', () => {
		it('Can be supplied once with a single value', async () => {
			const inputValue = './somefile.cls';
			await RunCommand.run(['--path-start', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('path-start', [inputValue]);
		});

		it('Can be supplied once with multiple comma-separated values', async () => {
			const inputValue =['./somefile.cls', './someotherfile.cls'];
			await RunCommand.run(['--path-start', inputValue.join(',')]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('path-start', inputValue);
		});

		it('Can be supplied multiple times with one value each', async () => {
			const inputValue1 = './somefile.cls';
			const inputValue2 = './someotherfile.cls';
			await RunCommand.run(['--path-start', inputValue1, '--path-start', inputValue2]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('path-start', [inputValue1, inputValue2]);
		});

		it('Can be supplied multiple times with multiple comma-separated values', async () => {
			const inputValue1 = ['./somefile.cls', './anotherfile.cls'];
			const inputValue2 = ['./someotherfile.cls', './yetanotherfile.cls'];
			await RunCommand.run(['--path-start', inputValue1.join(','), '--path-start', inputValue2.join(',')]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('path-start', [...inputValue1, ...inputValue2]);
		});

		it('Can be referenced by its shortname, -s', async () => {
			const inputValue = './somefile.cls';
			await RunCommand.run(['-s', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('path-start', [inputValue]);
		});
	});

	describe('--rule-selector', () => {
		it('Can be supplied once with a single value', async () => {
			const inputValue = 'abcde';
			await RunCommand.run(['--rule-selector', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', [inputValue]);
		});

		it('Can be supplied once with multiple comma-separated values', async () => {
			const inputValue = ['abcde', 'defgh'];
			await RunCommand.run(['--rule-selector', inputValue.join(',')]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', inputValue);
		});

		it('Can be supplied multiple times with one value each', async () => {
			const inputValue1 = 'abcde';
			const inputValue2 = 'defgh';
			await RunCommand.run(['--rule-selector', inputValue1, '--rule-selector', inputValue2]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', [inputValue1, inputValue2]);
		});

		it('Can be supplied multiple times with multiple comma-separated values each', async () => {
			const inputValue1 = ['abcde', 'hijlk'];
			const inputValue2 = ['defgh', 'mnopq'];
			await RunCommand.run(['--rule-selector', inputValue1.join(','), '--rule-selector', inputValue2.join(',')]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', [...inputValue1, ...inputValue2]);
		});

		it('Defaults to value of "Recommended"', async () => {
			await RunCommand.run([]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', ["Recommended"]);
		});

		it('Can be referenced by its shortname, -r', async () => {
			const inputValue = 'abcde';
			await RunCommand.run(['-r', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('rule-selector', [inputValue]);
		});
	});

	describe('--severity-threshold', () => {
		for (let i = 1; i <= 5; i++) {
			it(`Accepts integers between 1 and 5 (inclusive); i = ${i}`, async () => {
				const inputValue = `${i}`
				await RunCommand.run(['--severity-threshold', inputValue]);
				expect(spy).toHaveBeenCalled();
				expect(receivedActionInput).toHaveProperty('severity-threshold', inputValue);
			});
		}

		it('Rejects integer below 1', async () => {
			const inputValue = '0';
			const executionPromise = RunCommand.run(['--severity-threshold', inputValue]);
			await expect(executionPromise).rejects.toThrow(`Expected --severity-threshold=${inputValue} to be one of:`);
			expect(spy).not.toHaveBeenCalled();
		});

		it('Rejects integer above 5', async () => {
			const inputValue = '7';
			const executionPromise = RunCommand.run(['--severity-threshold', inputValue]);
			await expect(executionPromise).rejects.toThrow(`Expected --severity-threshold=${inputValue} to be one of:`);
			expect(spy).not.toHaveBeenCalled();
		});

		const severities = ["critical", "high", "moderate", "low", "info"];
		for (let sev of severities) {
			it(`Accepts severity level names; name = ${sev}`, async () => {
				await RunCommand.run(['--severity-threshold', sev]);
				expect(spy).toHaveBeenCalled();
				expect(receivedActionInput).toHaveProperty('severity-threshold', sev);
			});
		}

		it('Rejects unknown severity name', async () => {
			const inputValue = 'beep';
			const executionPromise = RunCommand.run(['--severity-threshold', inputValue]);
			await expect(executionPromise).rejects.toThrow(`Expected --severity-threshold=${inputValue} to be one of:`);
			expect(spy).not.toHaveBeenCalled();
		});

		it('Can only be supplied once', async () => {
			const inputValue1 = 'critical';
			const inputValue2 = 'high';
			const executionPromise = RunCommand.run(['--severity-threshold', inputValue1, '--severity-threshold', inputValue2]);
			await expect(executionPromise).rejects.toThrow(`Flag --severity-threshold can only be specified once`);
			expect(spy).not.toHaveBeenCalled();
		});

		it('Is unused if not directly specified', async () => {
			await RunCommand.run([]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).not.toHaveProperty('severity-threshold');
		});

		it('Can be referenced by its shortname, -t', async () => {
			const inputValue = `3`
			await RunCommand.run(['-t', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('severity-threshold', inputValue);
		});
	});

	describe('--config-file', () => {
		it('Accepts a real file', async () => {
			const inputValue = 'package.json';
			await RunCommand.run(['--config-file', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('config-file', inputValue);
		});

		it('Rejects non-existent file', async () => {
			const inputValue = 'definitelyFakeFile.json';
			const executionPromise = RunCommand.run(['--config-file', inputValue]);
			await expect(executionPromise).rejects.toThrow(`No file found at ${inputValue}`);
			expect(spy).not.toHaveBeenCalled();
		});

		it('Can only be supplied once', async () => {
			const inputValue1 = 'package.json';
			const inputValue2 = 'LICENSE';
			const executionPromise = RunCommand.run(['--config-file', inputValue1, '--config-file', inputValue2]);
			await expect(executionPromise).rejects.toThrow(`Flag --config-file can only be specified once`);
			expect(spy).not.toHaveBeenCalled();
		});

		it('Can be referenced by its shortname, -c', async () => {
			const inputValue = 'package.json';
			await RunCommand.run(['-c', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('config-file', inputValue);
		});
	});

	describe('--output-file', () => {
		it('Can be supplied once with a single value', async () => {
			const inputValue = './somefile.json';
			await RunCommand.run(['--output-file', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('output-file', [inputValue]);
		});

		it('Can be supplied once with multiple comma-separated values', async () => {
			const inputValue =['./somefile.json', './someotherfile.xml'];
			await RunCommand.run(['--output-file', inputValue.join(',')]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('output-file', inputValue);
		});

		it('Can be supplied multiple times with one value each', async () => {
			const inputValue1 = './somefile.json';
			const inputValue2 = './someotherfile.xml';
			await RunCommand.run(['--output-file', inputValue1, '--output-file', inputValue2]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('output-file', [inputValue1, inputValue2]);
		});

		it('Can be supplied multiple times with multiple comma-separated values', async () => {
			const inputValue1 = ['./somefile.json', './someotherfile.xml'];
			const inputValue2 = ['./athirdfile.html', './afourthfile.sarif'];
			await RunCommand.run(['--output-file', inputValue1.join(','), '--output-file', inputValue2.join(',')]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('output-file', [...inputValue1, ...inputValue2]);
		});

		it('Can be referenced by its shortname, -f', async () => {
			const inputValue = './somefile.json';
			await RunCommand.run(['-f', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('output-file', [inputValue]);
		});
	});

	describe('--view', () => {
		it('Accepts the value, "table"', async () => {
			const inputValue = 'table';
			await RunCommand.run(['--view', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('view', inputValue);
		});

		it('Accepts the value, "detail"', async () => {
			const inputValue = 'detail';
			await RunCommand.run(['--view', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('view', inputValue);
		});

		it('Rejects all other values', async () => {
			const inputValue = 'beep';
			const executionPromise = RunCommand.run(['--view', inputValue]);
			await expect(executionPromise).rejects.toThrow(`Expected --view=${inputValue} to be one of:`);
			expect(spy).not.toHaveBeenCalled();
		});

		it('Defaults to value of "table"', async () => {
			await RunCommand.run([]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('view', 'table');
		});

		it('Can be supplied only once', async () => {
			const inputValue1 = 'detail';
			const inputValue2 = 'table';
			const executionPromise = RunCommand.run(['--view', inputValue1, '--view', inputValue2]);
			await expect(executionPromise).rejects.toThrow(`Flag --view can only be specified once`);
			expect(spy).not.toHaveBeenCalled();
		});

		it('Can be referenced by its shortname, -v', async () => {
			const inputValue = 'table';
			await RunCommand.run(['-v', inputValue]);
			expect(spy).toHaveBeenCalled();
			expect(receivedActionInput).toHaveProperty('view', inputValue);
		});
	});
});

