import {FileHandler} from './FileHandler';
import {Logger, LoggerLevel} from '@salesforce/core';
import {CONFIG_FILE, SFDX_SCANNER_PATH} from '../../Constants';
import path = require('path');

export type ConfigContent = {
	javaHome?: string;
	engines?: EngineConfigContent[];
	targetPatterns?: string[];
}

export type EngineConfigContent = {
	name: string;
	disabled?: boolean;
	targetPatterns: string[];
	supportedLanguages?: string[];
	useDefaultConfig?: boolean;
	overriddenConfigPath?: string;
}

const CONFIG_FILE_PATH = path.join(SFDX_SCANNER_PATH, CONFIG_FILE);
const DEFAULT_CONFIG: ConfigContent = {
	engines: [
		{
			name: "pmd",
			targetPatterns: [
				"**/*.cls","**/*.trigger","**/*.java","**/*.page","**/*.component","**/*.xml",
				"!**/node_modules/**","!**/*-meta.xml"
			],
			supportedLanguages: ['apex']
		},
		{
			name: "eslint",
			targetPatterns: [
				"**/*.js",
				"!**/node_modules/**",
			],
			useDefaultConfig: true
		},
		{
            name: "eslint-typescript",
            targetPatterns: [
                "**/*.ts",
                "!**/node_modules/**"
			],
			useDefaultConfig: true
        }
	]
};

export class Config {

	configContent!: ConfigContent;
	fileHandler!: FileHandler;
	private logger!: Logger;
	private initialized: boolean;

	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child('Config');

		this.fileHandler = new FileHandler();
		this.logger.setLevel(LoggerLevel.TRACE);
		await this.initializeConfig();

		this.initialized = true;
	}

	public async setJavaHome(value: string): Promise<void> {
		this.configContent.javaHome = value;
		await this.writeConfig();
	}

	public getJavaHome(): string {
		return this.configContent.javaHome;
	}

	public isEngineEnabled(name: string): boolean {
		if (!this.configContent.engines) {
			// Fast exit.  No definitions means all enabled.
			return true;
		}

		const e = this.configContent.engines.find(e => e.name === name);
		// No definition means enabled by default.  Must explicitly disable.
		return !e || e.disabled;
	}

	public getEngineConfig(name: string): EngineConfigContent {
		return this.configContent.engines.find(e => e.name === name);
	}

	public async getSupportedLanguages(engineName: string): Promise<string[]> {
		const ecc = this.getEngineConfig(engineName);
		// If the config specifies supported languages, use those.
		if (ecc.supportedLanguages && ecc.supportedLanguages.length > 0) {
			this.logger.trace(`Retrieving supported languages ${ecc.supportedLanguages} from engine ${engineName}`);
			return ecc.supportedLanguages;
		} else {
			// If the config doesn't specify supported languages, see if we have any default values.
			const defaultConfig = DEFAULT_CONFIG.engines.find(e => e.name === engineName);
			// If we find default values, persist those to the config.
			if (defaultConfig.supportedLanguages) {
				ecc.supportedLanguages = defaultConfig.supportedLanguages;
				this.logger.trace(`Persisting default languages ${ecc.supportedLanguages} for engine ${engineName}`);
				await this.writeConfig();
			}
			// Return whatever we came back with.
			return ecc.supportedLanguages;
		}
	}

	public getOverriddenConfigPath(name: string): string {
		const defaultValue = '';
		const engineConfig = this.getEngineConfig(name);

		if (this.shouldUseDefaultConfig(name)) {
			return engineConfig.overriddenConfigPath;
		}
		return defaultValue;
	}

	private shouldUseDefaultConfig(name: string): boolean {
		const engineConfig = this.getEngineConfig(name);
		const defaultValue = false;

		if (engineConfig) {
			if (engineConfig.useDefaultConfig != null) {
				return engineConfig.useDefaultConfig;
			}
		}
		return defaultValue;
	}

	private async writeConfig(): Promise<void> {
		const jsonString = JSON.stringify(this.configContent, null, 4);
		this.logger.trace(`Writing Config file with content: ${jsonString}`);
		await this.fileHandler.writeFile(CONFIG_FILE_PATH, jsonString);
	}

	private async initializeConfig(): Promise<void> {
		this.logger.trace(`Initializing Config`);
		if (!await this.fileHandler.exists(CONFIG_FILE_PATH)) {
			await this.createNewConfigFile(DEFAULT_CONFIG);
		}
		const fileContent = await this.fileHandler.readFile(CONFIG_FILE_PATH);
		this.logger.trace(`Config content to be set as ${fileContent}`);
		this.configContent = JSON.parse(fileContent);

		// TODO remove this logic before GA, as it is only necessary for short term migrations from old format.
		if (this.configContent['java-home']) {
			// Prior version.  Migrate.
			await this.createNewConfigFile(Object.assign({javaHome: this.configContent['java-home']}, DEFAULT_CONFIG) );
		}
	}

	private async createNewConfigFile(configContent: ConfigContent): Promise<void> {
		this.logger.trace(`Creating a new Config file`);
		await this.fileHandler.mkdirIfNotExists(SFDX_SCANNER_PATH);
		await this.fileHandler.writeFile(CONFIG_FILE_PATH, JSON.stringify(configContent, null, 2));
		this.configContent = configContent;
	}
}
