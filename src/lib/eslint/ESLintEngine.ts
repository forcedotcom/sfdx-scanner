/* eslint-disable @typescript-eslint/no-explicit-any */
import {Logger, SfdxError} from '@salesforce/core';
import {CLIEngine} from 'eslint';
import {Controller} from '../../ioc.config';
import {Catalog, Rule, RuleGroup, RuleResult, RuleViolation} from '../../types';
import {RuleEngine} from '../services/RuleEngine';
import {Config} from '../util/Config';
import {FileHandler} from '../util/FileHandler';

/**
 * Format of rules returned from Linter
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

const DEFAULT_ESCONFIG = {
	"extends": ["eslint:recommended"],
	"parserOptions": {
		"sourceType": "module",
		"ecmaVersion": 2018,
	},
	"ignorePatterns": [
		"node_modules/!**"
	],
	"useEslintrc": false
};

const DEFAULT_ESCONFIG_TS = {
	"parser": "@typescript-eslint/parser",
	"extends": [
		"eslint:recommended",
		"plugin:@typescript-eslint/recommended",
		"plugin:@typescript-eslint/eslint-recommended"
	],
	"parserOptions": {
		"sourceType": "module",
		"ecmaVersion": 2018,
		"project": "./tsconfig.json"
	},
	"extensions": ["ts"],
	"plugins": [
		"@typescript-eslint"
	],
	"useEslintrc": false
};

export class ESLintEngine implements RuleEngine {
	public static NAME = "eslint";
	public static JAVASCRIPT_LANGUAGE = "javascript";
	public static TYPESCRIPT_LANGUAGE = "typescript";

	private logger: Logger;
	private initialized: boolean;
	private fileHandler: FileHandler;
	private esconfig: any;
	private config: Config;

	public getName(): string {
		return ESLintEngine.NAME;
	}

	public isEnabled(): boolean {
		return this.config.isEngineEnabled(this.getName());
	}

	public matchPath(path: string): boolean {
		// TODO implement this for realz
		return path != null;
	}

	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getName());

		this.fileHandler = new FileHandler();
		this.config = await Controller.getConfig();

		// We currently assume you must execute the engine from your project directory.  This doesn't matter for all
		// situations, but it is critical at least for typescript.  TS rules require tsconfig.  In the future we could
		// change to treat targetPaths which are directories as project directories, and look inside them for a
		// tsconfig.json file.  Until then, just require that if you want to run against *.ts, you better do so from a
		// working directory that contains tsconfig.
		this.esconfig = await this.fileHandler.exists("tsconfig.json") ?
			DEFAULT_ESCONFIG_TS : DEFAULT_ESCONFIG;

		this.initialized = true;
	}

	getCatalog(): Promise<Catalog> {
		const categoryMap: Map<string, RuleGroup> = new Map();
		const catalog: Catalog = {rulesets: [], categories: [], rules: []};
		const rules: Rule[] = [];
		const cli = new CLIEngine(this.esconfig);
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
			rules.push(r)
		});

		catalog.categories = Array.from(categoryMap.values());
		catalog.rules = rules;
		return Promise.resolve(catalog);
	}

	public async run(ruleGroups: RuleGroup[], rules: Rule[], targets: string[]): Promise<RuleResult[]> {
		const targetPaths: string[] = targets;
		// If we didn't find any paths, we're done.
		if (targetPaths == null || targetPaths.length === 0) {
			this.logger.trace('No matching eslint target files found. Nothing to execute.');
			return [];
		}

		if (rules.length === 0) {
			this.logger.trace('No matching eslint rules found. Nothing to execute.');
			return [];
		}

		this.logger.trace(`About to run eslint engine. Files: ${targetPaths.length}, rules: ${rules.length}`);
		try {
			const config = {
				rules: {}
			};
			rules.forEach(r => {config.rules[r.name] = "error"});
			Object.assign(config, this.esconfig);
			const cli = new CLIEngine(config);
			const report = cli.executeOnFiles(targetPaths);
			return this.reportToRuleResults(report, cli.getRules());
		} catch (e) {
			throw new SfdxError(e.message || e);
		}
	}

	private reportToRuleResults(report: ESReport, ruleMap: Map<string,ESRule>): RuleResult[] {
		return report.results.map(r => this.toRuleResult(r.filePath, r.messages, ruleMap));
	}

	private toRuleResult(fileName: string, messages: ESMessage[], ruleMap: Map<string,ESRule>): RuleResult {
		return {
			engine: ESLintEngine.NAME,
			fileName,
			violations: messages.map(
				(v): RuleViolation => {
					const rule = ruleMap.get(v.ruleId);
					const category = rule ? rule.meta.docs.category : "";
					return {
						line: v.line,
						column: v.column,
						severity: v.severity,
						message: v.message,
						ruleName: v.ruleId,
						category
					};
				}
			)
		};
	}
}
