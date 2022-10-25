import {expect} from 'chai';
import Sinon = require('sinon');
import {Messages} from '@salesforce/core';
import {Config} from '../../../src/lib/util/Config';
import {LANGUAGE} from '../../../src/Constants';
import * as PmdLanguageManager from '../../../src/lib/pmd/PmdLanguageManager';
import messages = require('../../../messages/PmdLanguageManager');
import { ENGINE } from '../../../src/Constants';
import { Controller } from '../../../src/Controller';
import * as TestOverrides from '../../test-related-lib/TestOverrides';

TestOverrides.initializeTestSetup();

Messages.importMessagesDirectory(__dirname);

describe('PmdLanguageManager', () => {
	describe('getSupportedLanguages()', () => {
		describe('When Config specifies exact language names', () => {
			const exactFakeLangs = [LANGUAGE.APEX, LANGUAGE.JAVA];

			before(() => {
				Sinon.createSandbox();
				// Simulate a config that specifies Apex, JavaScript, and Java.
				Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(ENGINE.PMD).resolves(exactFakeLangs);
			});

			after(() => {
				Sinon.restore();
			});

			it('Explicitly supported languages are returned', async () => {
				const langs = await PmdLanguageManager.getSupportedLanguages();
				expect(langs).to.deep.equal([LANGUAGE.APEX, LANGUAGE.JAVA], 'Explicitly supported languages should have been returned');
			});
		});

		describe('When Config specifies weirdly aliased language names', () => {
			const weirdlyAliasedLangs = ['ApEx', 'JaVa', 'ViSuAlFoRcE'];

			before(() => {
				Sinon.createSandbox();
				// Simulate a config that specifies some languages using some weird casing and aliases.
				Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(ENGINE.PMD).resolves(weirdlyAliasedLangs);
			});

			after(() => {
				Sinon.restore();
			});

			it('Aliases are successfully resolved into a viable language name', async () => {
				const langs = await PmdLanguageManager.getSupportedLanguages();
				expect(langs).to.deep.equal([LANGUAGE.APEX, LANGUAGE.JAVA, 'visualforce'], 'Aliases to languages should have been resolved');
			});
		});

		describe('When Config specifies invalid language alias', () => {
			const invalidAliasLangs = ['NotRealLang'];

			before(() => {
				Sinon.createSandbox();
				// Simulate a config that specifies some nonsensical alias.
				Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(ENGINE.PMD).resolves(invalidAliasLangs);
			});

			after(() => {
				Sinon.restore();
			});

			it('When no viable alias is found, an error is thrown', async () => {
				try {
					await PmdLanguageManager.getSupportedLanguages();
					expect(true).to.equal(false, 'Error should have thrown');
				} catch (e) {
					expect(e.message).to.include(messages.InvalidLanguageAlias.replace('%s', (await Controller.getConfig()).getConfigFilePath()).replace('%s', 'NotRealLang'));
				}
			});
		});

		describe('When Javascript appears in config()', () => {
			const langsContainingJavaScript = [LANGUAGE.APEX, LANGUAGE.JAVASCRIPT];

			before(() => {
				Sinon.createSandbox();
				// Simulate a config that specifies javascript.
				Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(ENGINE.PMD).resolves(langsContainingJavaScript);
			});

			after(() => {
				Sinon.restore();
			});

			it('Throws exception if javascript is found in supportedLanguages array', async () => {
				try {
					await PmdLanguageManager.getSupportedLanguages();
					expect(true).to.equal(false, 'Error should have thrown');
				} catch (ex) {
					expect(ex.message).to.equal(`Javascript isn't supported by the PMD engine.`);
				}
			});
		});
	});
});
