import {expect} from 'chai';
import Sinon = require('sinon');
import {Messages} from '@salesforce/core';
import {Config} from '../../../src/lib/util/Config';
import {LANGUAGE} from '../../../src/Constants';
import * as PmdLanguageManager from '../../../src/lib/pmd/PmdLanguageManager';
import messages = require('../../../messages/PmdLanguageManager');
import { ENGINE } from '../../../src/Constants';
import {uxEvents} from '../../../src/lib/ScannerEvents';
import * as TestOverrides from '../../test-related-lib/TestOverrides';

TestOverrides.initializeTestSetup();

Messages.importMessagesDirectory(__dirname);
const eventMessages = Messages.loadMessages("@salesforce/sfdx-scanner", "EventKeyTemplates");

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
			const weirdlyAliasedLangs = ['ApEx', 'JaVa', 'Pl/SqL'];

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
				expect(langs).to.deep.equal([LANGUAGE.APEX, LANGUAGE.JAVA, LANGUAGE.PLSQL], 'Aliases to languages should have been resolved');
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
					expect(ex.message).to.equal('Javascript is not currently supported by the PMD engine.');
				}
			});
		});

		describe('When config includes a language marked for termination', () => {
			const langs = ['JaVa', 'PlSqL', 'modelica', 'scala'];
			let uxSpy;

			before(() => {
				Sinon.createSandbox();
				uxSpy = Sinon.spy(uxEvents, 'emit');
				// Simulate a config that specifies (among other languages) at least one language for which we intend to
				// pull support.
				Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(ENGINE.PMD).resolves(langs);
			});

			after(() => {
				Sinon.restore();
			});

			it('Emits warning for each such language', async () => {
				const outLangs = await PmdLanguageManager.getSupportedLanguages();
				// All languages should be included, since they're all valid.
				expect(outLangs.length).to.equal(4, 'Wrong number of supported langs output');
				Sinon.assert.callCount(uxSpy, 2);
				expect(
					uxSpy.getCall(0).calledWith('warning-always', eventMessages.getMessage('warning.langMarkedForDeath', ['modelica']))
				).to.equal(true, 'Wrong warning logged');
				expect(
					uxSpy.getCall(1).calledWith('warning-always', eventMessages.getMessage('warning.langMarkedForDeath', ['scala']))
				).to.equal(true, 'Wrong warning logged');
			});
		})
	});
});
