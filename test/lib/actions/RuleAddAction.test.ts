import {Logger} from '@salesforce/core';
import {assert, expect} from 'chai';
import sinon = require('sinon');
import path = require('path');
import untildify = require('untildify');

import {Inputs} from '../../../src/types';
import {InputProcessor, InputProcessorImpl} from '../../../src/lib/InputProcessor';
import {RuleAddAction} from '../../../src/lib/actions/RuleAddAction';
import {BundleName, getMessage} from '../../../src/MessageCatalog';
import {RulePathManager} from '../../../src/lib/RulePathManager';
import {Controller} from '../../../src/Controller';

import {FakeDisplay} from '../FakeDisplay';

class FakeRulePathManager implements RulePathManager {
	public language: string;
	public paths: string[];

	public init(): Promise<void> {
		return Promise.resolve();
	}

	/**
	 * For now, only this method needs a non-empty implementation.
	 * It just tracks what arguments it's fed.
	 */
	public addPathsForLanguage(language: string, paths: string[]): Promise<string[]> {
		this.language = language;
		this.paths = paths;
		return Promise.resolve(paths);
	}

	public getAllPaths(): string[] {
		return [];
	}

	public getMatchingPaths(paths: string[]): Promise<string[]> {
		return Promise.resolve([]);
	}

	public removePaths(paths: string[]): Promise<string[]> {
		return Promise.resolve([]);
	}

	public getRulePathEntries(engine: string): Map<string, Set<string>> {
		return new Map();
	}
}

describe('RuleAddAction', () => {
	let testLogger: Logger;
	let testDisplay: FakeDisplay;
	let testInputProcessor: InputProcessor;
	let testAction: RuleAddAction;

	beforeEach(async () => {
		testLogger = await Logger.child('RuleAddAction.test.ts');
		testDisplay = new FakeDisplay();
		testInputProcessor = new InputProcessorImpl('test', testDisplay);
		testAction = new RuleAddAction(testLogger, testDisplay, testInputProcessor);
	});

	describe('#validateInputs', () => {

		it('Rejects missing `.language` property', async () => {
			const inputs: Inputs = {
				path: ['this/does/not/matter']
			};

			try {
				await testAction.validateInputs(inputs);
				assert.fail('Exception should have been thrown');
			} catch (e) {
				expect(e.message).to.equal(getMessage(BundleName.Add, 'validations.languageCannotBeEmpty', []));
			}
		});

		it('Rejects empty `.language` property', async () => {
			const inputs: Inputs = {
				language: '',
				path: ['this/does/not/matter']
			};

			try {
				await testAction.validateInputs(inputs);
				assert.fail('Exception should have been thrown');
			} catch (e) {
				expect(e.message).to.equal(getMessage(BundleName.Add, 'validations.languageCannotBeEmpty', []));
			}
		});

		it('Rejects missing `.path` property', async () => {
			const inputs: Inputs = {
				language: 'apex'
			};

			try {
				await testAction.validateInputs(inputs);
				assert.fail('Exception should have been thrown');
			} catch (e) {
				expect(e.message).to.equal(getMessage(BundleName.Add, 'validations.pathCannotBeEmpty', []));
			}
		});

		it('Rejects empty `.path` property', async () => {
			const inputs: Inputs = {
				language: 'apex',
				path: []
			};

			try {
				await testAction.validateInputs(inputs);
				assert.fail('Exception should have been thrown');
			} catch (e) {
				expect(e.message).to.equal(getMessage(BundleName.Add, 'validations.pathCannotBeEmpty', []));
			}
		});

		it('Rejects `.path` containing empty string', async () => {
			const inputs: Inputs = {
				language: 'apex',
				path: [""]
			}

			try {
				await testAction.validateInputs(inputs);
				assert.fail('Exception should have been thrown');
			} catch (e) {
				expect(e.message).to.equal(getMessage(BundleName.Add, 'validations.pathCannotBeEmpty', []));
			}
		});
	});

	describe('#run', () => {
		let fakeRulePathManager: FakeRulePathManager;
		let controllerStub;
		const language = 'apex';
		const inputPaths = [
			path.join('~', 'this', 'path', 'is', 'tildified'),
			path.resolve('.', 'this', 'path', 'is', 'absolute'),
			path.join('.', 'this', 'path', 'is', 'relative')
		];
		const expectedOutputPaths = [
			untildify(inputPaths[0]),
			inputPaths[1],
			path.resolve(inputPaths[2])
		];
		const inputs: Inputs = {
			language,
			path: inputPaths
		};

		beforeEach(() => {
			fakeRulePathManager = new FakeRulePathManager();
			controllerStub = sinon.stub(Controller, 'createRulePathManager');
			controllerStub.resolves(fakeRulePathManager);

		});

		afterEach(() => {
			sinon.restore();
		});

		it('Passes un-tildified paths into CustomRulePathManager', async () => {
			// ==== TESTED METHOD INVOCATION ====
			await testAction.run(inputs);

			// ==== ASSERTIONS ====
			expect(fakeRulePathManager.language).to.equal(language);
			expect(fakeRulePathManager.paths).to.have.lengthOf(3);
			expect(fakeRulePathManager.paths[0]).to.equal(expectedOutputPaths[0], 'Should receive untildified tilde path');
			expect(fakeRulePathManager.paths[1]).to.equal(expectedOutputPaths[1], 'Should receive unchanged absolute path');
			expect(fakeRulePathManager.paths[2]).to.equal(expectedOutputPaths[2], 'Should absolutized relative path');
		});

		it('Displays information about added paths', async () => {
			// ==== TESTED METHOD INVOCATION ====
			await testAction.run(inputs);

			// ==== ASSERTIONS ====
			expect(testDisplay.getOutputArray()).to.contain(`[Info]: ${getMessage(BundleName.Add, 'output.successfullyAddedRules', [language])}`);
			expect(testDisplay.getOutputArray()).to.contain(`[Info]: ${getMessage(BundleName.Add, 'output.resultSummary', [3, expectedOutputPaths.toString()])}`);
		});
	});
});
