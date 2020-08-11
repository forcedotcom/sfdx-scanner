import {PmdCatalogWrapper} from '../../../src/lib/pmd/PmdCatalogWrapper';
import {PmdEngine} from '../../../src/lib/pmd/PmdEngine';
import {CustomRulePathManager} from '../../../src/lib/CustomRulePathManager';
import {Config} from '../../../src/lib/util/Config';
import {expect} from 'chai';
import Sinon = require('sinon');
import path = require('path');
import {ENGINE,LANGUAGE} from '../../../src/Constants';
import {FileHandler} from '../../../src/lib/util/FileHandler';
import {uxEvents} from '../../../src/lib/ScannerEvents';

// In order to get access to PmdCatalogWrapper's protected methods, we're going to extend it with a test class here.
class TestablePmdCatalogWrapper extends PmdCatalogWrapper {
	public async buildCommandArray(): Promise<[string, string[]]> {
		return super.buildCommandArray();
	}

	public async getRulePathEntries(): Promise<Map<string, Set<string>>> {
		return super.getRulePathEntries();
	}
}
const irrelevantPath = path.join('this', 'path', 'does', 'not', 'actually', 'matter');

describe('PmdCatalogWrapper', () => {
	describe('buildCommandArray()', () => {
		describe('JAR parameters', () => {
			describe('When Custom PMD JARs have been registered for a language whose default PMD rules are off...', () => {
				before(() => {
					Sinon.createSandbox();
					// Spoof a config that claims that only Apex's default PMD JAR is enabled.
					Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(ENGINE.PMD).resolves([LANGUAGE.APEX]);
					// Spoof a CustomPathManager that claims that a custom JAR exists for Java.
					const customJars: Map<string, Set<string>> = new Map();
					customJars.set(LANGUAGE.JAVA, new Set([irrelevantPath]));
					Sinon.stub(CustomRulePathManager.prototype, 'getRulePathEntries').withArgs(PmdEngine.NAME).resolves(customJars);
					Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
				});

				after(() => {
					Sinon.restore();
				});

				it('Custom PMD JARs are included', async () => {
					// Get our parameters.
					const target = await TestablePmdCatalogWrapper.create({});
					const params = (await target.buildCommandArray())[1];

					// Expect there to be 7 parameters.
					expect(params.length).to.equal(7, `Should have been 7 parameters: ${params}`);
					// Expect the first three parameters to be the catalogHome, catalogName and -cp flag.
					expect(params[0]).to.contain('-DcatalogHome=', 'First parameter should be catalog home path');
					expect(params[1]).to.equal('-DcatalogName=PmdCatalog.json', 'Second parameter should be catalog override');
					expect(params[2]).to.equal('-cp', 'Second parameter should be -cp flag');
					// The third parameter should be the classpath, and we want to make sure it contains our fake path.
					expect(params[3]).to.contain(irrelevantPath, 'Third parameter should be classpath, including custom Java JAR');
					// The fourth parameter should be the main class.
					expect(params[4]).to.equal('sfdc.sfdx.scanner.pmd.Main', 'Fourth parameter is the main class');
					// The fifth parameter is the Java JAR, and not the standard PMD JAR.
					expect(params[5]).to.equal(`java=${irrelevantPath}`, 'Fifth parameter should be java-specific input, w/Custom Jar.');
					// The sixth parameter should be the default Apex JAR.
					expect(params[6]).to.match(/^apex=.*pmd-apex-.*.jar$/, 'Sixth parameter is Apex-specific, with only standard JAR.');
				});
			});

			describe('When Custom PMD JARs have been registered for a language under a weird alias...', () => {
				before(() => {
					Sinon.createSandbox();
					// Spoof a config that claims that only Apex's default PMD JAR is enabled.
					Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(ENGINE.PMD).resolves([LANGUAGE.APEX]);
					// Spoof a CustomPathManager that claims that a custom JAR exists for plsql, using a weird alias for that language.
					const customJars: Map<string, Set<string>> = new Map();
					customJars.set('Pl/SqL', new Set([irrelevantPath]));
					Sinon.stub(CustomRulePathManager.prototype, 'getRulePathEntries').withArgs(PmdEngine.NAME).resolves(customJars);
					Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
				});

				after(() => {
					Sinon.restore();
				});

				it('Custom PMD JARs are included', async () => {
					// Get our parameters.
					const target = await TestablePmdCatalogWrapper.create({});
					const params = (await target.buildCommandArray())[1];

					// Expect there to be 6 parameters.
					expect(params.length).to.equal(7, `Should have been 7 parameters: ${params}`);
					// Expect the first three parameters to be the catalogHome, catalogName and -cp flag.
					expect(params[0]).to.contain('-DcatalogHome=', 'First parameter should be catalog home path');
					expect(params[1]).to.equal('-DcatalogName=PmdCatalog.json', 'First parameter should be catalog override');
					expect(params[2]).to.equal('-cp', 'Second parameter should be -cp flag');
					// The next parameter should be the classpath, and we want to make sure it contains our fake path.
					expect(params[3]).to.contain(irrelevantPath, 'Third parameter should be classpath, including custom Java JAR');
					// The next parameter should be the main class.
					expect(params[4]).to.equal('sfdc.sfdx.scanner.pmd.Main', 'Fourth parameter is the main class');
					// The next parameter is the PLSQL JAR, and not the standard PMD JAR.
					expect(params[5]).to.equal(`plsql=${irrelevantPath}`, 'Fifth parameter should be plsql-specific input, w/Custom Jar.');
					// The next parameter should be the default Apex JAR.
					expect(params[6]).to.match(/^apex=.*pmd-apex-.*.jar$/, 'Sixth parameter is Apex-specific, with only standard JAR.');
				});
			});

			describe("When not all supported languages have an associated PMD JAR", () => {
				before(() => {
					Sinon.createSandbox();
					// Spoof a config that claims that only apex is the supported language
					Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(ENGINE.PMD).resolves([LANGUAGE.APEX]);
					const customJars: Map<string, Set<string>> = new Map();
					customJars.set('pl/sql', new Set([irrelevantPath]));
					customJars.set(LANGUAGE.JAVA, new Set());
					Sinon.stub(CustomRulePathManager.prototype, 'getRulePathEntries').withArgs(PmdEngine.NAME).resolves(customJars);
					Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
				});

				after(() => {
					Sinon.restore();
				});

				it('should not include a supported language as input to PmdCataloger if the language has no associated path', async () => {
					// Get our parameters.
					const target = await TestablePmdCatalogWrapper.create({});
					const params = (await target.buildCommandArray())[1];

					// verify that there's no eighth parameter for java since there's no associated path
					expect(params.length).equals(7, `Expected exactly seven parameters. Last parameter is ${params[params.length - 1]}`);

					// Confirm that the fourth parameter is the main class.
					expect(params[4]).to.equal('sfdc.sfdx.scanner.pmd.Main', 'Fifth parameter is the main class');

					// verify that the fifth parameter is for plsql
					expect(params[5]).to.equal(`plsql=${irrelevantPath}`, 'Sixth parameter should be plsql-specific');

					// verify that the sixth parameter is for plsql
					expect(params[6]).to.match(/^apex=.*jar$/, 'Seventh parameter should be apex-specific');
				});
			});

			describe('Missing Rule Files are Handled Gracefully', () => {
				const validJar = 'jar-that-exists.jar';
				const missingJar = 'jar-that-is-missing.jar';
				// This jar is automatically included by the PmdCatalogWrapper
				const pmdJar = path.resolve(path.join('dist', 'pmd', 'lib', 'pmd-java-6.22.0.jar'));
				let uxSpy = null;

				before(() => {
					Sinon.createSandbox();
					Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(ENGINE.PMD).resolves([LANGUAGE.JAVA]);
					const customJars: Map<string, Set<string>> = new Map();
					// Simulate CustomPaths.json contains a jar that has been deleted or moved
					customJars.set(LANGUAGE.JAVA, new Set([validJar, missingJar]));
					Sinon.stub(CustomRulePathManager.prototype, 'getRulePathEntries').withArgs(PmdEngine.NAME).resolves(customJars);
					const stub = Sinon.stub(FileHandler.prototype, 'exists');
					stub.withArgs(validJar).resolves(true);
					stub.withArgs(missingJar).resolves(false);
					uxSpy = Sinon.spy(uxEvents, 'emit');
				});

				after(() => {
					Sinon.restore();
				});

				it('Missing file should be filtered out and display warning', async () => {
					const target = await TestablePmdCatalogWrapper.create({});
					const entries = await target.getRulePathEntries();

					// The rule path entries should only include the jar that exists
					expect(entries.size).to.equal(1, `Entries: ${Array.from(entries.keys())}`);
					const jars = entries.get(LANGUAGE.JAVA);
					const jarsErrorMessage =  `Jars: ${Array.from(jars)}`;
					expect(jars.size).to.equal(2, jarsErrorMessage);
					expect(jars).to.contain(validJar, jarsErrorMessage);
					expect(jars).to.contain(pmdJar, jarsErrorMessage);
					expect(jars).to.not.contain(missingJar, jarsErrorMessage);

					// A warning should be displayed
					Sinon.assert.calledOnce(uxSpy);
					Sinon.assert.calledWith(uxSpy, 'warning-always', `Custom rule file path [${missingJar}] for language [${LANGUAGE.JAVA}] was not found.`);
				});
			});
		});
	});
});
