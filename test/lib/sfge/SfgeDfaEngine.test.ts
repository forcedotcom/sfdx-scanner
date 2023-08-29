import 'reflect-metadata';
import * as TestOverrides from '../../test-related-lib/TestOverrides';

import Sinon = require('sinon');
import path = require('path');
import fs = require('fs');
import {assert, expect} from 'chai';

import {RuleResult} from '../../../src/types';
import {SfgeDfaEngine} from '../../../src/lib/sfge/SfgeDfaEngine';

TestOverrides.initializeTestSetup();

class TestableSfgeEngine extends SfgeDfaEngine {
	public parseViolations(output: string): RuleResult[] {
		return super.parseViolations(output);
	}

	public parseError(output: string): string {
		return TestableSfgeEngine.parseError(output);
	}

	public processExecutionFailure(rawErrorMessage: string): Promise<RuleResult[]> {
		return super.processExecutionFailure(rawErrorMessage);
	}
}

describe('SfgeDfaEngine', () => {
	describe('#parseViolations()', () => {
		it('When output contains violations, they are converted into RuleResult objects', async () => {
			// ==== SETUP ====
			const testEngine = new TestableSfgeEngine();
			await testEngine.init();
			const spoofedOutput = fs.readFileSync(path.join('test', 'code-fixtures', 'sfge-results', 'one_violation.txt')).toString();

			// ==== TESTED METHOD ====
			const results = testEngine.parseViolations(spoofedOutput);

			// ==== ASSERTIONS ====
			expect(results.length).to.equal(1, 'Should be results');
		});

		it('When output contains no violations, results are empty', async () => {
			// ==== SETUP ====
			const testEngine = new TestableSfgeEngine();
			await testEngine.init();
			const spoofedOutput = fs.readFileSync(path.join('test', 'code-fixtures', 'sfge-results', 'no_violations.txt')).toString();

			// ==== TESTED METHOD ====
			const results = testEngine.parseViolations(spoofedOutput);

			// ==== ASSERTIONS ====
			expect(results.length).to.equal(0, 'No results should have been returned');
		});
	});

	describe('#parseError', () => {
		it('Returns delineated error message if available', async () => {
			// ==== SETUP ====
			const testEngine = new TestableSfgeEngine();
			await testEngine.init();
			const spoofedOutput = fs.readFileSync(path.join('test', 'code-fixtures', 'sfge-results', 'delineated_error_without_partial_results.txt')).toString();
			// ASSUMPTION: Line 21 of the spoofed output is where the delineation is.
			const expectedError = spoofedOutput.split('\n').slice(21).join('\n');

			// ==== TESTED METHOD ====
			const errorMessage = testEngine.parseError(spoofedOutput);

			// ==== ASSERTIONS ====
			expect(errorMessage).to.equal(expectedError, 'Wrong error found');
		});

		it('Returns entire message when no delineated error message is available', async () => {
			// ==== SETUP ====
			const testEngine = new TestableSfgeEngine();
			await testEngine.init();
			const spoofedOutput = fs.readFileSync(path.join('test', 'code-fixtures', 'sfge-results', 'undelineated_error.txt')).toString();

			// ==== TESTED METHOD ====
			const errorMessage = testEngine.parseError(spoofedOutput);

			// ==== ASSERTIONS ====
			expect(errorMessage).to.equal(spoofedOutput, 'Wrong error found');
		});
	});

	describe('#processExecutionFailure', () => {
		beforeEach(() => {
			Sinon.createSandbox();
		})

		afterEach(() => {
			Sinon.restore();
		});

		it('If partial results are present, they are returned and errors are logged', async () => {
			// ==== SETUP ====
			const testEngine = new TestableSfgeEngine();
			await testEngine.init();
			const spoofedOutput = fs.readFileSync(path.join('test', 'code-fixtures', 'sfge-results', 'delineated_error_with_partial_results.txt')).toString();
			const uxSpy = Sinon.spy((testEngine as any).eventCreator, 'createUxErrorMessage');

			// ==== TESTED METHOD ====
			const results = await testEngine.processExecutionFailure(spoofedOutput);

			// ==== ASSERTIONS ====
			Sinon.assert.callCount(uxSpy, 1);
			expect(results).to.have.lengthOf(1, 'Wrong result count');
		});

		it('If no partial results are present, error is rethrown', async () => {
			// ==== SETUP ====
			const testEngine = new TestableSfgeEngine();
			await testEngine.init();
			const spoofedOutput = fs.readFileSync(path.join('test', 'code-fixtures', 'sfge-results', 'delineated_error_without_partial_results.txt')).toString();
			const uxSpy = Sinon.spy((testEngine as any).eventCreator, 'createUxErrorMessage');
			// ASSUMPTION: Line 21 of the spoofed output is where the delineation is.
			const expectedError = spoofedOutput.split('\n').slice(21).join('\n');

			// ==== TESTED METHOD ====
			let err: string = null;
			try {
				const results = await testEngine.processExecutionFailure(spoofedOutput);
				assert.fail(`Expected error, received ${JSON.stringify(results)}`);
			} catch (e) {
				err = e instanceof Error ? e.message : e as string;
			}

			// ==== ASSERTIONS ====
			expect(err).to.equal(expectedError, 'Wrong error returned');
			Sinon.assert.callCount(uxSpy, 0);
		});
	});


});
