import {PmdCatalogWrapper} from '../../../src/lib/pmd/PmdCatalogWrapper';
import * as PmdLanguageManager from '../../../src/lib/pmd/PmdLanguageManager';
import {PmdEngine} from '../../../src/lib/pmd/PmdEngine';
import {CustomRulePathManager} from '../../../src/lib/CustomRulePathManager';
import {Config} from '../../../src/lib/util/Config';
import {LANGUAGES} from '../../../src/Constants';
import {expect} from 'chai';
import Sinon = require('sinon');
import path = require('path');
import {fail} from 'assert';
// In order to get access to PmdCatalogWrapper's protected methods, we're going to extend it with a test class here.
class TestablePmdCatalogWrapper extends PmdCatalogWrapper {
	public async buildCommandArray(): Promise<[string, string[]]> {
		return super.buildCommandArray();
	}

	public async getRulePathEntries(): Promise<Map<string, Set<string>>> {
		return super.getRulePathEntries();
	}
}

describe('PmdCatalogWrapper', () => {
	describe('buildCommandArray()', () => {
		describe('JAR parameters', () => {
			describe('When Custom PMD JARs have been registered for a language whose default PMD rules are off...', () => {
				const irrelevantPath = path.join('this', 'path', 'does', 'not', 'actually', 'matter');
				before(() => {
					Sinon.createSandbox();
					// Spoof a config that claims that only Apex's default PMD JAR is enabled.
					Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(PmdEngine.NAME).resolves([LANGUAGES.APEX]);
					// Spoof a CustomPathManager that claims that a custom JAR exists for Java.
					const customJars: Map<string, Set<string>> = new Map();
					customJars.set('java', new Set([irrelevantPath]));
					Sinon.stub(CustomRulePathManager.prototype, 'getRulePathEntries').withArgs(PmdEngine.NAME).resolves(customJars);
				});

				after(() => {
					Sinon.restore();
				});

				it('Custom PMD JARs are included', async () => {
					// Get our parameters.
					const target = await TestablePmdCatalogWrapper.create({});
					const params = (await target.buildCommandArray())[1];

					// Expect there to be 6 parameters.
					expect(params.length).to.equal(6, 'Should have been 6 parameters');
					// Expect the first two parameters to be the catalog and -cp flag.
					expect(params[0]).to.equal('-DcatalogName=PmdCatalog.json', 'First parameter should be catalog override');
					expect(params[1]).to.equal('-cp', 'Second parameter should be -cp flag');
					// The third parameter should be the classpath, and we want to make sure it contains our fake path.
					expect(params[2]).to.contain(irrelevantPath, 'Third parameter should be classpath, including custom Java JAR');
					// The fourth parameter should be the main class.
					expect(params[3]).to.equal('sfdc.sfdx.scanner.pmd.Main', 'Fourth parameter is the main class');
					// The fifth parameter is the Java JAR, and not the standard PMD JAR.
					expect(params[4]).to.equal(`java=${irrelevantPath}`, 'Fifth parameter should be java-specific input, w/Custom Jar.');
					// The sixth parameter should be the default Apex JAR.
					expect(params[5]).to.match(/^apex=.*pmd-apex-.*.jar$/, 'Sixth parameter is Apex-specific, with only standard JAR.');
				});
			});

			describe('When Custom PMD JARs have been registered for a language under a weird alias...', () => {
				const irrelevantPath = path.join('this', 'path', 'does', 'not', 'actually', 'matter');
				before(() => {
					Sinon.createSandbox();
					// Spoof a config that claims that only Apex's default PMD JAR is enabled.
					Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(PmdEngine.NAME).resolves([LANGUAGES.APEX]);
					// Spoof a CustomPathManager that claims that a custom JAR exists for plsql, using a weird alias for that language.
					const customJars: Map<string, Set<string>> = new Map();
					customJars.set('Pl/SqL', new Set([irrelevantPath]));
					Sinon.stub(CustomRulePathManager.prototype, 'getRulePathEntries').withArgs(PmdEngine.NAME).resolves(customJars);
				});

				after(() => {
					Sinon.restore();
				});

				it('Custom PMD JARs are included', async () => {
					// Get our parameters.
					const target = await TestablePmdCatalogWrapper.create({});
					const params = (await target.buildCommandArray())[1];

					// Expect there to be 6 parameters.
					expect(params.length).to.equal(6, 'Should have been 6 parameters');
					// Expect the first two parameters to be the catalog and -cp flag.
					expect(params[0]).to.equal('-DcatalogName=PmdCatalog.json', 'First parameter should be catalog override');
					expect(params[1]).to.equal('-cp', 'Second parameter should be -cp flag');
					// The third parameter should be the classpath, and we want to make sure it contains our fake path.
					expect(params[2]).to.contain(irrelevantPath, 'Third parameter should be classpath, including custom Java JAR');
					// The fourth parameter should be the main class.
					expect(params[3]).to.equal('sfdc.sfdx.scanner.pmd.Main', 'Fourth parameter is the main class');
					// The fifth parameter is the PLSQL JAR, and not the standard PMD JAR.
					expect(params[4]).to.equal(`plsql=${irrelevantPath}`, 'Fifth parameter should be plsql-specific input, w/Custom Jar.');
					// The sixth parameter should be the default Apex JAR.
					expect(params[5]).to.match(/^apex=.*pmd-apex-.*.jar$/, 'Sixth parameter is Apex-specific, with only standard JAR.');
				});
			});
		});
	});

	describe('getRulePathEntries()', () => {
		before(() => {
			Sinon.createSandbox();
			// Stub out the array so that it returns 'javascript'
			Sinon.stub(PmdLanguageManager, "getSupportedLanguages").resolves([LANGUAGES.APEX, LANGUAGES.JAVASCRIPT]);
		});

		after(() => {
			Sinon.restore();
		});

		it('Throws exception if javascript is found in supportedLanguages array', async () => {
			const target = await TestablePmdCatalogWrapper.create({});

			try {
				await target.getRulePathEntries();
				fail('getRulePathEntries should have thrown');
			} catch (ex) {
				expect(ex.message).to.equal('Javascript is not currently supported by the PMD engine.');
			}
		});
	});
});
