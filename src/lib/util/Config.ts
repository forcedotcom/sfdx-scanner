import {FileHandler} from './FileHandler';
import {Logger, LoggerLevel, Messages, SfdxError} from '@salesforce/core';
import {ENGINE, CONFIG_PILOT_FILE, CONFIG_FILE} from '../../Constants';
import path = require('path');
import { Controller } from '../../Controller';
import { uxEvents, EVENTS } from '../ScannerEvents';
import {deepCopy, booleanTypeGuard, numberTypeGuard, stringArrayTypeGuard} from './Utils';
import {VersionUpgradeError, VersionUpgradeManager} from './VersionUpgradeManager';

Messages.importMessagesDirectory(__dirname);
const configMessages = Messages.loadMessages("@salesforce/sfdx-scanner", "Config");
type GenericTypeGuard<T> = (obj: unknown) => obj is T;

export type ConfigContent = {
	currentVersion?: string;
	'java-home'?: string;
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
	// It's typically bad practice to use `require` instead of `import`, but the former is much more straightforward
	// in this case.
	// eslint-disable-next-line @typescript-eslint/no-var-requires, @typescript-eslint/no-unsafe-member-access, @typescript-eslint/no-unsafe-assignment
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
			disabled: false
		},
		{
			name: ENGINE.CPD,
			targetPatterns: [
				"**/*.cls","**/*.trigger","**/*.java","**/*.page","**/*.component","**/*.xml",
				"!**/node_modules/**","!**/*-meta.xml"
			],
			disabled: true,
			minimumTokens: 100
		},
		{
			name: ENGINE.SFGE,
			targetPatterns: [
				"**/*.cls",
				"**/*.trigger"
			],
			disabled: false
		}
	]
};

export class Config {

	configContent!: ConfigContent;
	fileHandler!: FileHandler;
	versionUpgradeManager!: VersionUpgradeManager;
	private sfdxScannerPath: string;
	private configPilotFilePath: string;
	private configFilePath: string;
	private logger!: Logger;
	private initialized: boolean;

	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child('Config');

		this.fileHandler = new FileHandler();
		this.versionUpgradeManager = new VersionUpgradeManager();
		this.sfdxScannerPath = Controller.getSfdxScannerPath();
		this.configPilotFilePath = path.join(this.sfdxScannerPath, CONFIG_PILOT_FILE);
		this.configFilePath = path.join(this.sfdxScannerPath, CONFIG_FILE);
		this.logger.setLevel(LoggerLevel.TRACE);
		await this.initializeConfig();

		this.initialized = true;
		// Upgrades MUST happen AFTER the config is marked as initialized, to prevent any unexpected recursion.
		await this.upgradeConfig();
	}

	private async upgradeConfig(): Promise<void> {
		if (this.versionUpgradeManager.upgradeRequired(this.configContent.currentVersion)) {
			// Start by cloning the existing config, so we have it if we need to create a back-up later.
			// Also, determine whether a back-up should be created always or just for failed upgrades,
			// and determine where such a back-up would go.
			const existingConfig = deepCopy(this.configContent);
			let shouldCreateBackup = this.versionUpgradeManager.versionNecessitatesBackup(this.configContent.currentVersion);
			const backupFileName = `${CONFIG_FILE}.${existingConfig.currentVersion || 'pre-2.7.0'}.bak`;

			let upgradeErrorMessage: string = null;
			let persistConfig = true;
			try {
				await this.versionUpgradeManager.upgradeToLatest(this.configContent, this.configContent.currentVersion);
			} catch (e) {
				// If the error included a partially-upgraded config, we should switch our config to that, so the partial
				// upgrade can be persisted.
				if (e instanceof VersionUpgradeError) {
					this.configContent = e.getLastSafeConfig();
				} else {
					// Otherwise, we should assume that the configuration is entirely unsafe, and prevent any data persistence.
					persistConfig = false;
				}
				// Since there was an error, we definitely want to create a back-up.
				shouldCreateBackup = true;
				// Hang onto the error so we can rethrow it.
				upgradeErrorMessage = e instanceof Error ? e.message : e as string;
			}

			// Persist a back-up of the original config if one is needed.
			if (shouldCreateBackup) {
				await this.fileHandler.writeFile(path.join(this.sfdxScannerPath, backupFileName), JSON.stringify(existingConfig, null, 4));
			}

			// Persist any changes that were successfully made.
			if (persistConfig) {
				await this.writeConfig();
			}

			// If an error was thrown during the upgrade, we'll want to modify the error message and rethrow it.
			if (upgradeErrorMessage) {
				throw SfdxError.create('@salesforce/sfdx-scanner',
					'Config',
					'UpgradeFailureTroubleshooting',
					[upgradeErrorMessage, backupFileName, this.configFilePath]
				);
			}
		}
	}

	public getConfigFilePath(): string {
		return this.configFilePath;
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
		return this.getConfigValue<boolean>(propertyName, engine, booleanTypeGuard, 'InvalidBooleanValue');
	}

	protected getNumberConfigValue(propertyName: string, engine: ENGINE): Promise<number> {
		return this.getConfigValue<number>(propertyName, engine, numberTypeGuard, 'InvalidNumberValue');
	}

	protected getStringArrayConfigValue(propertyName: string, engine: ENGINE): Promise<string[]> {
		return this.getConfigValue<string[]>(propertyName, engine, stringArrayTypeGuard, 'InvalidStringArrayValue');
	}

	private async getConfigValue<T>(propertyName: string, engine: ENGINE, typeChecker: GenericTypeGuard<T>, errTemplate: string): Promise<T> {
		let ecc = this.getEngineConfig(engine);
		// If the config for this engine is undefined or the requested property is undefined, update to the default value
		// and persist it for future use. Note: We're intentionally distinguishing between null and undefined, in case
		// we want to allow nullable config properties in the future.
		if (!ecc || ecc[propertyName] === undefined) {
			this.logger.trace(`Looking for missing config property ${propertyName} of engine ${engine} in default configs`);
			ecc = await this.lookupAndUpdateToDefault(engine, ecc, propertyName);
		} else {
			this.logger.trace(`Retrieving property ${propertyName} from existing config for engine ${engine}`);
		}
		// At this point, the config definitely has a value, so we can typecheck it and return it.
		const propertyValue = ecc[propertyName] as unknown;
		if (typeChecker(propertyValue)) {
			this.logger.trace(`Config property ${propertyName} for engine ${engine} has value ${String(propertyValue)}`);
			return propertyValue;
		} else {
			throw SfdxError.create('@salesforce/sfdx-scanner', 'Config', errTemplate, [propertyName, this.configFilePath, engine.valueOf(), String(propertyValue)]);
		}
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
			// We don't know what the type of the property we're setting is, nor do we particularly care in this moment.
			const defaultValue = defaultConfig[propertyName] as unknown;
			ecc[propertyName] = defaultValue;
			this.logger.warn(`Persisting default values ${defaultValue.toString()} for engine ${engine}`);
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
		if (await this.fileHandler.exists(this.configFilePath)) {
			// If there's an existing config, we can use that.
			this.logger.trace(`Getting existing config from ${this.configFilePath}`);
			const configFileContent = await this.fileHandler.readFile(this.configFilePath);
			this.configContent = JSON.parse(configFileContent) as ConfigContent;
		} else if (await this.fileHandler.exists(this.configPilotFilePath)) {
			// If there's no existing GA config, but there's a pilot config, that means
			// that the user was previously on the pilot version, and is running GA for
			// the first time. To avoid unexpected behavior changes, we'll copy the pilot
			// config as the source for the GA config.
			this.logger.trace(`Creating GA config by copying pilot config`);
			uxEvents.emit(EVENTS.WARNING_ALWAYS, configMessages.getMessage("GeneratingConfigFromPilot", [
				this.configPilotFilePath
			]));
			const configFileContent = await this.fileHandler.readFile(this.configPilotFilePath);
			this.configContent =  JSON.parse(configFileContent) as ConfigContent;
			await this.createNewConfigFile(this.configContent, this.configFilePath);
		} else {
			// If there's neither a pilot nor GA config, then it's a fresh install.
			// Generate the config from the defaults.
			this.logger.trace(`Creating GA config from defaults`);
			this.configContent = DEFAULT_CONFIG;
			await this.createNewConfigFile(DEFAULT_CONFIG, this.configFilePath);
		}
	}

	private async createNewConfigFile(configContent: ConfigContent, configFilePath: string): Promise<void> {
		this.logger.trace(`Creating a new Config file`);
		await this.fileHandler.mkdirIfNotExists(this.sfdxScannerPath);
		await this.fileHandler.writeFile(configFilePath, JSON.stringify(configContent, null, 2));
	}
}

