/* eslint-disable @typescript-eslint/no-explicit-any */
import {Logger, SfdxError, LoggerLevel} from '@salesforce/core';
import {CLIEngine} from 'eslint';
import {Catalog, Rule, RuleEvent, RuleGroup, RuleResult, RuleTarget, RuleViolation} from '../../types';
import {OutputProcessor} from '../pmd/OutputProcessor';
import {RuleEngine} from '../services/RuleEngine';
import * as path from 'path';

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

export abstract class ESLintEngine implements RuleEngine {

	protected logger: Logger;
	private initialized: boolean;
	private outputProcessor: OutputProcessor;

	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child("eslint");
		this.logger.setLevel(LoggerLevel.TRACE); //TODO: remove this before merge
		this.outputProcessor = await OutputProcessor.create({});

		this.initialized = true;
	}

	public matchPath(path: string): boolean {
		// TODO implement this for realz
		return path != null;
	}

	// TODO: this method is already defined in RuleEngine - can't I not declare this method again?
	public abstract getName(): string;

	// TODO: this method is already defined in RuleEngine - can't I not declare this method again?
	public abstract isEnabled(): boolean;

	// TODO: this method is already defined in RuleEngine - can't I not declare this method again?
	public abstract async getTargetPatterns(target?: string): Promise<string[]>;

	protected abstract getCatalogConfig(): Object;

	protected abstract async getRunConfig(target?: string): Promise<Object>;

	protected abstract getLanguage(): string;

	protected abstract isRuleKeySupported(key: string): boolean;

	protected abstract isRuleRelevant(rule: Rule): boolean;

	protected abstract filterUnsupportedPaths(paths: string[]): string[];

	getCatalog(): Promise<Catalog> {
		const categoryMap: Map<string, RuleGroup> = new Map();
		const catalog: Catalog = {rulesets: [], categories: [], rules: []};
		const rules: Rule[] = [];

		const cli = new CLIEngine(this.getCatalogConfig());
		cli.getRules().forEach((esRule: ESRule, key: string) => {
			const docs = esRule.meta.docs;

			const rule = this.processRule(key, docs);
			if (rule) {
				rules.push(rule);
				const categoryName = docs.category;
				let category = categoryMap.get(categoryName);
				if (!category) {
					category = { name: categoryName, engine: this.getName(), paths: [] };
					categoryMap.set(categoryName, category);
				}
				category.paths.push(docs.url);
			}
		});

		catalog.categories = Array.from(categoryMap.values());
		catalog.rules = rules;
		return Promise.resolve(catalog);
	}

	private processRule(key: string, docs: any): Rule {

		if (this.isRuleKeySupported(key)) {

			const rule = {
				engine: this.getName(),
				sourcepackage: this.getName(),
				name: key,
				description: docs.description,
				categories: [docs.category],
				rulesets: [docs.category],
				languages: [this.getLanguage()],
				defaultEnabled: docs.recommended,
				url: docs.url
			};
			return rule;
		}

		return null;
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

				Object.assign(config, await this.getRunConfig(target.target));

				const filteredRules = this.selectRelevantRules(rules);
				config["rules"] = filteredRules; // TODO: don't run if no rules were selected

				target.paths = this.filterUnsupportedPaths(target.paths);

				this.logger.trace(`About to run ${this.getName()}. targets: ${target.paths.length}`);

				const cli = new CLIEngine(config);

				const report = cli.executeOnFiles(target.paths);
				this.addRuleResultsFromReport(results, report, cli.getRules());
			}

			return results;
		} catch (e) {
			throw new SfdxError(e.message || e);
		} finally {
			this.outputProcessor.emitEvents(events);
		}
	}

	private selectRelevantRules(rules: Rule[]) {
		const filteredRules = {};
		let ruleCount = 0;
		for (const rule of rules) {
			if (this.isRuleRelevant(rule)) {
				filteredRules[rule.name] = "error";
				ruleCount++;
			}
		}
		this.logger.trace(`Count of rules selected for ${this.getName}: ${ruleCount}`);
		return filteredRules;
	}

	private addRuleResultsFromReport(results: RuleResult[], report: ESReport, ruleMap: Map<string, ESRule>): void {
		for (const r of report.results) {
			results.push(this.toRuleResult(r.filePath, r.messages, ruleMap));
		}
	}

	private toRuleResult(fileName: string, messages: ESMessage[], ruleMap: Map<string, ESRule>): RuleResult {
		return {
			engine: this.getName(),
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
