import {expect} from 'chai';
import Sinon = require('sinon');
import {Config} from '../../../src/lib/util/Config';
import * as PmdLanguageManager from '../../../src/lib/pmd/PmdLanguageManager';
import messages = require('../../../messages/PmdLanguageManager');
import { ENGINE } from '../../../src/Constants';

describe('PmdLanguageManager', () => {
	describe('getSupportedLanguages()', () => {
		describe('When Config specifies exact language names', () => {
			const exactFakeLangs = ['apex', 'javascript', 'java'];

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
				expect(langs).to.deep.equal(['apex', 'javascript', 'java'], 'Explicitly supported languages should have been returned');
			});
		});

		describe('When Config specifies weirdly aliased language names', () => {
			const weirdlyAliasedLangs = ['ApEx', 'EcMaScRiPt', 'JaVa', 'Pl/SqL'];

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
				expect(langs).to.deep.equal(['apex', 'javascript', 'java', 'plsql'], 'Aliases to languages should have been resolved');
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
					expect(e.message).to.include(messages.InvalidLanguageAlias.replace('%s', 'NotRealLang'));
				}
			});
		});
	});
});
