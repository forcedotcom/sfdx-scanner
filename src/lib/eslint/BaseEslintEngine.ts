/* eslint-disable @typescript-eslint/no-explicit-any */
import {Logger, SfdxError, LoggerLevel} from '@salesforce/core';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget, RuleViolation, ESRule, ESReport, ESMessage} from '../../types';
import {OutputProcessor} from '../pmd/OutputProcessor';
import {RuleEngine} from '../services/RuleEngine';
import {CLIEngine} from 'eslint';
import * as path from 'path';


export interface EslintStrategy {

	init(): Promise<void>;

	getName(): string;

	isEnabled(): boolean;

	getTargetPatterns(target?: string): Promise<string[]>;

	getCatalogConfig(): Record<string, any>;

	getRunConfig(target?: string): Promise<Record<string, any>>;

	getLanguage(): string[];

	isRuleKeySupported(key: string): boolean;

	filterUnsupportedPaths(paths: string[]): string[];
}
export class StaticDependencies {
	public createCLIEngine(config: Record<string,any>): CLIEngine {
		return new CLIEngine(config);
	}
	
	public resolveTargetPath(target: string): string {
		return path.resolve(target);
	}
	
	public getCurrentWorkingDirectory(): string {
		return process.cwd();
	}
}

export abstract class BaseEslintEngine implements RuleEngine {

	private strategy: EslintStrategy;
	protected logger: Logger;
	private initializedBase: boolean;
	protected outputProcessor: OutputProcessor;
	private baseDependencies: StaticDependencies;

	public async abstract init(): Promise<void>;

	public async initializeContents(strategy: EslintStrategy, baseDependencies = new StaticDependencies()): Promise<void> {
		if (this.initializedBase) {
			return;
		}
		this.strategy = strategy;
		this.logger = await Logger.child(strategy.getName());
		this.logger.setLevel(LoggerLevel.TRACE); //TODO: remove this before merge
		// this.outputProcessor = await OutputProcessor.create({});
		this.baseDependencies = baseDependencies;

		this.initializedBase = true;
	}

	public matchPath(path: string): boolean {
		// TODO implement matchPath when Custom Rules are handled for eslint
		this.logger.trace(`Custom rules for eslint is not supported yet: ${path}`);
		return false;
	}

	public getName(): string {
		return this.strategy.getName();
	}

	public isEnabled(): boolean {
		return this.strategy.isEnabled();
	}

	public async getTargetPatterns(target?: string): Promise<string[]> {
		return await this.strategy.getTargetPatterns(target);
	}

	getCatalog(): Promise<Catalog> {
		const categoryMap: Map<string, RuleGroup> = new Map();
		const catalog: Catalog = {rulesets: [], categories: [], rules: []};
		const rules: Rule[] = [];

		const cli = this.baseDependencies.createCLIEngine(this.strategy.getCatalogConfig());
		const allRules = cli.getRules();
		allRules.forEach((esRule: ESRule, key: string) => {
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

		if (this.strategy.isRuleKeySupported(key)) {

			const rule = {
				engine: this.getName(),
				sourcepackage: this.getName(),
				name: key,
				description: docs.description,
				categories: [docs.category],
				rulesets: [docs.category],
				languages: [...this.strategy.getLanguage()],
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
			this.logger.trace('No matching target files found. Nothing to execute.');
			return [];
		}

		const filteredRules = this.selectRelevantRules(rules);
		if (Object.keys(filteredRules).length === 0 && filteredRules.constructor === Object) {
			// No rules to run
			this.logger.trace('No matching rules to run. Nothing to execute.');
			return [];
		}

		try {
			const results: RuleResult[] = [];

			for (const target of targets) {
				const cwd = target.isDirectory ? this.baseDependencies.resolveTargetPath(target.target) : this.baseDependencies.getCurrentWorkingDirectory();
				this.logger.trace(`Using current working directory in config as ${cwd}`);
				const config = {cwd};

				config["rules"] = filteredRules;

				target.paths = this.strategy.filterUnsupportedPaths(target.paths);

				if (target.paths.length === 0) {
					// No target files to analyze
					this.logger.trace(`No target files to analyze from ${target.paths}`);
					continue; // to the next target
				}

				Object.assign(config, await this.strategy.getRunConfig(target.target));

				this.logger.trace(`About to run ${this.getName()}. targets: ${target.paths.length}`);

				const cli = this.baseDependencies.createCLIEngine(config);

				const report = cli.executeOnFiles(target.paths);
				this.logger.trace(`Finished running ${this.getName()}`);
				this.addRuleResultsFromReport(results, report, cli.getRules());
			}

			return results;
		} catch (e) {
			throw new SfdxError(e.message || e);
		}
	}

	private selectRelevantRules(rules: Rule[]): Record<string,any> {
		const filteredRules = {};
		let ruleCount = 0;
		for (const rule of rules) {
			if (rule.engine === this.strategy.getName()) {
				filteredRules[rule.name] = "error";
				ruleCount++;
			}
		}
		this.logger.trace(`Count of rules selected for ${this.getName()}: ${ruleCount}`);
		return filteredRules;
	}

	private addRuleResultsFromReport(results: RuleResult[], report: ESReport, ruleMap: Map<string, ESRule>): void {
		for (const r of report.results) {
			// Only add report entries that have actual violations to report.
			if (r.messages && r.messages.length > 0) {
				results.push(this.toRuleResult(r.filePath, r.messages, ruleMap));
			}
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
