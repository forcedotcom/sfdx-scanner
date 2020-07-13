import {expect} from 'chai';
import Sinon = require('sinon');
import path = require('path');
import {Config, DEFAULT_CONFIG} from '../../../src/lib/util/Config';
import {CONFIG_FILE, SFDX_SCANNER_PATH, ENGINE} from '../../../src/Constants'
import { FileHandler } from '../../../src/lib/util/FileHandler';
import {Messages} from '@salesforce/core';
import { fail } from 'assert';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

const configMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'Config');

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
			expect(supportedLanguages).deep.equals(DEFAULT_CONFIG.engines[0].supportedLanguages);
		});

		it('should update config with default value if config does not exist', async () => {
			const writeFileStub = Sinon.stub(FileHandler.prototype, 'writeFile').resolves();
			const config = await createConfig(testConfig, false);

			// supportedLanguages is not set in testConfig. Expect that default value is returned
			await config.getSupportedLanguages(ENGINE.PMD);
			expect(writeFileStub.calledWith(configFilePath, Sinon.match(DEFAULT_CONFIG.engines[0].supportedLanguages)));
		});

		it("should update config with default engine value if config does not contain the engine's information", async () => {
			const writeFileStub = Sinon.stub(FileHandler.prototype, 'writeFile').resolves();
			const config = await createConfig(testConfig, false);

			// testConfig does not have an eslint section
			const targetPatterns = await config.getTargetPatterns(ENGINE.ESLINT);
			const expectedTargetPatterns = DEFAULT_CONFIG.engines[1].targetPatterns;
			expect(targetPatterns).deep.equals(expectedTargetPatterns);
			expect(writeFileStub.calledWith(configFilePath, Sinon.match(DEFAULT_CONFIG.engines[1].name)));
		});

		it("should be compatible with older version of Config.json", async () => {
			const olderConfig = {"javaHome": "/my/test/java/home"};

			Sinon.stub(FileHandler.prototype, 'writeFile').resolves();

			// initialization should automatically add default engines block of config
			const config = await createConfig(olderConfig, false);

			const actualTargetPatterns = await config.getTargetPatterns(ENGINE.PMD);
			const expectedTargetPatterns = DEFAULT_CONFIG.engines[0].targetPatterns;
			expect(actualTargetPatterns).deep.equals(expectedTargetPatterns);

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

		});
	});

});

async function createConfig(testConfig: Object, stubWrite: boolean = true) {
	Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
	Sinon.stub(FileHandler.prototype, 'readFile').resolves(JSON.stringify(testConfig));
	
	if (stubWrite) {
		Sinon.stub(FileHandler.prototype, 'writeFile').resolves();
	}

	const config = new Config();
	await config.init();
	return config;
}
