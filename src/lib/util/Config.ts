import {FileHandler} from './FileHandler';
import {Logger, LoggerLevel, SfdxError} from '@salesforce/core';
import {ENGINE, CONFIG_FILE, SFDX_SCANNER_PATH} from '../../Constants';
import path = require('path');
import { boolean } from '@oclif/command/lib/flags';

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
export const DEFAULT_CONFIG: ConfigContent = {
	engines: [
		{
			name: ENGINE.PMD,
			targetPatterns: [
				"**/*.cls","**/*.trigger","**/*.java","**/*.js","**/*.page","**/*.component","**/*.xml",
				"!**/node_modules/**","!**/*-meta.xml"
			],
			supportedLanguages: ['apex', 'javascript']
		},
		{
			name: ENGINE.ESLINT,
			targetPatterns: [
				"**/*.js",
				"!**/node_modules/**",
			],
			useDefaultConfig: true
		},
		{
            name: ENGINE.ESLINT_TYPESCRIPT,
            targetPatterns: [
                "**/*.ts",
                "!**/node_modules/**"
			],
			useDefaultConfig: true
        }
	]
};

class TypeChecker {

	/* eslint-disable @typescript-eslint/no-explicit-any */
	stringArrayCheck(value: any, propertyName: string, engine: ENGINE): boolean {
		if (Array.isArray(value) && value.length > 0) {
			value.forEach((item) => {
				if (typeof item != 'string') {
					throw SfdxError.create('@salesforce/sfdx-scanner', 'Config', 'OnlyStringAllowedInStringArray', [propertyName, engine.valueOf(), String(value)]);
				}
			});
			return true;
		} 
		throw SfdxError.create('@salesforce/sfdx-scanner', 'Config', 'InvalidStringArrayValue', [propertyName, engine.valueOf(), String(value)]);
	}

	/* eslint-disable @typescript-eslint/no-explicit-any */
	booleanCheck(value: any, propertyName: string, engine: ENGINE): boolean {
		if (value instanceof boolean) {
			return true;
		}
		throw SfdxError.create('@salesforce/sfdx-scanner', 'Config', 'InvalidBooleanValue', [propertyName, engine.valueOf(), String(value)]);
	}
}

export class Config {

	configContent!: ConfigContent;
	fileHandler!: FileHandler;
	private typeChecker: TypeChecker;
	private logger!: Logger;
	private initialized: boolean;

	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child('Config');

		this.typeChecker = new TypeChecker();
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

	// FIXME: Not supported yet - the logic is not hooked up to the actual call
	// Leaving this as-is instead of moving to getConfigValue() style
	public isEngineEnabled(name: string): boolean {
		if (!this.configContent.engines) {
			// Fast exit.  No definitions means all enabled.
			return true;
		}

		const e = this.configContent.engines.find(e => e.name === name);
		// No definition means enabled by default.  Must explicitly disable.
		return !e || e.disabled;
	}

	public async getSupportedLanguages(engine: ENGINE): Promise<string[]> {
		const value = await this.getConfigValue('supportedLanguages', engine, this.typeChecker.stringArrayCheck);
		return value as Array<string>;
	}

	public async getTargetPatterns(engine: ENGINE): Promise<string[]> {
		const value = await this.getConfigValue('targetPatterns', engine, this.typeChecker.stringArrayCheck);
		return value as Array<string>;
	}

	private async getConfigValue(propertyName: string, engine: ENGINE, typeChecker: (any, string, ENGINE) => boolean): Promise<string[]> {
		let ecc = this.getEngineConfig(engine);
		// If the config specifies property, use those.
		if (ecc && ecc[propertyName] && typeChecker(ecc[propertyName], propertyName, engine)) {
			this.logger.trace(`Retrieving ${propertyName} from engine ${engine}: ${ecc[propertyName]}`);
			return ecc[propertyName];
		} else {
			// If the config doesn't specify the property, see if we have any default values.
			const defaultConfig = DEFAULT_CONFIG.engines.find(e => e.name.toLowerCase() === engine.valueOf().toLowerCase());
			// If we find default values, persist those to the config.
			if (!ecc) {
				ecc = defaultConfig;
				this.logger.warn(`Persisting missing block for engine ${engine}`);
				await this.writeConfig();
			} else if (defaultConfig[propertyName]) {
				ecc[propertyName] = defaultConfig[propertyName];
				this.logger.warn(`Persisting default values ${ecc[propertyName]} for engine ${engine}`);
				await this.writeConfig();
			} else {
				throw new Error(`Developer error: no default value set for ${propertyName} of ${engine} engine. Or invalid property call.`)
			}
			// Return whatever we came back with.
			return ecc[propertyName];
		}
	}

	private getEngineConfig(name: ENGINE): EngineConfigContent {
		return this.configContent.engines.find(e => e.name === name);
	}

	// TODO: remove this method and the associated config
	public getOverriddenConfigPath(name: ENGINE): string {
		const defaultValue = '';
		const engineConfig = this.getEngineConfig(name);

		if (this.shouldUseDefaultConfig(name)) {
			return engineConfig.overriddenConfigPath;
		}
		return defaultValue;
	}

	// TODO: remove this method and the associated config
	private shouldUseDefaultConfig(name: ENGINE): boolean {
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

