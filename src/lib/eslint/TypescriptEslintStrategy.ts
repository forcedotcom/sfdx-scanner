import * as path from 'path';
import { EslintStrategy } from "./BaseEslintEngine";
import {FileHandler} from '../util/FileHandler';
import {ENGINE, LANGUAGE, HARDCODED_RULES} from '../../Constants';
import {ESRule, ESRuleConfig, LooseObject, RuleViolation} from '../../types';
import { Logger, Messages, SfdxError } from '@salesforce/core';
import { OutputProcessor } from '../pmd/OutputProcessor';
import {deepCopy} from '../../lib/util/Utils';
import { EslintStrategyHelper, ProcessRuleViolationType } from './EslintCommons';

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'TypescriptEslintStrategy');

export enum TYPESCRIPT_ENGINE_OPTIONS {
	// Specify the location of the tsconfig.json file
	TSCONFIG = 'tsconfig'
}

const ES_PLUS_TS_CONFIG = {
	"parser": "@typescript-eslint/parser",
	"baseConfig": {},
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
	"useEslintrc": false, // Will not use external config
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
	private recommendedConfig: LooseObject;

	async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getEngine().valueOf());
		this.fileHandler = new FileHandler();
		this.outputProcessor = await OutputProcessor.create({});
		const pathToRecommendedConfig = require.resolve('@typescript-eslint/eslint-plugin')
			.replace('index.js', path.join('configs', 'recommended.json'));
		this.recommendedConfig = JSON.parse(await this.fileHandler.readFile(pathToRecommendedConfig));
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

	filterDisallowedRules(rulesByName: Map<string, ESRule>): Map<string, ESRule> {
		// We want to identify rules that are deprecated, and rules that are extended by other rules, so we can manually
		// filter them out.
		const extendedRules: Set<string> = new Set();
		const deprecatedRules: Set<string> = new Set();

		for (const [name, rule] of rulesByName.entries()) {
			if (rule.meta.deprecated) {
				deprecatedRules.add(name);
			}
			if (rule.meta.docs.extendsBaseRule) {
				// The `extendsBaseRule` property can be a string, representing the name of the rule being extended.
				if (typeof rule.meta.docs.extendsBaseRule === 'string') {
					extendedRules.add(rule.meta.docs.extendsBaseRule);
				} else {
					// Alternatively, it can be just the boolean true, which indicates that this rule extends a base rule
					// with the exact same name. So to determine the base rule's name, we need to remove all namespacing
					// information from this rule's name, i.e. strip everything up through the last '/' character.
					extendedRules.add(name.slice(name.lastIndexOf('/') + 1));
				}
			}
		}

		// Now, we'll want to create a new Map containing every rule that isn't deprecated or extended.
		const filteredMap: Map<string,ESRule> = new Map();
		for (const [name, rule] of rulesByName.entries()) {
			if (!extendedRules.has(name) && !deprecatedRules.has(name)) {
				filteredMap.set(name, rule);
			}
		}
		return filteredMap;
	}

	ruleDefaultEnabled(name: string): boolean {
		return EslintStrategyHelper.isDefaultEnabled(this.recommendedConfig, name);
	}

	getDefaultConfig(ruleName: string): ESRuleConfig {
		return EslintStrategyHelper.getDefaultConfig(this.recommendedConfig, ruleName);
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
	processRuleViolation(): ProcessRuleViolationType {
		return (fileName: string, ruleViolation: RuleViolation): void => {
			const message: string = ruleViolation.message;

			if (message.startsWith('Parsing error: "parserOptions.project" has been set for @typescript-eslint/parser.\nThe file does not match your project config') &&
				message.endsWith('The file must be included in at least one of the projects provided.')) {
				ruleViolation.message = messages.getMessage('FileNotIncludedByTsConfig', [fileName, TS_CONFIG]);
			} else if (message.startsWith('Parsing error:')) {
				ruleViolation.ruleName = HARDCODED_RULES.FILES_MUST_COMPILE.name;
				ruleViolation.category = HARDCODED_RULES.FILES_MUST_COMPILE.category;
			}
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

