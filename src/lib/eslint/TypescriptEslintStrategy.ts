import * as path from 'path';
import { EslintStrategy } from "./BaseEslintEngine";
import {FileHandler} from '../util/FileHandler';
import {Config} from '../util/Config';
import {Controller} from '../../ioc.config';
import { SfdxError, Logger } from '@salesforce/core';
import { OutputProcessor } from '../pmd/OutputProcessor';
import * as stripJsonComments from 'strip-json-comments';

/**
 * Type mapping to tsconfig.json files
 */
type TSConfig = {
	compilerOptions: {
		allowJs: boolean;
		outDir: string;
		outFile: string;
	};
	include: string[];
	exclude: string[];
	files: string[];
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
const NO_TS_CONFIG = '';

export class TypescriptEslintStrategy implements EslintStrategy {
	private static ENGINE_NAME = "eslint-typescript";
	private static LANGUAGES = ["typescript"];

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

	async getTargetPatterns(target?: string): Promise<string[]> {
		const engineConfig = this.config.getEngineConfig(this.getName());

		
		// Find the typescript config file, if any
		const tsconfigPath = await this.findTsconfig(target);

		const targetPatterns: string[] = [];
		if (tsconfigPath) {
			const json: string = await this.fileHandler.readFile(tsconfigPath);
			// The default TSConfig has JSON comments. Strip them out before parsing
			const tsconfig: TSConfig = JSON.parse(stripJsonComments(json));

			// Found a tsconfig.  Load up its patterns.
			if (tsconfig.include) {
				targetPatterns.push(...tsconfig.include);
			}

			if (tsconfig.exclude) {
				// Negate the exclude pattern (because that's how we like it)
				targetPatterns.push(...tsconfig.exclude.map(e => "!" + e));
			} else if (tsconfig.compilerOptions && tsconfig.compilerOptions.outDir) {
				// TS likes to auto-exclude the outDir but only if exclude is not specified.
				targetPatterns.push("!" + path.join(tsconfig.compilerOptions.outDir, "**"));
			} else if (tsconfig.compilerOptions && tsconfig.compilerOptions.outFile) {
				// Same reasoning as outDir
				targetPatterns.push("!" + tsconfig.compilerOptions.outFile);
			}

			if (tsconfig.files) {
				targetPatterns.push(...tsconfig.files);
			}
		}

		// TODO: the files returned from here also include .js files even if our target pattern asks not to include them.
		// We should handle the combination more meaningfully than a simple concatenation.

		return engineConfig.targetPatterns.concat(targetPatterns);
	}

	filterUnsupportedPaths(paths: string[]): string[] {
		const filteredPaths = paths.filter(p => p.endsWith(".ts"));
		this.logger.trace(`Input paths: ${paths}, Filtered paths: ${filteredPaths}`);
		return filteredPaths;
	}

	/* eslint-disable @typescript-eslint/no-explicit-any */
	async getRunConfig(target?: string): Promise<Record<string, any>> {
		const tsconfigPath = await this.findTsconfig(target);

		if (tsconfigPath === NO_TS_CONFIG) {
			throw new Error(`Unable to find ${TS_CONFIG}. Please provide ${TS_CONFIG} in the target ${target} or the current working directory or in Config.`);
		}

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

	async findTsconfig(target: string): Promise<string> {
		let tsconfigPath: string;

		// Check config
		tsconfigPath = await this.getTsconfigFromConfig();

		// Check target
		if (!tsconfigPath) {
			tsconfigPath = await this.getTsconfigFromTarget(target);
		}

		// Check current working directory
		if (!tsconfigPath) {
			tsconfigPath = await this.checkDirectoryForTsconfig('');
		}

		this.logger.trace(`Using ${TS_CONFIG} from ${tsconfigPath}`);
		return tsconfigPath;
	}

	private async getTsconfigFromConfig(): Promise<string> {
		let tsconfigPath = NO_TS_CONFIG;
		// Check if overriddenConfig is available
		const overriddenConfig = this.config.getOverriddenConfigPath(this.getName());
		if (!overriddenConfig) {
			this.logger.trace(`Did not find an overridden path from Config`);
			return tsconfigPath;
		}

		// Complain if overriddenConfig does not exist.
		if (!(await this.fileHandler.exists(path.resolve(overriddenConfig)))) {
			throw SfdxError.create('@salesforce/sfdx-scanner', 'eslintEngine', 'InvalidPath', [overriddenConfig]);
		}

		// If overriddenConfig is a directory, look for tsconfig.json file.
		if (await this.fileHandler.isDir(overriddenConfig)) {
			this.logger.trace(`Looking for ${TS_CONFIG} inside overridden dir from config ${overriddenConfig}`);
			tsconfigPath = await this.checkDirectoryForTsconfig(overriddenConfig);
			// Complain if we can't find a tsconfig.json inside.
			if (!tsconfigPath) {
				throw new SfdxError(`Overridden path [${overriddenConfig}] does not contain ${TS_CONFIG}`);
			}
		} else if (overriddenConfig.endsWith(TS_CONFIG)) { // If overriddenConfig is a file, check if the file is tsconfig.json
			// TODO: what if the user has a config file with a different name? How would eslint react to it if we set the value?
			this.logger.trace(`Found ${TS_CONFIG} directly as overridden path in Config, ${overriddenConfig}`);
			tsconfigPath = overriddenConfig;
		} else { // Complain if the file is something else
			throw SfdxError.create('@salesforce/sfdx-scanner', 'eslintEngine', 'ConfigFileMissing', [TS_CONFIG, overriddenConfig]);
		}

		return tsconfigPath;
	}

	private async getTsconfigFromTarget(target: string): Promise<string> {
		let tsconfigPath = NO_TS_CONFIG;
		if (await this.fileHandler.isDir(target)) {
			tsconfigPath = await this.checkDirectoryForTsconfig(target);
		}
		// TODO: if this is a pattern, should we look at each directory from bottom to top?
		return tsconfigPath;
	}

	private async checkDirectoryForTsconfig(givenPath: string): Promise<string> {
		if (await this.fileHandler.exists(path.resolve(givenPath, TS_CONFIG))) {
			const tsconfigPath = path.resolve(givenPath, TS_CONFIG);
			this.logger.trace(`Found ${TS_CONFIG} in directory ${givenPath}`);
			return tsconfigPath;
		}
		return NO_TS_CONFIG;
	}

}

