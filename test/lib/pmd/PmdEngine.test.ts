import 'reflect-metadata';
import {FileHandler} from '../../../src/lib/util/FileHandler';
import {RuleResult} from '../../../src/types';
import path = require('path');
import {expect} from 'chai';
import Sinon = require('sinon');
import {PmdEngine}  from '../../../src/lib/pmd/PmdEngine'
import {uxEvents} from '../../../src/lib/ScannerEvents';
import * as TestOverrides from '../../test-related-lib/TestOverrides';

TestOverrides.initializeTestSetup();
class TestPmdEngine extends PmdEngine {
	public processStdOut(stdout: string): RuleResult[] {
		return super.processStdOut(stdout);
	}
}

describe('PmdEngine', () => {
	const testPmdEngine = new TestPmdEngine();

	before(async () => {
		Sinon.createSandbox();

		await testPmdEngine.init();
	});
	after(() => {
		Sinon.restore();
	});
	describe('processStdOut()', () => {
		it('Non file XML nodes are filtered out of results', async () => {
			// This file contains non 'file' nodes that are direct children of the pmd node.
			// These nodes should be filtered out of the results without causing any errors.
			const xmlPath = path.join('test', 'code-fixtures', 'pmd-results', 'result-with-errors.txt');
			const fileHandler: FileHandler = new FileHandler();
			const xml: string = await fileHandler.readFile(xmlPath);
			expect(xml).to.not.be.null;

			const results = testPmdEngine.processStdOut(xml);
			expect(results).to.be.length(1, 'Results should be for be a single file');
			expect(results[0].violations).to.be.length(13, 'The file should have 13 violations');
		});
	});

	describe('emitErrorsAndWarnings()', () => {
		it('Non file XML nodes are filtered converted to UX events', async () => {
			const expectedError = `PMD failed to evaluate against file 'Foo.java'. Message: Issue with Foo`;
			const expectedConfigError = `PMD failed to evaluate rule 'LoosePackageCoupling'. Message: No packages or classes specified`;
			const expectedSuppressedViolation = `PMD suppressed violation against file 'Bar.java'. Message: Rule suppressed message. Suppression Type: Warning. User Message: Rule user message`;
			const uxSpy = Sinon.spy(uxEvents, 'emit');

			const xmlPath = path.join('test', 'code-fixtures', 'pmd-results', 'result-with-errors.txt');
			const fileHandler: FileHandler = new FileHandler();
			const xml: string = await fileHandler.readFile(xmlPath);
			expect(xml).to.not.be.null;

			await testPmdEngine.processStdOut(xml);
			Sinon.assert.callCount(uxSpy, 3);
			Sinon.assert.calledWith(uxSpy, 'warning-always', expectedConfigError);
			Sinon.assert.calledWith(uxSpy, 'warning-always', expectedError);
			Sinon.assert.calledWith(uxSpy, 'warning-always', expectedSuppressedViolation);
		});
	});

	describe('processStdout unusual cases', () => {
		it('Empty stdout', async () => {
			const results = await testPmdEngine.processStdOut('');
			expect(results).to.be.not.null;
			expect(results).to.be.lengthOf(0);
		})

		it('Missing closing tag', async () => {
			const results = await testPmdEngine.processStdOut('<?xml blah blah blah');
			expect(results).to.be.not.null;
			expect(results).to.be.lengthOf(0);
		})

		it('Missing opening tag', async () => {
			const results = await testPmdEngine.processStdOut('blah blah blah</pmd>');
			expect(results).to.be.not.null;
			expect(results).to.be.lengthOf(0);
		})
	});
});
