import * as path from 'path';
import { EslintStrategy } from "./BaseEslintEngine";
import {FileHandler} from '../util/FileHandler';
import {ENGINE, LANGUAGE} from '../../Constants';
import {RuleViolation} from '../../types';
import { Logger, Messages, SfdxError } from '@salesforce/core';
import { OutputProcessor } from '../pmd/OutputProcessor';
import {deepCopy} from '../../lib/util/Utils';

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'TypescriptEslintStrategy');

export enum TYPESCRIPT_ENGINE_OPTIONS {
	// Specify the location of the tsconfig.json file
	TSCONFIG = 'tsconfig'
}

const ES_PLUS_TS_CONFIG = {
	"parser": "@typescript-eslint/parser",
	"baseConfig": {
		"extends": [
			"eslint:recommended",
			"plugin:@typescript-eslint/recommended",
			"plugin:@typescript-eslint/eslint-recommended"
		]
	},
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
	private static LANGUAGES = [LANGUAGE.TYPESCRIPT];

	private initialized: boolean;
	private logger: Logger;
	private fileHandler: FileHandler;
	private outputProcessor: OutputProcessor;

	async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getEngine().valueOf());
		this.fileHandler = new FileHandler();
		this.outputProcessor = await OutputProcessor.create({});
		this.initialized = true;
	}

	getEngine(): ENGINE {
		return ENGINE.ESLINT_TYPESCRIPT;
	}

	/* eslint-disable @typescript-eslint/no-explicit-any */
	getCatalogConfig(): Record<string, any> {
		return ES_PLUS_TS_CONFIG;
	}

	getLanguages(): string[] {
		return TypescriptEslintStrategy.LANGUAGES;
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

		Object.assign(config, deepCopy(ES_PLUS_TS_CONFIG));

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

	/**
	 * Converts the eslint message that requires the scanned files to be a subset of the files specified by tsconfig.json
	 */
	processRuleViolation(fileName: string, ruleViolation: RuleViolation): void {
		const message: string = ruleViolation.message;

		if (message.startsWith('Parsing error: "parserOptions.project" has been set for @typescript-eslint/parser.\nThe file does not match your project config') &&
			message.endsWith('The file must be included in at least one of the projects provided.')) {
			ruleViolation.message = messages.getMessage('FileNotIncludedByTsConfig', [fileName, TS_CONFIG]);
		}
	}

	/**
	 * Try to find a tsconfig.json in the engineOptions map or current working directory, engineOptions takes precedence.
	 * Throw an error if a tsconfig.json file can't be found.
	 */
	async findTsconfig(engineOptions: Map<string, string>): Promise<string> {
		const foundTsConfig = (await this.checkEngineOptionsForTsconfig(engineOptions)) || (await this.checkWorkingDirectoryForTsconfig());

		if (!foundTsConfig) {
			const cwd = path.resolve();
			// Not specified in engineOptions and not found in the current directory
			throw SfdxError.create('@salesforce/sfdx-scanner', 'TypescriptEslintStrategy', 'MissingTsConfigFromCwd',
				[TS_CONFIG, cwd, TS_CONFIG]);
		}

		this.logger.trace(`Using ${TS_CONFIG} from ${foundTsConfig}`);

		return foundTsConfig;
	}

	/**
	 * Returns the tsconfig.json file specified in the engineOptions if it is specified and valid, or null if
	 * engineOptions doesn't specify a tsconfig.json value.
	 * Throw an error if the file specified in engineOptions is invalid for any reason.
	 */
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

	/**
	 * Return the path of a tsconfig.json file if it exists in the current working directory, or null if it doesn't.
	 */
	protected async checkWorkingDirectoryForTsconfig(): Promise<string> {
		const tsconfigPath = path.resolve(TS_CONFIG);

		if (await this.fileHandler.exists(tsconfigPath)) {
			this.logger.trace(`Found ${TS_CONFIG} in current working directory ${tsconfigPath}`);
			return tsconfigPath;
		}

		return null;
	}

}

