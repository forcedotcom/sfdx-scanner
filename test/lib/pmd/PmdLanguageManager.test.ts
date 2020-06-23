import {expect} from 'chai';
import {before} from 'mocha';
import Sinon = require('sinon');
import {Config, EngineConfigContent} from '../../../src/lib/util/Config';
import * as PmdLanguageManager from '../../../src/lib/pmd/PmdLanguageManager';
import {PmdEngine} from '../../../src/lib/pmd/PmdEngine';

describe('PmdLanguageManager', () => {
	describe('getSupportedLanguages()', () => {
		const fakePmdConfig: EngineConfigContent = {
			name: "pmd",
			targetPatterns: [
				"**/*.cls", "**/*.java", "**/*.js", "**/*.page", "**/*.component", "**/*.xml",
				"!**/node_modules/**", "!**/*-meta.xml"
			]
		};
		let finalConfig: EngineConfigContent = null;

		before(() => {
			Sinon.createSandbox();
			// getEngineConfig() should return the fake config that we've spoofed.
			Sinon.stub(Config.prototype, 'getEngineConfig').withArgs(PmdEngine.NAME).returns(fakePmdConfig);
			// setEngineConfig() should assign the config to finalConfig so we can see what it's trying to set.
			Sinon.stub(Config.prototype, 'setEngineConfig').callsFake(async (name: string, ecc: EngineConfigContent) => {
				finalConfig = ecc;
			});
		});

		after(() => {
			Sinon.restore();
			finalConfig = null;
		});

		describe('When Config DOES specify supported languages', () => {
			it('Explicitly supported languages are returned, and config is NOT updated', async () => {
				// Simulate a config that specifies Apex, JavaScript, and Java.
				fakePmdConfig.supportedLanguages = ['apex', 'javascript', 'java'];

				const langs = await PmdLanguageManager.getSupportedLanguages();
				expect(langs).to.deep.equal(['apex', 'javascript', 'java'], 'Explicitly supported languages should have been returned');
				expect(finalConfig).to.equal(null, 'Config should not have been overwritten');
			});

			it('Aliases are successfully resolved into a viable language name', async () => {
				// Simulate a config that specifies some languages using some weird casing and aliasesspecifies Apex, JavaScript, and Java, using some weird casing and aliases.
				fakePmdConfig.supportedLanguages = ['ApEx', 'EcMaScRiPt', 'Java', 'pl/sql'];

				const langs = await PmdLanguageManager.getSupportedLanguages();
				expect(langs).to.deep.equal(['apex', 'javascript', 'java', 'plsql'], 'Aliases to languages should have been resolved');
				expect(finalConfig).to.equal(null, 'Config should not have been overwritten');
			});
		});

		describe('When Config DOES NOT specify supported languages', () => {
			it('Default supported languages are returned, and Config is updated', async () => {
				// Simulate a config that specifies no default languages.
				delete fakePmdConfig.supportedLanguages;

				const langs = await PmdLanguageManager.getSupportedLanguages();
				expect(langs).to.deep.equal(['apex', 'javascript'], 'Default supported languages should have been returned');
				expect(finalConfig).to.have.property('supportedLanguages');
				expect(finalConfig.supportedLanguages).to.deep.equal(['apex', 'javascript'], 'Default languages should have been written to config');
			});
		});
	});
});
