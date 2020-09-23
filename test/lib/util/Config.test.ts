import {expect} from 'chai';
import Sinon = require('sinon');
import path = require('path');
import {Config, ConfigContent, EngineConfigContent} from '../../../src/lib/util/Config';
import {CONFIG_FILE, ENGINE} from '../../../src/Constants'
import { FileHandler } from '../../../src/lib/util/FileHandler';
import {Messages} from '@salesforce/core';
import { fail } from 'assert';
import { Controller } from '../../../src/Controller';
import * as TestOverrides from '../../test-related-lib/TestOverrides';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

TestOverrides.initializeTestSetup();
const SFDX_SCANNER_PATH = Controller.getSfdxScannerPath();
const configMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'Config');

class TestConfig extends Config {
	// If defined, this will supersede the value normally returned from Config#getDefaultConfig
	private defaultConfig: ConfigContent;

	constructor(defaultConfig: ConfigContent) {
		super();
		this.defaultConfig = defaultConfig;
	}

	/**
	 * Overridden to make public
	 */
	public async lookupAndUpdateToDefault(engine: ENGINE, ecc: EngineConfigContent, propertyName: string): Promise<EngineConfigContent> {
		return super.lookupAndUpdateToDefault(engine, ecc, propertyName);
	}

	public getDefaultConfig(): ConfigContent {
		return this.defaultConfig || super.getDefaultConfig();
	}
}

describe('Config.js tests', () => {

	const configFilePath = path.resolve(SFDX_SCANNER_PATH, CONFIG_FILE);

	const testConfig = {
		"engines": [
			{
				"name": "pmd",
				"targetPatterns": [
					"pattern1",
					"pattern2"
				]
			}
		],
		"javaHome": "/my/test/java/home"
	};

	describe('Verifying config behavior', () => {
		afterEach(() => {
			Sinon.restore();
		});

		it('should create new file if Config.json does not exist', async () => {
			const existsStub = Sinon.stub(FileHandler.prototype, 'exists').resolves(false);
			const makeDirStub = Sinon.stub(FileHandler.prototype, 'mkdirIfNotExists').resolves();
			const writeFileStub = Sinon.stub(FileHandler.prototype, 'writeFile').resolves();

			const config = new Config();
			await config.init();

			expect(existsStub.calledWith(configFilePath)).is.true;
			expect(makeDirStub.calledWith(SFDX_SCANNER_PATH)).is.true;
			expect(writeFileStub.calledWith(configFilePath)).is.true;
		});

		it('should initialize with existing config file if available', async () => {
			Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
			Sinon.stub(FileHandler.prototype, 'readFile').resolves(JSON.stringify(testConfig));

			const config = new Config();
			await config.init();

			const javaHome = config.getJavaHome();
			expect(javaHome).equals(testConfig.javaHome);
		});

		it('should fetch config value if available', async () => {
			const config = await createConfig(testConfig);

			// testConfig already has targetPatterns populated. Expect that the existing value is returned
			const targetPatterns = await config.getTargetPatterns(ENGINE.PMD);
			expect(targetPatterns).deep.equals(testConfig.engines[0].targetPatterns);
		});

		it('should get default value if config value is empty', async () => {
			const config = await createConfig(testConfig);

			// supportedLanguages is not set in testConfig. Expect that default value is returned
			const supportedLanguages = await config.getSupportedLanguages(ENGINE.PMD);
			expect(supportedLanguages).deep.equals(config.getDefaultConfig().engines[0].supportedLanguages);
		});

		it('should update config with default value if config does not exist', async () => {
			const writeFileStub = Sinon.stub(FileHandler.prototype, 'writeFile').resolves();
			const config = await createConfig(testConfig, false);

			// supportedLanguages is not set in testConfig. Expect that default value is returned
			await config.getSupportedLanguages(ENGINE.PMD);
			expect(writeFileStub.calledWith(configFilePath, Sinon.match(config.getDefaultConfig().engines[0].supportedLanguages)));
		});

		it("should update config with default engine value if config does not contain the engine's information", async () => {
			const writeFileStub = Sinon.stub(FileHandler.prototype, 'writeFile').resolves();
			const config = await createConfig(testConfig, false);

			// testConfig does not have an eslint section
			const targetPatterns = await config.getTargetPatterns(ENGINE.ESLINT);
			const expectedTargetPatterns = config.getDefaultConfig().engines[1].targetPatterns;
			expect(targetPatterns).deep.equals(expectedTargetPatterns);
			expect(writeFileStub.calledWith(configFilePath, Sinon.match(config.getDefaultConfig().engines[1].name)));
		});

		it("should be compatible with older version of Config.json", async () => {
			const olderConfig = {"javaHome": "/my/test/java/home"};

			Sinon.stub(FileHandler.prototype, 'writeFile').resolves();

			// initialization should automatically add default engines block of config
			const config = await createConfig(olderConfig, false);

			const actualTargetPatterns = await config.getTargetPatterns(ENGINE.PMD);
			const expectedTargetPatterns = config.getDefaultConfig().engines[0].targetPatterns;
			expect(actualTargetPatterns).deep.equals(expectedTargetPatterns);

		});

		it ('Test default enabled engines', async() => {
			const config = new Config();
			await config.init();

			expect(await config.isEngineEnabled(ENGINE.PMD)).to.be.true;
			expect(await config.isEngineEnabled(ENGINE.ESLINT)).to.be.true;
			expect(await config.isEngineEnabled(ENGINE.ESLINT_LWC)).to.be.false;
			expect(await config.isEngineEnabled(ENGINE.ESLINT_TYPESCRIPT)).to.be.true;
		});

		it ('Test lookupAndUpdateToDefault for string array', async() => {
			const defaultConfig = {
				"engines": [
					{
						"name": "pmd",
						"targetPatterns": [],
						"supportedLanguages": ["go", "kotlin"]
					}
				]
			}

			const userConfig = {
				"engines": [
					{
						"name": "pmd",
						"targetPatterns": []
					}
				]
			};

			const config = await createConfig(userConfig, true, defaultConfig);
			const updatedConfig = await config.lookupAndUpdateToDefault(ENGINE.PMD, userConfig.engines[0], 'supportedLanguages');
			expect(updatedConfig.supportedLanguages).to.have.members(["go", "kotlin"]);
		});

		it ('Test lookupAndUpdateToDefault for default false value', async() => {
			const defaultConfig = {
				"engines": [
					{
						"name": "pmd",
						"targetPatterns": [],
						"disabled": false
					}
				]
			}

			const userConfig = {
				"engines": [
					{
						"name": "pmd",
						"targetPatterns": []
					}
				]
			};

			const config = await createConfig(userConfig, true, defaultConfig);
			const updatedConfig = await config.lookupAndUpdateToDefault(ENGINE.PMD, userConfig.engines[0], 'disabled');
			expect(updatedConfig.disabled).to.be.false;
		});

		it ('Test lookupAndUpdateToDefault for default true value', async() => {
			const defaultConfig = {
				"engines": [
					{
						"name": "pmd",
						"targetPatterns": [],
						"disabled": true
					}
				]
			}

			const userConfig = {
				"engines": [
					{
						"name": "pmd",
						"targetPatterns": []
					}
				]
			};

			const config = await createConfig(userConfig, true, defaultConfig);
			const updatedConfig = await config.lookupAndUpdateToDefault(ENGINE.PMD, userConfig.engines[0], 'disabled');
			expect(updatedConfig.disabled).to.be.true;
		});

		it ('Test lookupAndUpdateToDefault for missing value throws exception', async() => {
			const defaultConfig = {
				"engines": [
					{
						"name": "pmd",
						"targetPatterns": []
					}
				]
			}

			const userConfig = {
				"engines": [
					{
						"name": "pmd",
						"targetPatterns": []
					}
				]
			};

			const config = await createConfig(userConfig, true, defaultConfig);
			try {
				await config.lookupAndUpdateToDefault(ENGINE.PMD, userConfig.engines[0], 'disabled');
				fail('Test failed. Did not throw expected error');
			} catch (error) {
				expect(error.message).equals('Developer error: no default value set for disabled of pmd engine. Or invalid property call.');
			}
		});
	});

	describe('Verifying typeCheckers', () => {
		describe('#stringArrayCheck', () => {
			afterEach(() => {
				Sinon.restore();
			});

			it('should fail for a value that is not an array', async () => {
				const invalidUserConfig = {
					"engines": [
						{
							"name": "pmd",
							"targetPatterns": 12
						}
					],
					"javaHome": "/my/test/java/home"
				};
				const config = await createConfig(invalidUserConfig);

				try {
					await config.getTargetPatterns(ENGINE.PMD);
					fail('Test failed. Did not throw expected error');
				} catch (error) {
					expect(error.message).equals(configMessages.getMessage('InvalidStringArrayValue', ['targetPatterns', 'pmd', invalidUserConfig.engines[0].targetPatterns]));
				}

			});

			it('should fail for a value that is an array but not an array of string', async () => {
				const invalidUserConfig = {
					"engines": [
						{
							"name": "pmd",
							"targetPatterns": [12, 13]
						}
					],
					"javaHome": "/my/test/java/home"
				};
				const config = await createConfig(invalidUserConfig);

				try {
					await config.getTargetPatterns(ENGINE.PMD);
					fail('Test failed. Did not throw expected error');
				} catch (error) {
					expect(error.message).equals(configMessages.getMessage('OnlyStringAllowedInStringArray', ['targetPatterns', 'pmd', String(invalidUserConfig.engines[0].targetPatterns)]));
				}

			});

			describe('#booleanCheck', () => {
				it('should succeed for a value that is false', async () => {
					const validUserConfig = {
						"engines": [
							{
								"name": "pmd",
								"disabled": false
							}
						]
					};

					const config = await createConfig(validUserConfig);

					expect(await config.isEngineEnabled(ENGINE.PMD)).to.be.true;

				});

				it('should succeed for a value that is true', async () => {
					const validUserConfig = {
						"engines": [
							{
								"name": "pmd",
								"disabled": true
							}
						]
					};

					const config = await createConfig(validUserConfig);

					expect(await config.isEngineEnabled(ENGINE.PMD)).to.be.false;

				});

				it('should fail for a value that is not a boolean', async () => {
					const invalidUserConfig = {
						"engines": [
							{
								"name": "pmd",
								"disabled": "foo"
							}
						]
					};
					const config = await createConfig(invalidUserConfig);

					try {
						await config.isEngineEnabled(ENGINE.PMD);
						fail('Test failed. Did not throw expected error');
					} catch (error) {
						expect(error.message).equals(configMessages.getMessage('InvalidBooleanValue', ['disabled', 'pmd', invalidUserConfig.engines[0].disabled]));
					}
				});
			});
		});
	});

});

async function createConfig(testConfig: Object, stubWrite: boolean = true, defaultConfig: ConfigContent = undefined): Promise<TestConfig> {
	Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
	Sinon.stub(FileHandler.prototype, 'readFile').resolves(JSON.stringify(testConfig));

	if (stubWrite) {
		Sinon.stub(FileHandler.prototype, 'writeFile').resolves();
	}

	const config = new TestConfig(defaultConfig);
	await config.init();
	return config;
}
