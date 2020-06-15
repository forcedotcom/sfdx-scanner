import * as path from 'path';
import { ESLintEngine } from "./ESLintEngine";
import {TYPESCRIPT_RULE_PREFIX} from '../../Constants';
import { Rule } from "../../types";
import {FileHandler} from '../util/FileHandler';
import {Config} from '../util/Config';
import {Controller} from '../../ioc.config';
import { SfdxError } from '@salesforce/core';

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
};

const TS_CONFIG = "tsconfig.json";

export class TypescriptEslintEngine extends ESLintEngine {
	private static ENGINE_NAME = "@typescript-eslint";
	private static LANGUAGE = "typescript";

	private fileHandler: FileHandler;
	private config: Config;

	public async init(): Promise<void> {
		await super.init();
		this.fileHandler = new FileHandler();
		this.config = await Controller.getConfig();
	}

	public isEnabled(): boolean {
		return this.config.isEngineEnabled(this.getName());
	}

	public getName(): string {
		return TypescriptEslintEngine.ENGINE_NAME;
	}

	protected isRuleKeySupported(key: string): boolean {
		return key.startsWith(TYPESCRIPT_RULE_PREFIX);
	}

	getCatalogConfig(): Object {
		return ES_PLUS_TS_CONFIG;
	}

	getLanguage(): string {
		return TypescriptEslintEngine.LANGUAGE;
	}

	isRuleRelevant(rule: Rule) {
		// TODO: is this sufficient or do we need to check something else?
		return this.getName() === rule.engine;
	}

	async getTargetPatterns(target?: string): Promise<string[]> {
		const engineConfig = this.config.getEngineConfig(this.getName());

		
		// Find the typescript config file, if any
		const tsconfigPath = await this.findTSConfig(target);

		const targetPatterns: string[] = [];
		if (tsconfigPath) {
			const tsconfig: TSConfig = JSON.parse(await this.fileHandler.readFile(tsconfigPath));

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

		// Join forces.  TODO or maybe not?  Should we just rely only on tsconfig if provided?
		return engineConfig.targetPatterns.concat(targetPatterns);
	}

	filterUnsupportedPaths(paths: string[]): string[] {
		const filteredPaths = paths.filter(p => p.endsWith(".ts"));
		return filteredPaths;
	}

	async getRunConfig(target?: string): Promise<Object> {
		const tsconfigPath = await this.findTSConfig(target);

		if (!tsconfigPath) {
			throw new Error(`Unable to find ${TS_CONFIG}. Please provide ${TS_CONFIG} in the target ${target} or the current working directory or in Config.`);
		}

		// const tsconfigContent = await this.fileHandler.readFile(tsconfigPath);
		// const tsconfig = JSON.parse(tsconfigContent);

		const config = {};

		// TODO: enable event after cleaning up OutputProcessor
		// events.push({
		// 	// Alert the user we found a config file, if --verbose
		// 	messageKey: 'info.usingEngineConfigFile', args: [tsconfigPath], type: 'INFO', handler: 'UX', verbose: true, time: Date.now()
		// });
		Object.assign(config, ES_PLUS_TS_CONFIG);
		// Object.assign(config, tsconfig);

		// Enable typescript by registering its project config file
		config["parserOptions"].project = tsconfigPath;

		this.logger.trace(`Using config for run: ${JSON.stringify(config)}`);

		return config;
	}

	private async findTSConfig(target: string): Promise<string> {
		let tsconfigPath;

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
		let tsconfigPath = '';
		// Check if overriddenConfig is available
		const overriddenConfig = this.config.getOverriddenConfigPath(this.getName());
		if (!overriddenConfig) {
			this.logger.trace(`Did not find an overridden path from Config`);
			return tsconfigPath;
		}

		// Complain if overriddenConfig does not exist.
		if (!(await this.fileHandler.exists(path.resolve(overriddenConfig)))) {
			throw new SfdxError(`Invalid path in Config: ${overriddenConfig}`);
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
			this.logger.trace(`Found ${TS_CONFIG} directly as overridden path in Config, ${overriddenConfig}`);
			tsconfigPath = overriddenConfig;
		} else { // Complain if the file is something else
			throw new SfdxError(`Cannot find ${TS_CONFIG} with overridden path ${overriddenConfig}`);
		}

		return tsconfigPath;
	}

	private async getTsconfigFromTarget(target: string): Promise<string> {
		let tsconfigPath = '';
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
		return '';
	}

}