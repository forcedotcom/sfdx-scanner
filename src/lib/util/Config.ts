import {FileHandler} from './FileHandler';
import {AsyncCreatable} from '@salesforce/kit';
import {Logger, LoggerLevel} from '@salesforce/core';
import {CONFIG, SFDX_SCANNER_PATH} from '../../Constants';
import path = require('path');

enum ConfigKey {
	JAVA_HOME = 'java-home'
}

const CONFIG_FILE = path.join(SFDX_SCANNER_PATH, CONFIG);

export class Config extends AsyncCreatable {

	configContent!: JSON;
	fileHandler!: FileHandler;
	private logger!: Logger;

	protected async init(): Promise<void> {
		this.fileHandler = new FileHandler();
		this.logger = await Logger.child('Config');
		this.logger.setLevel(LoggerLevel.TRACE);
		await this.initializeConfig();
	}

	public async setJavaHome(value: string): Promise<void> {
		this.setKey(ConfigKey.JAVA_HOME, value);
		await this.writeConfig();
	}

	public getJavaHome(): string {
		return this.getKey(ConfigKey.JAVA_HOME) as string;
	}

	private setKey(key: ConfigKey, value: string): void {
		this.logger.trace(`Setting key ${key} to value ${value}`);
		this.configContent[key] = value;
	}

	private getKey(key: ConfigKey): unknown {
		return this.configContent[key];
	}

	private async writeConfig(): Promise<void> {
		const jsonString = JSON.stringify(this.configContent, null, 4);
		this.logger.trace(`Writing Config file with content: ${jsonString}`);
		await this.fileHandler.writeFile(CONFIG_FILE, jsonString);
	}

	private async initializeConfig(): Promise<void> {
		this.logger.trace(`Initializing Config`);
		try {
			await this.fileHandler.stats(CONFIG_FILE);
		} catch (error) {
			await this.createNewConfigFile();
		}
		const fileContent = await this.fileHandler.readFile(CONFIG_FILE);
		this.logger.trace(`Config content to be set as ${fileContent}`);
		this.configContent = JSON.parse(fileContent);
	}

	private async createNewConfigFile(): Promise<void> {
		this.logger.trace(`Creating a new Config file`);
		await this.fileHandler.mkdirIfNotExists(SFDX_SCANNER_PATH);
		await this.fileHandler.writeFile(CONFIG_FILE, '{}');
	}
}
