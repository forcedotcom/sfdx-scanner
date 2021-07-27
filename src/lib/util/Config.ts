import {FileHandler} from './FileHandler';
import {Logger, LoggerLevel, SfdxError} from '@salesforce/core';
import {ENGINE, CONFIG_FILE} from '../../Constants';
import path = require('path');
import { Controller } from '../../Controller';
import {deepCopy} from './Utils';
import {VersionUpgradeError, VersionUpgradeManager} from './VersionUpgradeManager';

export type ConfigContent = {
	currentVersion?: string;
	javaHome?: string;
	engines?: EngineConfigContent[];
	targetPatterns?: string[];
}

export type EngineConfigContent = {
	name: string;
	disabled?: boolean;
	targetPatterns: string[];
	supportedLanguages?: string[];
	minimumTokens?: number;
}

const DEFAULT_CONFIG: ConfigContent = {
	currentVersion: require('../../../package.json').version,
	engines: [
		{
			name: ENGINE.PMD,
			targetPatterns: [
				"**/*.cls","**/*.trigger","**/*.java","**/*.page","**/*.component","**/*.xml",
				"!**/node_modules/**","!**/*-meta.xml"
			],
			supportedLanguages: ['apex', 'vf'],
			disabled: false
		},
		{
			name: ENGINE.ESLINT,
			targetPatterns: [
				"**/*.js",
				"!**/node_modules/**",
				"!**/bower_components/**"
			],
			disabled: false
		},
		{
			name: ENGINE.ESLINT_LWC,
			targetPatterns: [
					"**/*.js",
					"!**/node_modules/**",
			],
			disabled: true
		},
		{
            name: ENGINE.ESLINT_TYPESCRIPT,
            targetPatterns: [
                "**/*.ts",
                "!**/node_modules/**",
				"!**/bower_components/**"
			],
			disabled: false
        },
		{
			name: ENGINE.RETIRE_JS,
			targetPatterns: [],
			disabled: true
		},
		{
			name: ENGINE.CPD,
			targetPatterns: [
				"**/*.cls","**/*.page","**/*.component","**/*.trigger"
			],
			disabled: true,
			minimumTokens: 100
		}
	]
};

class TypeChecker {

	/* eslint-disable @typescript-eslint/no-explicit-any */
	stringArrayCheck = (value: any, propertyName: string, engine: ENGINE): boolean => {
		if (Array.isArray(value)) {
			if (value.length > 0) {
				value.forEach((item) => {
					if (typeof item != 'string') {
						throw SfdxError.create('@salesforce/sfdx-scanner', 'Config', 'OnlyStringAllowedInStringArray', [propertyName, engine.valueOf(), String(value)]);
					}
				});
				return true;
			} else {
				return true;
			}
		} else {
			throw SfdxError.create('@salesforce/sfdx-scanner', 'Config', 'InvalidStringArrayValue', [propertyName, engine.valueOf(), String(value)]);
		}
	};

	/* eslint-disable @typescript-eslint/no-explicit-any */
	booleanCheck = (value: any, propertyName: string, engine: ENGINE): boolean => {
		if (typeof value === 'boolean') {
			return true;
		}
		throw SfdxError.create('@salesforce/sfdx-scanner', 'Config', 'InvalidBooleanValue', [propertyName, engine.valueOf(), String(value)]);
	};

	numberCheck = (value: any, propertyName: string, engine: ENGINE): boolean => {
		if (typeof value === 'number') {
			return true;
		}
		throw SfdxError.create('@salesforce/sfdx-scanner', 'Config', 'InvalidNumberValue', [propertyName, engine.valueOf(), String(value)]);
	};
}

export class Config {

	configContent!: ConfigContent;
	fileHandler!: FileHandler;
	versionUpgradeManager!: VersionUpgradeManager;
	private typeChecker: TypeChecker;
	private sfdxScannerPath: string;
	private configFilePath: string;
	private logger!: Logger;
	private initialized: boolean;

	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child('Config');

		this.typeChecker = new TypeChecker();
		this.fileHandler = new FileHandler();
		this.versionUpgradeManager = new VersionUpgradeManager();
		this.sfdxScannerPath = Controller.getSfdxScannerPath();
		this.configFilePath = path.join(this.sfdxScannerPath, CONFIG_FILE);
		this.logger.setLevel(LoggerLevel.TRACE);
		await this.initializeConfig();

		this.initialized = true;
		// Upgrades MUST happen AFTER the config is marked as initialized, to prevent any unexpected recursion.
		await this.upgradeConfig();
	}

	private async upgradeConfig(): Promise<void> {
		if (this.versionUpgradeManager.upgradeRequired(this.configContent.currentVersion)) {
			// Start by creating a copy of the existing config, so we have it if we need it to create a backup file.
			// Also determine where such a file would go.
			const existingConfig: ConfigContent = JSON.parse(JSON.stringify(this.configContent));
			const backupFileName = `${CONFIG_FILE}.${existingConfig.currentVersion || 'pre-2.7.0'}.bak`;

			let upgradeError = null;
			let persistConfig = true;
			try {
				await this.versionUpgradeManager.upgradeToLatest(this.configContent, this.configContent.currentVersion);
			} catch (e) {
				// If the error included a partially-upgraded config, we should switch our config to that, so the partial
				// upgrade can be persisted.
				if (e instanceof VersionUpgradeError) {
					this.configContent = e.getLastSafeConfig();
				} else {
					// Otherwise, we should assume that the configuration is entirely unsafe, and prevent any data persistance.
					persistConfig = false;
				}
				// Persist the original config as a backup file so the user doesn't lose it.
				await this.fileHandler.writeFile(path.join(this.sfdxScannerPath, backupFileName), JSON.stringify(existingConfig, null, 4));
				// Hang onto the error so we can rethrow it.
				upgradeError = e;
			}

			// Persist any changes that were successfully made.
			if (persistConfig) {
				await this.writeConfig();
			}

			// If an error was thrown during the upgrade, we'll want to modify the error message and rethrow it.
			if (upgradeError) {
				throw SfdxError.create('@salesforce/sfdx-scanner',
					'Config',
					'UpgradeFailureTroubleshooting',
					[upgradeError.message || upgradeError, backupFileName, this.configFilePath]
				);
			}
		}
	}

	public async setJavaHome(value: string): Promise<void> {
		this.configContent.javaHome = value;
		await this.writeConfig();
	}

	public getJavaHome(): string {
		return this.configContent.javaHome;
	}

	public async isEngineEnabled(engine: ENGINE): Promise<boolean> {
		if (!this.configContent.engines) {
			// Fast exit.  No definitions means all enabled.
			return true;
		}

		return !(await this.getBooleanConfigValue('disabled', engine));
	}

	public getSupportedLanguages(engine: ENGINE): Promise<string[]> {
		return this.getStringArrayConfigValue('supportedLanguages', engine);
	}

	public getTargetPatterns(engine: ENGINE): Promise<string[]> {
		return this.getStringArrayConfigValue('targetPatterns', engine);
	}

	public getMinimumTokens(engine: ENGINE): Promise<number> {
		return this.getNumberConfigValue('minimumTokens', engine);
	}

	protected getBooleanConfigValue(propertyName: string, engine: ENGINE): Promise<boolean> {
		return this.getConfigValue<boolean>(propertyName, engine, this.typeChecker.booleanCheck);
	}

	protected getNumberConfigValue(propertyName: string, engine: ENGINE): Promise<number> {
		return this.getConfigValue<number>(propertyName, engine, this.typeChecker.numberCheck);
	}

	protected getStringArrayConfigValue(propertyName: string, engine: ENGINE): Promise<string[]> {
		return this.getConfigValue<string[]>(propertyName, engine, this.typeChecker.stringArrayCheck);
	}

	private async getConfigValue<T>(propertyName: string, engine: ENGINE, typeChecker: (any, string, ENGINE) => boolean): Promise<T> {
		let ecc = this.getEngineConfig(engine);
		// If the config specifies property, use those.
		// Intentionally distinguishing null from undefined in case we want to allow nullable config values in the future.
		// TODO: Look into possibly using typeguards, see discussion here https://github.com/forcedotcom/sfdx-scanner/pull/222/files#r497080143
		if (ecc && (ecc[propertyName] !== undefined) && typeChecker(ecc[propertyName], propertyName, engine)) {
			this.logger.trace(`Retrieving ${propertyName} from engine ${engine}: ${ecc[propertyName]}`);
		} else {
			// If the config doesn't specify the property, use the default value
			this.logger.trace(`Looking for missing property ${propertyName} of ${engine} in defaults`);
			ecc = await this.lookupAndUpdateToDefault(engine, ecc, propertyName);
		}

		return ecc[propertyName];
	}

	/**
	 * Get the default ConfigContent. This value will be used as-is when a Config.json file doesn't exist, or
	 * its contents will be merged with an existing Config.json file in cases where new values are added after the
	 * Config.json file has been written.
	 */
	protected getDefaultConfig(): ConfigContent {
		return DEFAULT_CONFIG;
	}

	protected async lookupAndUpdateToDefault(engine: ENGINE, ecc: EngineConfigContent, propertyName: string): Promise<EngineConfigContent> {
		const defaultConfig = this.getDefaultConfig().engines.find(e => e.name.toLowerCase() === engine.valueOf().toLowerCase());

		if (!ecc) { // if engine block doesn't exist, add a new one based on the default
			ecc = defaultConfig;
			this.addEngineToConfig(ecc);
			this.logger.warn(`Persisting missing block for engine ${engine}`);
		} else if (defaultConfig[propertyName] !== undefined) { // engine block exists if we are here. Check if default has a value for property
			ecc[propertyName] = defaultConfig[propertyName];
			this.logger.warn(`Persisting default values ${ecc[propertyName]} for engine ${engine}`);
		} else {
			// if we are here, this is a developer problem
			throw new Error(`Developer error: no default value set for ${propertyName} of ${engine} engine. Or invalid property call.`);
		}

		// update contents in Config file to latest configContent value
		await this.writeConfig();
		return ecc;
	}

	private addEngineToConfig(ecc: EngineConfigContent): void {
		if (!this.configContent.engines) {
			this.configContent.engines = [];
		}
		this.configContent.engines.push(ecc);
	}

	private getEngineConfig(name: ENGINE): EngineConfigContent {
		return this.configContent.engines.find(e => e.name === name);
	}

	private async writeConfig(): Promise<void> {
		const jsonString = JSON.stringify(this.configContent, null, 4);
		this.logger.trace(`Writing Config file with content: ${jsonString}`);
		await this.fileHandler.writeFile(this.configFilePath, jsonString);
	}

	private async initializeConfig(): Promise<void> {
		this.logger.trace(`Initializing Config`);
		if (!await this.fileHandler.exists(this.configFilePath)) {
			await this.createNewConfigFile(DEFAULT_CONFIG);
		}
		const fileContent = await this.fileHandler.readFile(this.configFilePath);
		this.logger.trace(`Config content to be set as ${fileContent}`);
		this.configContent = JSON.parse(fileContent);

		// TODO remove this logic before GA, as it is only necessary for short term migrations from old format.
		if (!this.configContent['engines'] && this.configContent['javaHome']) {
			// Prior version.  Migrate.
			await this.createNewConfigFile(Object.assign({javaHome: this.configContent['java-home']}, deepCopy(DEFAULT_CONFIG)) );
		}
	}

	private async createNewConfigFile(configContent: ConfigContent): Promise<void> {
		this.logger.trace(`Creating a new Config file`);
		await this.fileHandler.mkdirIfNotExists(this.sfdxScannerPath);
		await this.fileHandler.writeFile(this.configFilePath, JSON.stringify(configContent, null, 2));
		this.configContent = configContent;
	}
}

