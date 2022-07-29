import 'reflect-metadata';
import {RuleResult} from '../../../src/types';
import path = require('path');
import fs = require('fs');
import {expect} from 'chai';
import {SfgeDfaEngine} from '../../../src/lib/sfge/SfgeDfaEngine';
import * as TestOverrides from '../../test-related-lib/TestOverrides';

TestOverrides.initializeTestSetup();

class TestableSfgeDfaEngine extends SfgeDfaEngine {
	public processStdout(output: string): RuleResult[] {
		return super.processStdout(output);
	}
}

describe('SfgeDfaEngine', () => {
	describe('#processStdout()', () => {
		it('When Sfge finds violations, they are converted into RuleResult objects', async () => {
			// ==== SETUP ====
			const testEngine = new TestableSfgeDfaEngine();
			await testEngine.init();
			const spoofedOutput = fs.readFileSync(path.join('test', 'code-fixtures', 'sfge-results', 'one_violation.txt')).toString();

			// ==== TESTED METHOD ====
			const results = testEngine.processStdout(spoofedOutput);

			// ==== ASSERTIONS ====
			expect(results.length).to.equal(1, 'Should be results');
		});

		it('When sfge finds no violations, results are empty', async () => {
			// ==== SETUP ====
			const testEngine = new TestableSfgeDfaEngine();
			await testEngine.init();
			const spoofedOutput = fs.readFileSync(path.join('test', 'code-fixtures', 'sfge-results', 'no_violations.txt')).toString();

			// ==== TESTED METHOD ====
			const results = testEngine.processStdout(spoofedOutput);

			// ==== ASSERTIONS ====
			expect(results.length).to.equal(0, 'No results should have been returned');
		});
	});
});
