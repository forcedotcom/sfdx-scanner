import {FileHandler} from './FileHandler';
import {Logger, LoggerLevel, SfdxError} from '@salesforce/core';
import {ENGINE, CONFIG_FILE} from '../../Constants';
import path = require('path');
import { boolean } from '@oclif/command/lib/flags';
import { Controller } from '../../Controller';

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
}

export const DEFAULT_CONFIG: ConfigContent = {
	engines: [
		{
			name: ENGINE.PMD,
			targetPatterns: [
				"**/*.cls","**/*.trigger","**/*.java","**/*.page","**/*.component","**/*.xml",
				"!**/node_modules/**","!**/*-meta.xml"
			],
			supportedLanguages: ['apex', 'vf']
		},
		{
			name: ENGINE.ESLINT,
			targetPatterns: [
				"**/*.js",
				"!**/node_modules/**",
			]
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
                "!**/node_modules/**"
			]
        }
	]
};

class TypeChecker {

	/* eslint-disable @typescript-eslint/no-explicit-any */
	stringArrayCheck = (value: any, propertyName: string, engine: ENGINE): boolean => {
		if (Array.isArray(value) && value.length > 0) {
			value.forEach((item) => {
				if (typeof item != 'string') {
					throw SfdxError.create('@salesforce/sfdx-scanner', 'Config', 'OnlyStringAllowedInStringArray', [propertyName, engine.valueOf(), String(value)]);
				}
			});
			return true;
		}
		throw SfdxError.create('@salesforce/sfdx-scanner', 'Config', 'InvalidStringArrayValue', [propertyName, engine.valueOf(), String(value)]);
	};

	/* eslint-disable @typescript-eslint/no-explicit-any */
	booleanCheck = (value: any, propertyName: string, engine: ENGINE): boolean => {
		if (value instanceof boolean) {
			return true;
		}
		throw SfdxError.create('@salesforce/sfdx-scanner', 'Config', 'InvalidBooleanValue', [propertyName, engine.valueOf(), String(value)]);
	};
}

export class Config {

	configContent!: ConfigContent;
	fileHandler!: FileHandler;
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
		this.sfdxScannerPath = Controller.getSfdxScannerPath();
		this.configFilePath = path.join(this.sfdxScannerPath, CONFIG_FILE);
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
	public isEngineEnabled(engine: ENGINE): boolean {
		if (!this.configContent.engines) {
			// Fast exit.  No definitions means all enabled.
			return true;
		}

		const e = this.getEngineConfig(engine);
		// No definition means enabled by default.  Must explicitly disable.
		return !e || !e.disabled;
	}

	public async getSupportedLanguages(engine: ENGINE): Promise<string[]> {
		return await this.getConfigValue('supportedLanguages', engine, this.typeChecker.stringArrayCheck);
	}

	public async getTargetPatterns(engine: ENGINE): Promise<string[]> {
		return await this.getConfigValue('targetPatterns', engine, this.typeChecker.stringArrayCheck);
	}

	private async getConfigValue(propertyName: string, engine: ENGINE, typeChecker: (any, string, ENGINE) => boolean): Promise<string[]> {
		let ecc = this.getEngineConfig(engine);
		// If the config specifies property, use those.
		if (ecc && ecc[propertyName] && typeChecker(ecc[propertyName], propertyName, engine)) {
			this.logger.trace(`Retrieving ${propertyName} from engine ${engine}: ${ecc[propertyName]}`);
		} else {
			// If the config doesn't specify the property, use the default value
			this.logger.trace(`Looking for missing property ${propertyName} of ${engine} in defaults`);
			ecc = await this.lookupAndUpdateToDefault(engine, ecc, propertyName);
		}

		return ecc[propertyName];
	}

	private async lookupAndUpdateToDefault(engine: ENGINE, ecc: EngineConfigContent, propertyName: string): Promise<EngineConfigContent> {
		const defaultConfig = DEFAULT_CONFIG.engines.find(e => e.name.toLowerCase() === engine.valueOf().toLowerCase());

		if (!ecc) { // if engine block doesn't exist, add a new one based on the default
			ecc = defaultConfig;
			this.addEngineToConfig(ecc);
			this.logger.warn(`Persisting missing block for engine ${engine}`);
		} else if (defaultConfig[propertyName]) { // engine block exists if we are here. Check if default has a value for property
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
			await this.createNewConfigFile(Object.assign({javaHome: this.configContent['java-home']}, DEFAULT_CONFIG) );
		}
	}

	private async createNewConfigFile(configContent: ConfigContent): Promise<void> {
		this.logger.trace(`Creating a new Config file`);
		await this.fileHandler.mkdirIfNotExists(this.sfdxScannerPath);
		await this.fileHandler.writeFile(this.configFilePath, JSON.stringify(configContent, null, 2));
		this.configContent = configContent;
	}
}

