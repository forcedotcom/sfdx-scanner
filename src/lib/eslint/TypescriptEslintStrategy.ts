import * as path from 'path';
import { EslintStrategy } from "./BaseEslintEngine";
import {FileHandler} from '../util/FileHandler';
import {Config} from '../util/Config';
import {ENGINE, LANGUAGE} from '../../Constants';
import {Controller} from '../../ioc.config';
import { Logger, Messages, SfdxError } from '@salesforce/core';
import { OutputProcessor } from '../pmd/OutputProcessor';

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'TypescriptEslintStrategy');

export enum TYPESCRIPT_ENGINE_OPTIONS {
	// Specify the location of the tsconfig.json file
	TSCONFIG = 'tsconfig'
}

const ES_PLUS_TS_CONFIG = {
	"parser": "@typescript-eslint/parser",
	"extends": [
		"eslint:recommended",
		"plugin:@typescript-eslint/recommended",
		"plugin:@typescript-eslint/eslint-recommended"
	],
	"parserOptions": {
		"sourceType": "module",
		"ecmaVersion": 2018,
	},
	"plugins": [
		"@typescript-eslint"
	],
	"ignorePatterns": [
		"lib/**",
		"node_modules/**"
	],
	"useEslintrc": false, // TODO derive from existing eslintrc if found and desired
	"resolvePluginsRelativeTo": __dirname, // Use the plugins found in the sfdx scanner installation directory
	"cwd": __dirname // Use the parser found in the sfdx scanner installation
};

const TS_CONFIG = 'tsconfig.json';

export class TypescriptEslintStrategy implements EslintStrategy {
	private static ENGINE_NAME = ENGINE.ESLINT_TYPESCRIPT.valueOf();
	private static LANGUAGES = [LANGUAGE.TYPESCRIPT];

	private initialized: boolean;
	private logger: Logger;
	private fileHandler: FileHandler;
	private config: Config;
	private outputProcessor: OutputProcessor;

	async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getName());
		this.fileHandler = new FileHandler();
		this.config = await Controller.getConfig();
		this.outputProcessor = await OutputProcessor.create({});
		this.initialized = true;
	}

	isEnabled(): boolean {
		return this.config.isEngineEnabled(this.getName());
	}

	getName(): string {
		return TypescriptEslintStrategy.ENGINE_NAME;
	}

	isRuleKeySupported(): boolean {
		// Javascript rules written for only eslint work for typescript as well,
		// though they require the parser information that is specific to typescript.
		return true;
	}

	/* eslint-disable @typescript-eslint/no-explicit-any */
	getCatalogConfig(): Record<string, any> {
		return ES_PLUS_TS_CONFIG;
	}

	getLanguages(): string[] {
		return TypescriptEslintStrategy.LANGUAGES;
	}

	async getTargetPatterns(): Promise<string[]> {
		return this.config.getTargetPatterns(ENGINE.ESLINT_TYPESCRIPT);
	}

	filterUnsupportedPaths(paths: string[]): string[] {
		const filteredPaths = paths.filter(p => p.endsWith(".ts"));
		this.logger.trace(`Input paths: ${paths}, Filtered paths: ${filteredPaths}`);
		return filteredPaths;
	}

	/* eslint-disable @typescript-eslint/no-explicit-any */
	async getRunConfig(engineOptions: Map<string, string>): Promise<Record<string, any>> {
		const tsconfigPath = await this.findTsconfig(engineOptions);

		// Alert the user we found a config file, if --verbose
		this.alertUser(tsconfigPath);

		const config = {};

		Object.assign(config, ES_PLUS_TS_CONFIG);

		// Enable typescript by registering its project config file
		config["parserOptions"].project = tsconfigPath;

		this.logger.trace(`Using config for run: ${JSON.stringify(config)}`);

		return config;
	}

	private alertUser(tsconfigPath: string): void {
		const event = {
			messageKey: 'info.usingEngineConfigFile', args: [tsconfigPath], type: 'INFO', handler: 'UX', verbose: true, time: Date.now()
		};
		this.outputProcessor.emitEvents([event]);
	}

	convertLintMessage(fileName: string, message: string):string {
		if (message.startsWith('Parsing error: "parserOptions.project" has been set for @typescript-eslint/parser.\nThe file does not match your project config') &&
			message.endsWith('The file must be included in at least one of the projects provided.')) {
			return messages.getMessage('FileNotIncludedByTsConfig', [fileName, TS_CONFIG]);
		} else {
			return message;
		}
	}

	async findTsconfig(engineOptions: Map<string, string>): Promise<string> {
		const cwd = path.resolve();
		const tsconfigFromWorkingDirectory = await this.checkWorkingDirectoryForTsconfig();
		const tsConfigFromOptions = await this.checkEngineOptionsForTsconfig(engineOptions);

		if (!tsconfigFromWorkingDirectory && !tsConfigFromOptions) {
			// Unable to find in current directory and not specified in the engineOptions
			throw SfdxError.create('@salesforce/sfdx-scanner', 'TypescriptEslintStrategy', 'MissingTsConfigFromCwd',
				[TS_CONFIG, cwd, TS_CONFIG]);
		} else if (tsconfigFromWorkingDirectory && tsConfigFromOptions) {
			// Found in current directory and specified in the engineOptions
			throw SfdxError.create('@salesforce/sfdx-scanner', 'TypescriptEslintStrategy', 'MultipleTsConfigs',
				[TS_CONFIG, tsconfigFromWorkingDirectory, tsConfigFromOptions]);
		}

		const foundTsConfig = tsconfigFromWorkingDirectory || tsConfigFromOptions;

		this.logger.trace(`Using ${TS_CONFIG} from ${foundTsConfig}`);

		return foundTsConfig;
	}

	protected async checkEngineOptionsForTsconfig(engineOptions: Map<string, string>): Promise<string> {
		const tsConfigFromOptions = engineOptions.get(TYPESCRIPT_ENGINE_OPTIONS.TSCONFIG);

		if (tsConfigFromOptions != null) {
			if (!(await this.fileHandler.exists(tsConfigFromOptions))) {
				// Specified in the engineOptions but it isn't a file
				throw SfdxError.create('@salesforce/sfdx-scanner', 'TypescriptEslintStrategy', 'NotAFileTsConfigFromOptions',
					[TS_CONFIG, tsConfigFromOptions]);
			} else if (path.basename(tsConfigFromOptions).toLowerCase() !== TS_CONFIG) {
				// Found the file, but it's not named tsconfig.json
				throw SfdxError.create('@salesforce/sfdx-scanner', 'TypescriptEslintStrategy', 'InvalidNameTsConfigFromOptions',
					[tsConfigFromOptions, TS_CONFIG]);
			} else {
				return tsConfigFromOptions;
			}
		}

		return null;
	}

	protected async checkWorkingDirectoryForTsconfig(): Promise<string> {
		const tsconfigPath = path.resolve(TS_CONFIG);

		if (await this.fileHandler.exists(tsconfigPath)) {
			this.logger.trace(`Found ${TS_CONFIG} in current working directory ${tsconfigPath}`);
			return tsconfigPath;
		}

		return null;
	}

}

