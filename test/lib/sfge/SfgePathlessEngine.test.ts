import 'reflect-metadata';
import {RuleResult, SfgeConfig} from '../../../src/types';
import path = require('path');
import fs = require('fs');
import {expect} from 'chai';
import {SfgePathlessEngine} from '../../../src/lib/sfge/SfgePathlessEngine';
import * as TestOverrides from '../../test-related-lib/TestOverrides';
import {describe} from "mocha";
import {SfgeCatalogWrapper, SfgeExecutionWrapper} from "../../../src/lib/sfge/SfgeWrapper";
import {CUSTOM_CONFIG} from "../../../src/Constants";
import Sinon = require('sinon');

TestOverrides.initializeTestSetup();

class TestableSfgePathlessEngine extends SfgePathlessEngine {
	public processStdout(output: string): RuleResult[] {
		return super.processStdout(output);
	}
}

describe('SfgePathlessEngine', () => {
	describe('#getCatalog()', () => {
		let catalogBuildSpy: Sinon.SinonSpy;

		beforeEach(async () => {
			catalogBuildSpy = Sinon.spy(SfgeCatalogWrapper, 'getCatalog');
		});

		afterEach(() => {
			Sinon.restore();
		});

		it('Non-DFA engine returns Non-DFA rules only', async () => {
			const testEngine = new TestableSfgePathlessEngine();
			await testEngine.init();

			// ==== TESTED METHOD ====
			const catalog = await testEngine.getCatalog();

			// ==== ASSERTIONS ====
			expect(catalog.rules.length).to.equal(1);
		});

		it('Catalog only initialized once', async () => {
			const testEngine = new TestableSfgePathlessEngine();
			await testEngine.init();

			// ==== TESTED METHOD ====
			await testEngine.getCatalog();
			await testEngine.getCatalog();

			// ==== ASSERTIONS ====
			Sinon.assert.callCount(catalogBuildSpy, 1);
		});
	})

	describe('#processStdout()', () => {
		it('When Sfge finds violations, they are converted into RuleResult objects', async () => {
			// ==== SETUP ====
			const testEngine = new TestableSfgePathlessEngine();
			await testEngine.init();
			const spoofedOutput = fs.readFileSync(path.join('test', 'code-fixtures', 'sfge-results', 'one_pathless_violation.txt')).toString();

			// ==== TESTED METHOD ====
			const results = testEngine.processStdout(spoofedOutput);

			// ==== ASSERTIONS ====
			expect(results.length).to.equal(1, 'Should be results');
		});

		it('When sfge finds no violations, results are empty', async () => {
			// ==== SETUP ====
			const testEngine = new TestableSfgePathlessEngine();
			await testEngine.init();
			const spoofedOutput = fs.readFileSync(path.join('test', 'code-fixtures', 'sfge-results', 'no_violations.txt')).toString();

			// ==== TESTED METHOD ====
			const results = testEngine.processStdout(spoofedOutput);

			// ==== ASSERTIONS ====
			expect(results.length).to.equal(0, 'No results should have been returned');
		});
	});

	describe('#run()', () => {
		const dummyRule1 = {
			engine: "sfge",
			sourcepackage: "sfge",
			name: "DummyRule1",
			description: "MeaninglessDescription",
			categories: ["Security"],
			rulesets: [],
			languages: ["apex"],
			defaultEnabled: true
		};
		const dummyRule2 = {
			engine: "sfge",
			sourcepackage: "sfge",
			name: "DummyRule2",
			description: "MeaninglessDescription",
			categories: ["Security"],
			rulesets: [],
			languages: ["apex"],
			defaultEnabled: true
		};
		const fakeCatalog = {
			rules: [dummyRule1],
			categories: [{
				engine: "sfge",
				name: "Security",
				paths: []
			}],
			rulesets: []
		};
		let testEngine: TestableSfgePathlessEngine;
		let runSfgeStub: Sinon.SinonStub;
		const fakeConfig: SfgeConfig = {
			projectDirs: ['irrelevant']
		}

		beforeEach(async () => {
			Sinon.createSandbox();
			testEngine = new TestableSfgePathlessEngine();
			await testEngine.init();
			// The catalog should be stubbed out for one that has only one of our fake SFGE rules.
			(testEngine as any).catalog = fakeCatalog;
			runSfgeStub = Sinon.stub(SfgeExecutionWrapper, 'runSfge').resolves("VIOLATIONS_START[]VIOLATIONS_END");
		});

		afterEach(() => {
			Sinon.restore();
		});


		it('Only attempts to run rules in catalog', async () => {
			// ==== SETUP ====
			// Create our engineOptions object
			const engineOptions: Map<string, string> = new Map();
			engineOptions.set(CUSTOM_CONFIG.SfgeConfig, JSON.stringify(fakeConfig));
			// ==== TESTED METHOD ===
			// Call the engine with both of the fake rules.
			const results = await testEngine.run([], [dummyRule1, dummyRule2], [{
				target: "this does not matter",
				paths: ["this does not matter either"],
				methods: []
			}], engineOptions);

			// ==== ASSERTIONS ====
			expect(results.length).to.equal(0);
			Sinon.assert.callCount(runSfgeStub, 1);
		});
	});
});
