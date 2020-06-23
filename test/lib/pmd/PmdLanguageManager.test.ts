import {expect} from 'chai';
import {before} from 'mocha';
import Sinon = require('sinon');
import {Config, EngineConfigContent} from '../../../src/lib/util/Config';
import * as PmdLanguageManager from '../../../src/lib/pmd/PmdLanguageManager';
import {PmdEngine} from '../../../src/lib/pmd/PmdEngine';

describe('PmdLanguageManager', () => {
	describe('getSupportedLanguages()', () => {
		describe('When Config DOES specify supported languages', () => {
			const withDefaultLanguages = {
				name: "pmd",
				targetPatterns: [
					"**/*.cls", "**/*.java", "**/*.js", "**/*.page", "**/*.component", "**/*.xml",
					"!**/node_modules/**", "!**/*-meta.xml"
				],
				// Simulate a config that specifies Apex, JavaScript, and Java.
				supportedLanguages: ['apex', 'javascript', 'java']
			};
			let finalConfig: EngineConfigContent = null;

			before(() => {
				Sinon.createSandbox();
				// getEngineConfig should return the fake config that we've spoofed.
				Sinon.stub(Config.prototype, 'getEngineConfig').withArgs(PmdEngine.NAME).returns(withDefaultLanguages);
				// setEngineConfig should assign the modified config to the finalConfig variable so we can see what it's trying
				// to set.
				Sinon.stub(Config.prototype, 'setEngineConfig').callsFake(async (name: string, ecc: EngineConfigContent) => {
					finalConfig = ecc
				});
			});

			after(() => {
				Sinon.restore();
				finalConfig = null;
			});

			it('Explicitly supported languages are returned, and config IS NOT updated', async () => {
				const langs: string[] = await PmdLanguageManager.getSupportedLanguages();
				expect(langs).to.deep.equal(['apex', 'javascript', 'java'], 'Explicitly supported languages should have been returned');
				expect(finalConfig).to.equal(null, 'Config should not have been overwritten');
			});
		});

		describe('When Config DOES NOT specify supported languages', () => {
			const withoutDefaultLanguages = {
				name: "pmd",
				targetPatterns: [
					"**/*.cls", "**/*.java", "**/*.js", "**/*.page", "**/*.component", "**/*.xml",
					"!**/node_modules/**", "!**/*-meta.xml"
				]
			};
			let finalConfig: EngineConfigContent = null;

			before(() => {
				Sinon.createSandbox();
				// getEngineConfig should return the fake config that we've spoofed.
				Sinon.stub(Config.prototype, 'getEngineConfig').withArgs(PmdEngine.NAME).returns(withoutDefaultLanguages);
				// setEngineConfig should assign the modified config to the finalConfig variable so we can see what it's trying
				// to set.
				Sinon.stub(Config.prototype, 'setEngineConfig').callsFake(async (name: string, ecc: EngineConfigContent) => {
					finalConfig = ecc
				});
			});

			after(() => {
				Sinon.restore();
				finalConfig = null;
			});

			it('Default supported languages are returned, and config IS updated', async () => {
				const langs: string[] = await PmdLanguageManager.getSupportedLanguages();
				expect(langs).to.deep.equal(['apex', 'javascript'], 'Default supported languages should have been returned');
				expect(finalConfig).to.have.property('supportedLanguages');
				expect(finalConfig.supportedLanguages).to.deep.equal(['apex', 'javascript'], 'Default languages should have been written to config.');
			});
		});
	});
});
