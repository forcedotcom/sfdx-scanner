import {PmdCatalogWrapper} from '../../../src/lib/pmd/PmdCatalogWrapper';
import {PMD_VERSION} from '../../../src/lib/pmd/PmdSupport';
import {PmdEngine} from '../../../src/lib/pmd/PmdEngine';
import {CustomRulePathManager} from '../../../src/lib/CustomRulePathManager';
import {Config} from '../../../src/lib/util/Config';
import {expect} from 'chai';
import Sinon = require('sinon');
import path = require('path');
import {ENGINE,LANGUAGE} from '../../../src/Constants';
import {FileHandler} from '../../../src/lib/util/FileHandler';
import {uxEvents} from '../../../src/lib/ScannerEvents';
import { after } from 'mocha';

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
			describe('Common to all scenarios', () => {
				before(() => {
					Sinon.createSandbox();
					// Spoof a config that claims that only Apex's default PMD JAR is enabled.
					Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(ENGINE.PMD).resolves([LANGUAGE.APEX]);
				});

				after(() => {
					Sinon.restore();
				});

				it('uses the correct common parameter values', async () => {
					const thePath = path.join('dist', 'pmd-cataloger', 'lib');
					const expectedParamList = [
						`-DcatalogHome=`,
						'-DcatalogName=PmdCatalog.json',
						'-cp',
						thePath,
						'sfdc.sfdx.scanner.pmd.Main'];

					const target = await TestablePmdCatalogWrapper.create({});
					const params = (await target.buildCommandArray())[1];

					expectedParamList.forEach((value: string, index: number, array: string[]) => {
						expect(params[index]).contains(value, `Unexpected param value at position ${index}`);
					});
				});
			});
			describe('When Custom PMD JARs have been registered for a language whose default PMD rules are off...', () => {
				before(() => {
					Sinon.createSandbox();
					// Spoof a config that claims that only Apex's default PMD JAR is enabled.
					Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(ENGINE.PMD).resolves([LANGUAGE.APEX]);
					// Spoof a CustomPathManager that claims that a custom JAR exists for Java.
					const customJars: Map<string, Set<string>> = new Map();
					customJars.set(LANGUAGE.JAVA, new Set([irrelevantPath]));
					Sinon.stub(CustomRulePathManager.prototype, 'getRulePathEntries').withArgs(PmdEngine.ENGINE_NAME).resolves(customJars);
					Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
				});

				after(() => {
					Sinon.restore();
				});

				it('Custom PMD JARs are included', async () => {
					// Get our parameters.
					const target = await TestablePmdCatalogWrapper.create({});
					const params = (await target.buildCommandArray())[1];

					const customJarValue = `java=${irrelevantPath}`;
					const defaultJarPattern = /^apex=.*pmd-apex-.*.jar$/;

					validateCustomAndDefaultJarParams(params, customJarValue, defaultJarPattern);
				});
			});

			describe('When Custom PMD JARs have been registered for a language under a weird alias...', () => {
				before(() => {
					Sinon.createSandbox();
					// Spoof a config that claims that only Apex's default PMD JAR is enabled.
					Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(ENGINE.PMD).resolves([LANGUAGE.APEX]);
					// Spoof a CustomPathManager that claims that a custom JAR exists for plsql, using a weird alias for that language.
					const customJars: Map<string, Set<string>> = new Map();
					customJars.set('ViSuAlFoRcE', new Set([irrelevantPath]));
					Sinon.stub(CustomRulePathManager.prototype, 'getRulePathEntries').withArgs(PmdEngine.ENGINE_NAME).resolves(customJars);
					Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
				});

				after(() => {
					Sinon.restore();
				});

				it('Custom PMD JARs are included', async () => {
					// Get our parameters.
					const target = await TestablePmdCatalogWrapper.create({});
					const params = (await target.buildCommandArray())[1];

					const customJarValue = `visualforce=${irrelevantPath}`;
					const defaultJarPattern = /^apex=.*pmd-apex-.*.jar$/;
					validateCustomAndDefaultJarParams(params, customJarValue, defaultJarPattern);
				});
			});

			describe("When not all supported languages have an associated PMD JAR", () => {
				before(() => {
					Sinon.createSandbox();
					// Spoof a config that claims that only apex is the supported language
					Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(ENGINE.PMD).resolves([LANGUAGE.APEX]);
					const customJars: Map<string, Set<string>> = new Map();
					customJars.set('visualforce', new Set([irrelevantPath]));
					customJars.set(LANGUAGE.JAVA, new Set());
					Sinon.stub(CustomRulePathManager.prototype, 'getRulePathEntries').withArgs(PmdEngine.ENGINE_NAME).resolves(customJars);
					Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
				});

				after(() => {
					Sinon.restore();
				});

				it('should not include a supported language as input to PmdCataloger if the language has no associated path', async () => {
					// Get our parameters.
					const target = await TestablePmdCatalogWrapper.create({});
					const params = (await target.buildCommandArray())[1];

					const customJarValue = `visualforce=${irrelevantPath}`;
					const defaultJarPattern = /^apex=.*jar$/;

					validateCustomAndDefaultJarParams(params, customJarValue, defaultJarPattern);
				});
			});

			describe('Missing Rule Files are Handled Gracefully', () => {
				const validJar = 'jar-that-exists.jar';
				const missingJar = 'jar-that-is-missing.jar';
				// This jar is automatically included by the PmdCatalogWrapper
				const pmdJar = path.resolve(path.join('dist', 'pmd', 'lib', `pmd-java-${PMD_VERSION}.jar`));
				let uxSpy = null;

				before(() => {
					Sinon.createSandbox();
					Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(ENGINE.PMD).resolves([LANGUAGE.JAVA]);
					const customJars: Map<string, Set<string>> = new Map();
					// Simulate CustomPaths.json contains a jar that has been deleted or moved
					customJars.set(LANGUAGE.JAVA, new Set([validJar, missingJar]));
					Sinon.stub(CustomRulePathManager.prototype, 'getRulePathEntries').withArgs(PmdEngine.ENGINE_NAME).resolves(customJars);
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

function validateCustomAndDefaultJarParams(params: string[], customJarValue: string, defaultJarPattern: RegExp) {
	const expectedParamLength = 7;
	expect(params.length).to.equal(expectedParamLength, `Should have been ${expectedParamLength} parameters: ${params}`);

	const classpathIndex = expectedParamLength - 4;
	expect(params[classpathIndex]).to.contain(irrelevantPath, `Parameter as position ${classpathIndex} should be classpath, including custom Java JAR`);

	const customJarIndex = expectedParamLength - 2;
	expect(params[customJarIndex]).to.equal(customJarValue, `Parameter as position ${customJarIndex} is incorrect`);

	const defaultJarIndex = expectedParamLength - 1;
	expect(params[defaultJarIndex]).to.match(defaultJarPattern, `Parameter as position ${defaultJarIndex} does not match pattern`);
}

