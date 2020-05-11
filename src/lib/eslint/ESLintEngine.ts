/* eslint-disable @typescript-eslint/no-explicit-any */
import {Logger, SfdxError} from '@salesforce/core';
import {CLIEngine} from 'eslint';
import * as path from 'path';
import {Controller} from '../../ioc.config';
import {Catalog, Rule, RuleEvent, RuleGroup, RuleResult, RuleTarget, RuleViolation} from '../../types';
import {OutputProcessor} from '../pmd/OutputProcessor';
import {RuleEngine} from '../services/RuleEngine';
import {Config} from '../util/Config';
import {FileHandler} from '../util/FileHandler';

/**
 * Type mapping to rules returned from eslint
 */
type ESRule = {
	meta: {
		docs: {
			description: string;
			category: string;
			recommended: boolean;
			url: string;
		};
		schema: Record<string, any>[];
	};
	create: Function;
}

/**
 * Type mapping to report output by eslint
 */
type ESReport = {
	results: [
		{
			filePath: string;
			messages: ESMessage[];
		}
	];
	errorCount: number;
	warningCount: number;
	fixableErrorCount: number;
	fixableWarningCount: number;
	usedDeprecatedRules: string[];
}

/**
 * Type mapping to report messages output by eslint
 */
type ESMessage = {
	fatal: boolean;
	ruleId: string;
	severity: number;
	line: number;
	column: number;
	message: string;
	fix: {
		range: [number, number];
		text: string;
	};
}

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

const ES_CONFIG = {
	"extends": ["eslint:recommended"],
	"parserOptions": {
		"sourceType": "module",
		"ecmaVersion": 2018,
	},
	"ignorePatterns": [
		"node_modules/!**"
	],
	"useEslintrc": false // TODO derive from existing eslintrc if found and desired
};

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
	"useEslintrc": false // TODO derive from existing eslintrc if found and desired
};

const TYPESCRIPT_RULE_PREFIX = '@typescript';

export class ESLintEngine implements RuleEngine {
	public static NAME = "eslint";
	public static JAVASCRIPT_LANGUAGE = "javascript";
	public static TYPESCRIPT_LANGUAGE = "typescript";

	private logger: Logger;
	private initialized: boolean;
	private outputProcessor: OutputProcessor;
	private fileHandler: FileHandler;
	private config: Config;

	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(ESLintEngine.NAME);
		this.outputProcessor = await OutputProcessor.create({});

		this.fileHandler = new FileHandler();
		this.config = await Controller.getConfig();
		this.initialized = true;
	}

	public getName(): string {
		return ESLintEngine.NAME;
	}

	public isEnabled(): boolean {
		return this.config.isEngineEnabled(ESLintEngine.NAME);
	}

	public matchPath(path: string): boolean {
		// TODO implement this for realz
		return path != null;
	}

	getCatalog(): Promise<Catalog> {
		const categoryMap: Map<string, RuleGroup> = new Map();
		const catalog: Catalog = {rulesets: [], categories: [], rules: []};
		const rules: Rule[] = [];
		// To get the full list of available rules for both js and ts, use the ts config here.
		const cli = new CLIEngine(ES_PLUS_TS_CONFIG);
		cli.getRules().forEach((rule: ESRule, key: string) => {
			const docs = rule.meta.docs;
			const categoryName = docs.category;
			let category = categoryMap.get(categoryName);
			if (!category) {
				category = {name: categoryName, engine: ESLintEngine.NAME, paths: []};
				categoryMap.set(categoryName, category);
			}
			category.paths.push(docs.url);

			const r = {
				engine: ESLintEngine.NAME,
				sourcepackage: ESLintEngine.NAME,
				name: key,
				description: docs.description,
				categories: [docs.category],
				rulesets: [docs.category],
				languages: [ESLintEngine.JAVASCRIPT_LANGUAGE],
				defaultEnabled: docs.recommended,
				url: docs.url
			};
			if (key.startsWith("@typescript")) {
				// Typescript is superset of javascript, so add it on top here.
				r.languages.push(ESLintEngine.TYPESCRIPT_LANGUAGE);
			}
			rules.push(r);
		});

		catalog.categories = Array.from(categoryMap.values());
		catalog.rules = rules;
		return Promise.resolve(catalog);
	}

	public async run(ruleGroups: RuleGroup[], rules: Rule[], targets: RuleTarget[]): Promise<RuleResult[]> {
		// If we didn't find any paths, we're done.
		if (targets == null || targets.length === 0) {
			this.logger.trace('No matching eslint target files found. Nothing to execute.');
			return [];
		}

		const events: RuleEvent[] = [];

		try {
			const results: RuleResult[] = [];
			for (const target of targets) {
				const cwd = target.isDirectory ? path.resolve(target.target) : process.cwd();
				const config = {cwd};

				// TYPESCRIPT HANDLING: Enable typescript by registering its project config file, if found.
				const tsconfigPath = await this.findTSConfig(target.target);
				if (tsconfigPath) {
					events.push({
						// Alert the user we found a config file, if --verbose
						messageKey: 'info.usingEngineConfigFile', args: [tsconfigPath], type: 'INFO', handler: 'UX', verbose: true, time: Date.now()
					});
					Object.assign(config, ES_PLUS_TS_CONFIG);
					config["parserOptions"].project = tsconfigPath;
				} else {
					Object.assign(config, ES_CONFIG);
				}

				const filteredRules = {};
				let ruleCount = 0;
				for (const rule of rules) {
					// TYPESCRIPT HANDLING: Include typescript rules only if we are running with a typescript project.
					if (tsconfigPath || !rule.name.startsWith(TYPESCRIPT_RULE_PREFIX)) {
						filteredRules[rule.name] = "error";
						ruleCount++;
					}
				}
				config["rules"] = filteredRules;
				this.logger.trace(`About to run eslint engine; targets: ${target.paths.length}, rules: ${ruleCount}`);

				const cli = new CLIEngine(config);

				// TYPESCRIPT HANDLING: Strip out any typescript files if we are not running with a typescript project.
				const targetPaths = tsconfigPath ? target.paths : target.paths.filter(p => !p.endsWith(".ts"));
				const report = cli.executeOnFiles(targetPaths);
				this.addRuleResultsFromReport(results, report, cli.getRules());
			}

			return results;
		} catch (e) {
			throw new SfdxError(e.message || e);
		} finally {
			this.outputProcessor.emitEvents(events);
		}
	}

	private async findTSConfig(target: string): Promise<string> {
		let tsconfigPath;
		if (await this.fileHandler.isDir(target) && await this.fileHandler.exists(path.resolve(target, "tsconfig.json"))) {
			// Found a config file under the target dir.
			tsconfigPath = path.resolve(target, "tsconfig.json");
		} else if (await this.fileHandler.exists(path.resolve("tsconfig.json"))) {
			// Check if one exists in the root.
			tsconfigPath = path.resolve("tsconfig.json");
		}
		return tsconfigPath;
	}

	async getTargetPatterns(target?: string): Promise<string[]> {
		// TODO derive from existing eslintrc here if found and desired
		const engineConfig = this.config.getEngineConfig(ESLintEngine.NAME);

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

	private addRuleResultsFromReport(results: RuleResult[], report: ESReport, ruleMap: Map<string, ESRule>): void {
		for (const r of report.results) {
			results.push(this.toRuleResult(r.filePath, r.messages, ruleMap));
		}
	}

	private toRuleResult(fileName: string, messages: ESMessage[], ruleMap: Map<string, ESRule>): RuleResult {
		return {
			engine: ESLintEngine.NAME,
			fileName,
			violations: messages.map(
				(v): RuleViolation => {
					const rule = ruleMap.get(v.ruleId);
					const category = rule ? rule.meta.docs.category : "";
					const url = rule ? rule.meta.docs.url : "";
					return {
						line: v.line,
						column: v.column,
						severity: v.severity,
						message: v.message,
						ruleName: v.ruleId,
						category,
						url
					};
				}
			)
		};
	}
}
