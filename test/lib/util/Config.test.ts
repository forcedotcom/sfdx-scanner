// ================= IMPORTS ======================
import {Messages} from '@salesforce/core';
import {expect} from 'chai';
import Sinon = require('sinon');
import path = require('path');
import { fail } from 'assert';
import {Config, ConfigContent} from '../../../src/lib/util/Config';
import {CONFIG_PILOT_FILE, CONFIG_FILE, ENGINE} from '../../../src/Constants'
import { FileHandler } from '../../../src/lib/util/FileHandler';
import {VersionUpgradeManager, VersionUpgradeError} from '../../../src/lib/util/VersionUpgradeManager';
import { Controller } from '../../../src/Controller';
import {deepCopy} from '../../../src/lib/util/Utils';
import * as TestOverrides from '../../test-related-lib/TestOverrides';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);
TestOverrides.initializeTestSetup();

// ============== TYPES ===========================

type StubsCollection = {
	existsStub?: Sinon.SinonStub;
	mkDirStub?: Sinon.SinonStub;
	writeFileStub?: Sinon.SinonStub;
	readFileStub?: Sinon.SinonStub;
	getDefaultConfigStub?: Sinon.SinonStub;
	upgradeRequiredStub?: Sinon.SinonStub;
	upgradeToLatestStub?: Sinon.SinonStub;
};

// =============== CONSTANTS ======================
const SFDX_SCANNER_PATH = Controller.getSfdxScannerPath();
const CONFIG_PATH = path.join(SFDX_SCANNER_PATH, CONFIG_FILE);
const CONFIG_PILOT_PATH = path.join(SFDX_SCANNER_PATH, CONFIG_PILOT_FILE);
const PACKAGE_VERSION = require('../../../package.json').version;
const configMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'Config');
const BASE_CONFIG_GA = {
	"engines": [{
		"name": ENGINE.PMD,
		"targetPatterns": [
			"pattern1",
			"pattern2"
		]
	}, {
		"name": ENGINE.ESLINT_TYPESCRIPT,
		"targetPatterns": []
	}, {
		"name": ENGINE.RETIRE_JS,
		"targetPatterns": [],
		"disabled": false
	}],
	"javaHome": "/my/test/java/home",
	"currentVersion": '2.13.1'
};
const BASE_CONFIG_PILOT = {
	"engines": [{
		"name": ENGINE.PMD,
		"targetPatterns": [
			"pattern1",
			"pattern2"
		]
	}, {
		"name": ENGINE.ESLINT_TYPESCRIPT,
		"targetPatterns": []
	}, {
		"name": ENGINE.RETIRE_JS,
		"targetPatterns": [],
		"disabled": false
	}],
	"javaHome": "/my/test/java/home",
	"currentVersion": PACKAGE_VERSION
};
const SINON_SETUP_FUNCTIONS = {
	NO_EXISTING_CONFIGS: (): StubsCollection => {
		// The readFileStub should return the appropriate config for the specified path, because it's helpful for
		// testing if they return different objects.
		const readFileStub = Sinon.stub(FileHandler.prototype, 'readFile');
		readFileStub.withArgs(CONFIG_PATH).resolves(JSON.stringify(BASE_CONFIG_GA));
		readFileStub.withArgs(CONFIG_PILOT_PATH).resolves(JSON.stringify(BASE_CONFIG_PILOT));
		return {
			// No matter what the file is, the stub should say it doesn't exist.
			existsStub: Sinon.stub(FileHandler.prototype, 'exists').resolves(false),
			// The mkdir stub should just resolve, as though it created a directory without problems.
			mkDirStub: Sinon.stub(FileHandler.prototype, 'mkdirIfNotExists').resolves(),
			readFileStub,
			// The writeFile stub should just resolve, as though the file was written without issue.
			writeFileStub: Sinon.stub(FileHandler.prototype, 'writeFile').resolves()
		};
	},
	GA_CONFIG_ONLY: (): StubsCollection => {
		// The existsStub should say the GA config exists and the pilot config doesn't.
		const existsStub = Sinon.stub(FileHandler.prototype, 'exists');
		existsStub.withArgs(CONFIG_PATH).resolves(true);
		existsStub.withArgs(CONFIG_PILOT_PATH).resolves(false);
		// The readFileStub should return the appropriate config for the specified path, because it's helpful for
		// testing if they return different objects.
		const readFileStub = Sinon.stub(FileHandler.prototype, 'readFile');
		readFileStub.withArgs(CONFIG_PATH).resolves(JSON.stringify(BASE_CONFIG_GA));
		readFileStub.withArgs(CONFIG_PILOT_PATH).resolves(JSON.stringify(BASE_CONFIG_PILOT));
		return {
			existsStub,
			// The mkdir stub should just resolve, as though it created a directory without problems.
			mkDirStub: Sinon.stub(FileHandler.prototype, 'mkdirIfNotExists').resolves(),
			readFileStub,
			// The writeFile stub should just resolve, as though the file was written without issue.
			writeFileStub: Sinon.stub(FileHandler.prototype, 'writeFile').resolves()
		};
	},
	DEFAULT_CONFIG: (): StubsCollection => {
		return {
			// If the config doesn't exist, then the default should be used instead.
			existsStub: Sinon.stub(FileHandler.prototype, 'exists').resolves(false),
			mkDirStub: Sinon.stub(FileHandler.prototype, 'mkdirIfNotExists').resolves(),
			writeFileStub: Sinon.stub(FileHandler.prototype, 'writeFile').resolves()
		};
	},
	OVERRIDE_CONFIG: (testConfig: ConfigContent): StubsCollection => {
		return {
			// All configs should be considered to exist.
			existsStub: Sinon.stub(FileHandler.prototype, 'exists').resolves(true),
			// Reading any config should return the provided test config.
			readFileStub: Sinon.stub(FileHandler.prototype, 'readFile').resolves(JSON.stringify(testConfig)),
			// Writing a config should just resolve immediately.
			writeFileStub: Sinon.stub(FileHandler.prototype, 'writeFile').resolves()
		};
	},
	NONUPGRADEABLE_VUM: (): StubsCollection => {
		return {
			upgradeRequiredStub: Sinon.stub(VersionUpgradeManager.prototype, 'upgradeRequired').returns(false),
			upgradeToLatestStub: Sinon.stub(VersionUpgradeManager.prototype, 'upgradeToLatest').resolves()
		};
	},
	SUCCESSFUL_UPGRADE_VUM: (): StubsCollection => {
		return {
			upgradeRequiredStub: Sinon.stub(VersionUpgradeManager.prototype, 'upgradeRequired').returns(true),
			upgradeToLatestStub: Sinon.stub(VersionUpgradeManager.prototype, 'upgradeToLatest').resolves()
		};
	},
	FAILED_UPGRADE_VUM: (e: Error): StubsCollection => {
		return {
			upgradeRequiredStub: Sinon.stub(VersionUpgradeManager.prototype, 'upgradeRequired').returns(true),
			upgradeToLatestStub: Sinon.stub(VersionUpgradeManager.prototype, 'upgradeToLatest').rejects(e)
		};
	}
};


describe('Config.ts', () => {
	const configFilePath = path.resolve(SFDX_SCANNER_PATH, CONFIG_PILOT_FILE);
	let testConfig: ConfigContent = null;

	beforeEach(() => {
		// Before each test, reset the testConfig to a known good state.
		testConfig = deepCopy(BASE_CONFIG_PILOT);
	});

	// Most (if not all) of these tests will be creating some number of Sinon stubs. After each test, the stubs should
	// be removed so they can be recreated for the next one.
	afterEach(() => {
		Sinon.restore();
	});
	describe('Methods', () => {
		describe('#init()', () => {
			it('When neither GA nor pilot configs exist, both are created', async () => {
				const {existsStub, mkDirStub, writeFileStub} = SINON_SETUP_FUNCTIONS.NO_EXISTING_CONFIGS();
				const config = new Config();

				// INVOCATION OF TESTED METHOD
				await config.init();

				// ASSERTIONS
				expect(existsStub.calledWith(CONFIG_PILOT_PATH)).to.equal(true, 'pilot config existence check unexpectedly skipped');
				expect(existsStub.calledWith(CONFIG_PATH)).to.equal(true, 'GA config existence check unexpectedly skipped');
				expect(mkDirStub.calledWith(SFDX_SCANNER_PATH)).to.equal(true, 'Scanner path directory should have been created');
				expect(writeFileStub.calledWith(CONFIG_PATH)).to.equal(false, 'GA config should not have been created');
				expect(writeFileStub.calledWith(CONFIG_PILOT_PATH)).to.equal(true, 'pilot config should have been created');
				expect(config.configContent.currentVersion).to.equal(PACKAGE_VERSION, 'Final config should be pilot version');
			});

			it('When only GA config exists, pilot config file is copied from that', async () => {
				const {existsStub, mkDirStub, writeFileStub} = SINON_SETUP_FUNCTIONS.GA_CONFIG_ONLY();
				const config = new Config();

				// INVOCATION OF TESTED METHOD
				await config.init();

				// ASSERTIONS
				expect(existsStub.calledWith(CONFIG_PILOT_PATH)).to.equal(true, 'pilot config existence check unexpectedly skipped');
				expect(existsStub.calledWith(CONFIG_PATH)).to.equal(true, 'GA config existence check unexpectedly skipped');
				expect(mkDirStub.calledWith(SFDX_SCANNER_PATH)).to.equal(true, 'Scanner path directory should have been created');
				expect(writeFileStub.calledWith(CONFIG_PATH)).to.equal(false, 'GA config should not have been written to');
				expect(writeFileStub.calledWith(CONFIG_PILOT_PATH)).to.equal(true, 'pilot config should have been created');
				expect(config.configContent.currentVersion).to.equal(PACKAGE_VERSION, 'Final config should be pilot version');
			});

			it('Initializes using existing config file if available', async () => {
				// SETUP
				const {existsStub, writeFileStub} = SINON_SETUP_FUNCTIONS.OVERRIDE_CONFIG(testConfig);
				const config = new Config();

				// INVOCATION OF TESTED METHOD
				await config.init();

				// ASSERTIONS
				expect(existsStub.calledWith(CONFIG_PILOT_PATH)).to.equal(true, 'pilot config existence check unexpectedly skipped');
				expect(existsStub.calledWith(CONFIG_PATH)).to.equal(false, 'since pilot config exists, GA existence check should not have occurred');
				expect(writeFileStub.calledWith(CONFIG_PILOT_PATH)).to.equal(false, 'Since pilot config exists, it should not have been modified during initialization');
				const javaHome = config.getJavaHome();
				expect(javaHome).to.equal(testConfig.javaHome, 'Should have used spoofed config');
			});
		});

		describe('#upgradeConfig()', () => {
			it('When upgrade is unnecessary, no upgrade is attempted', async () => {
				// SETUP
				SINON_SETUP_FUNCTIONS.OVERRIDE_CONFIG(testConfig);
				const {upgradeToLatestStub} = SINON_SETUP_FUNCTIONS.NONUPGRADEABLE_VUM();
				const config = new Config();

				// INVOCATION OF TESTED METHOD.
				// Config.upgradeConfig() is private, but it's called within Config.init(), so we can call that instead.
				await config.init();

				// ASSERTIONS
				expect(upgradeToLatestStub.callCount).to.equal(0, 'Upgrade should not have been attempted');
			});

			it('Successful upgrades are persisted', async () => {
				// SETUP
				const {writeFileStub} = SINON_SETUP_FUNCTIONS.OVERRIDE_CONFIG(testConfig);
				const {upgradeToLatestStub} = SINON_SETUP_FUNCTIONS.SUCCESSFUL_UPGRADE_VUM();
				const config = new Config();

				// INVOCATION OF TESTED METHOD.
				// Config.upgradeConfig() is private, but it's called within Config.init(), so we can call that instead.
				await config.init();

				// ASSERTIONS
				expect(upgradeToLatestStub.callCount).to.equal(1, 'Upgrade should be attempted once');
				expect(writeFileStub.calledAfter(upgradeToLatestStub)).to.equal(true, 'Results of upgrade should be persisted');
				expect(writeFileStub.callCount).to.equal(1, 'Only one file should be written');
				expect(writeFileStub.getCall(0).args[0]).to.equal(CONFIG_PILOT_PATH, 'Should have written to the config');
			});

			it('Persists partial upgrades', async () => {
				// SETUP
				const partialUpgradeError = new VersionUpgradeError('Forced to fail', testConfig);
				const {writeFileStub} = SINON_SETUP_FUNCTIONS.OVERRIDE_CONFIG(testConfig);
				const {upgradeToLatestStub} = SINON_SETUP_FUNCTIONS.FAILED_UPGRADE_VUM(partialUpgradeError);
				const config = new Config();

				// INVOCATION OF TESTED METHOD.
				// Config.upgradeConfig() is private, but it's called within Config.init(), so we can call that instead.
				try {
					await config.init();
					fail('Expected error was not thrown');
				} catch (e) {
					// ASSERTIONS
					expect(upgradeToLatestStub.callCount).to.equal(1, 'Upgrade should be attempted once');
					expect(writeFileStub.calledAfter(upgradeToLatestStub)).to.equal(true, 'File persistence should be attempted');
					expect(writeFileStub.callCount).to.equal(2, 'Two file writes should be attempted');
					expect(writeFileStub.getCall(0).args[0]).to.equal(`${CONFIG_PILOT_PATH}.${PACKAGE_VERSION}.bak`, 'Should have written a backup file first');
					expect(writeFileStub.getCall(1).args[0]).to.equal(CONFIG_PILOT_PATH, 'Should have written partial upgrade second');
				}
			});

			it('Total failures are not persisted', async () => {
				// SETUP
				const {writeFileStub} = SINON_SETUP_FUNCTIONS.OVERRIDE_CONFIG(testConfig);
				const {upgradeToLatestStub} = SINON_SETUP_FUNCTIONS.FAILED_UPGRADE_VUM(new Error('forced to fail'));
				const config = new Config();

				// INVOCATION OF TESTED METHOD.
				// Config.upgradeConfig() is private, but it's called within Config.init(), so we can call that instead.
				try {
					await config.init();
					fail('Expected error was not thrown');
				} catch (e) {
					// ASSERTIONS
					expect(upgradeToLatestStub.callCount).to.equal(1, 'Upgrade should be attempted once');
					expect(writeFileStub.calledAfter(upgradeToLatestStub)).to.equal(true, 'File persistence should be attempted');
					expect(writeFileStub.callCount).to.equal(1, 'One file write should be attempted');
					expect(writeFileStub.getCall(0).args[0]).to.match(/\.bak$/g, 'Should have written a backup file first');
				}
			});
		});

		describe('#isEngineEnabled()', () => {
			it('Expected engines are default-enabled', async () => {
				// SETUP
				SINON_SETUP_FUNCTIONS.DEFAULT_CONFIG();
				const config = new Config();
				await config.init();

				// INVOCATION OF TESTED METHODS
				expect(await config.isEngineEnabled(ENGINE.PMD)).to.equal(true, `Engine ${ENGINE.PMD} should be enabled by default`);
				expect(await config.isEngineEnabled(ENGINE.ESLINT)).to.equal(true, `Engine ${ENGINE.ESLINT} should be enabled by default`);
				expect(await config.isEngineEnabled(ENGINE.RETIRE_JS)).to.equal(true, `Engine ${ENGINE.RETIRE_JS} should be enabled by default`);
				expect(await config.isEngineEnabled(ENGINE.ESLINT_LWC)).to.equal(false, `Engine ${ENGINE.ESLINT_LWC} should be disabled by default`);
				expect(await config.isEngineEnabled(ENGINE.ESLINT_TYPESCRIPT)).to.equal(true, `Engine ${ENGINE.ESLINT_TYPESCRIPT} should be enabled by default`);
			});
		});
	});

	// Many of the methods in Config are wrappers around a set of core functions. Rather than writing unit tests for
	// each of these methods, it's more useful to write tests that verify certain patterns of behavior.
	describe('Behaviors', () => {
		describe('Fetching of config values', () => {
			it('If config value is available, it should be returned', async () => {
				// SETUP
				const {writeFileStub} = SINON_SETUP_FUNCTIONS.OVERRIDE_CONFIG(testConfig);
				const config = new Config();
				await config.init();
				expect(writeFileStub.callCount).to.equal(0, 'No write attempts should be made yet');

				// INVOCATIONS OF TESTED METHODS
				const pmdTargetPatterns = await config.getTargetPatterns(ENGINE.PMD);
				const typescriptTargetPatterns = await config.getTargetPatterns(ENGINE.ESLINT_TYPESCRIPT);
				const retireJsEnabled = await config.isEngineEnabled(ENGINE.RETIRE_JS);

				// ASSERTIONS
				// For all cases, we expect the value that existed in the testConfig to be returned, because an empty
				// array and boolean false all count as existing.
				expect(pmdTargetPatterns).to.deep.equal(testConfig.engines[0].targetPatterns, 'Should have used existing value for PMD targetPatterns');
				expect(typescriptTargetPatterns.length).to.equal(0, 'Empty arrays count as existing values');
				// We're expecting this to be true because the false value in the config means it's not disabled.
				expect(retireJsEnabled).to.equal(true, `Boolean 'false' counts as existing value`);

				expect(writeFileStub.callCount).to.equal(0, 'No attempts to update the config file should be made');
			});

			it('If config value is undefined, it should be replaced with the default value', async () => {
				// SETUP
				const {writeFileStub} = SINON_SETUP_FUNCTIONS.OVERRIDE_CONFIG(testConfig);
				const config = new Config();
				await config.init();
				expect(writeFileStub.callCount).to.equal(0, 'No write attempts should be made yet');

				// INVOCATION OF TESTED METHODS
				const supportedLanguages = await config.getSupportedLanguages(ENGINE.PMD);
				const pmdEnabled = await config.isEngineEnabled(ENGINE.PMD);

				// ASSERTIONS
				// Since the testConfig has no definitions for PMD's supportedLanguages or disabled properties, we
				// expect the default value to be returned and written to the config.
				const expectedConfig = (config as any).getDefaultConfig().engines.find(ecc => ecc.name === ENGINE.PMD);
				const updatedConfig = (config as any).configContent.engines.find(ecc => ecc.name === ENGINE.PMD);

				expect(updatedConfig.supportedLanguages).to.deep.equal(expectedConfig.supportedLanguages, 'Default value should replace undefined array');
				expect(supportedLanguages).to.deep.equal(expectedConfig.supportedLanguages, 'Should return default value instead of undefined array');

				expect(updatedConfig.disabled).to.equal(expectedConfig.disabled, 'Default value should replace undefined boolean');
				expect(pmdEnabled).to.equal(!expectedConfig.disabled, 'Should return default value instead of undefined boolean');

				expect(writeFileStub.callCount).to.equal(2, 'Changed config should be persisted to file');
			});

			it('If config contains value of wrong type, an error should be thrown', async () => {
				// SETUP
				// Seed the testConfig with a few values that are of disallowed types. Note that the following claims
				// about "disallowed" types are the result of choices, not natural law. They should not be treated as
				// immutable or proscriptive. E.g., none of these properties can be null, but we might add ones that can.
				//
				// `disabled` must be a non-null boolean.
				testConfig.engines[0].disabled = null;
				(testConfig.engines[1] as any).disabled = 15;
				// `targetPatterns` must be a non-null array of strings.
				(testConfig.engines[0] as any).targetPatterns = null;
				(testConfig.engines[1] as any).targetPatterns = [1, 2, 3];
				SINON_SETUP_FUNCTIONS.OVERRIDE_CONFIG(testConfig);
				const config = new Config();
				await config.init();

				// INVOCATION OF TESTED METHODS
				try {
					await config.isEngineEnabled(ENGINE.PMD);
					fail('Expected error was not thrown');
				} catch (e) {
					expect(e.message).equals(configMessages.getMessage('InvalidBooleanValue', ['disabled', 'pmd', null]));
				}

				try {
					await config.isEngineEnabled(ENGINE.ESLINT_TYPESCRIPT);
					fail('Expected error was not thrown');
				} catch (e) {
					expect(e.message).equals(configMessages.getMessage('InvalidBooleanValue', ['disabled', 'eslint-typescript', 15]));
				}

				try {
					await config.getTargetPatterns(ENGINE.PMD);
					fail('Expected error was not thrown');
				} catch (e) {
					expect(e.message).equals(configMessages.getMessage('InvalidStringArrayValue', ['targetPatterns', 'pmd', null]));
				}

				try {
					await config.getTargetPatterns(ENGINE.ESLINT_TYPESCRIPT);
					fail('Expected error was not thrown');
				} catch (e) {
					expect(e.message).equals(configMessages.getMessage('InvalidStringArrayValue',
						['targetPatterns', 'eslint-typescript', String([1, 2, 3])]));
				}
			});

			it('If a whole engine is missing from config, it is replaced with default value', async () => {
				// SETUP
				const {writeFileStub} = SINON_SETUP_FUNCTIONS.OVERRIDE_CONFIG(testConfig);
				const config = new Config();
				await config.init();

				// INVOCATION OF TESTED METHOD
				const targetPatterns = await config.getTargetPatterns(ENGINE.ESLINT);

				// ASSERTIONS
				// Since ESLint was entirely missing from the testConfig, all its default values should be added.
				const expectedConfig = (config as any).getDefaultConfig().engines.find(ecc => ecc.name === ENGINE.ESLINT);
				const updatedConfig = (config as any).configContent.engines.find(ecc => ecc.name === ENGINE.ESLINT);
				expect(targetPatterns).to.deep.equal(expectedConfig.targetPatterns, 'Should use default targetPatterns');
				expect(updatedConfig).to.deep.equal(expectedConfig, 'Default config should replace missing engine config');
				expect(writeFileStub.calledWith(configFilePath)).to.equal(true, 'Changed config should be persisted');
			});
		});
	});
});
