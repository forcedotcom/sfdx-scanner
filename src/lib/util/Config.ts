import {FileHandler} from './FileHandler';
import {Logger, LoggerLevel} from '@salesforce/core';
import {CONFIG_FILE, SFDX_SCANNER_PATH} from '../../Constants';
import path = require('path');

type ConfigContent = {
	javaHome?: string;
	engines?: EngineConfigContent[];
	targetDefaultPatterns?: string[];
}

type EngineConfigContent = {
	name: string;
	disabled?: boolean;
}

const CONFIG_FILE_PATH = path.join(SFDX_SCANNER_PATH, CONFIG_FILE);
const DEFAULT_CONFIG: ConfigContent = {
	engines: [],
	targetDefaultPatterns: [
		"!node_modules/**"
	]
}

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

	public getDefaultTargetPatterns(): string[] {
		return this.configContent.targetDefaultPatterns;
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
		try {
			this.configContent = JSON.parse(fileContent);

			// TODO remove this logic before GA, as it is only necessary for short term migrations from old format.
			if (this.configContent['java-home']) {
				// Prior version.  Migrate.
				await this.createNewConfigFile(Object.assign({javaHome: this.configContent['java-home']}, DEFAULT_CONFIG) );
			}
		} catch (e) {
			// Should never happen, only in odd cases.
			const oldFile = await this.uniqueFilenameFor(CONFIG_FILE_PATH + '.bak');
			await this.fileHandler.writeFile(oldFile, fileContent);
			this.logger.error(`Problem loading existing configuration file.  Backed up original config to ${oldFile} and created ${CONFIG_FILE_PATH} fresh.`)
		}
	}

	private async createNewConfigFile(configContent: ConfigContent): Promise<void> {
		this.logger.trace(`Creating a new Config file`);
		await this.fileHandler.mkdirIfNotExists(SFDX_SCANNER_PATH);
		await this.fileHandler.writeFile(CONFIG_FILE_PATH, JSON.stringify(configContent, null, 2));
		this.configContent = configContent;
	}

	private async uniqueFilenameFor(filename: string): Promise<string> {
		let ext = '';
		for(let i = 1; i < 100; i++) {
			if(await this.fileHandler.exists(filename + ext)) {
				ext = String(i);
			} else {
				return filename + ext;
			}
		}
		throw new Error(`Something is very wrong.  Please clear out your backups of ${filename} and try again`);
	}
}
