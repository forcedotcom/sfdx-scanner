import {Logger,} from '@salesforce/core';
import {Stats} from 'fs';
import * as path from 'path';
import {inject, injectable, injectAll} from 'tsyringe';
import {Controller} from '../ioc.config';
import {Rule, RuleGroup, RuleResult} from '../types';
import {RuleFilter} from './RuleFilter';
import {OUTPUT_FORMAT, RuleManager} from './RuleManager';
import {RuleResultRecombinator} from './RuleResultRecombinator';
import {RuleCatalog} from './services/RuleCatalog';
import {RuleEngine} from './services/RuleEngine';
import {EngineConfigContent} from './util/Config';
import {FileHandler} from './util/FileHandler';
import globby = require('globby');

@injectable()
export class DefaultRuleManager implements RuleManager {
	private logger: Logger;

	// noinspection JSMismatchedCollectionQueryUpdate
	private readonly engines: RuleEngine[];
	private readonly catalog: RuleCatalog;
	private fileHandler: FileHandler;
	private initialized: boolean;

	constructor(
		@injectAll("RuleEngine") engines?: RuleEngine[],
		@inject("RuleCatalog") catalog?: RuleCatalog
	) {
		this.engines = engines;
		this.catalog = catalog;
	}

	async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child('DefaultManager');
		this.fileHandler = new FileHandler();
		for (const engine of this.engines) {
			await engine.init();
		}
		await this.catalog.init();

		this.initialized = true;
	}

	async getRulesMatchingCriteria(filters: RuleFilter[]): Promise<Rule[]> {
		return this.catalog.getRulesMatchingFilters(filters);
	}

	async runRulesMatchingCriteria(filters: RuleFilter[], targets: string[], format: OUTPUT_FORMAT): Promise<string | { columns; rows }> {
		let results: RuleResult[] = [];

		// Derives rules from our filters to feed the engines.
		const ruleGroups: RuleGroup[] = await this.catalog.getRuleGroupsMatchingFilters(filters);
		const rules: Rule[] = await this.catalog.getRulesMatchingFilters(filters);
		const ps: Promise<RuleResult[]>[] = [];
		for (const e of this.engines) {
			const engineGroups = ruleGroups.filter(g => g.engine === e.getName());
			const engineRules = rules.filter(r => r.engine === e.getName());
			const engineTargets = await this.unpackTargets(e, targets);
			ps.push(e.run(engineGroups, engineRules, engineTargets));
		}
		const psResults: RuleResult[][] = await Promise.all(ps);
		psResults.forEach(r => results = results.concat(r));
		this.logger.trace(`Received rule violations: ${results}`);
		this.logger.trace(`Recombining results into requested format ${format}`);
		return RuleResultRecombinator.recombineAndReformatResults(results, format);
	}

	private async unpackTargets(engine: RuleEngine, targets: string[]): Promise<string[]> {
		// Add any default target patterns from config.
		const config = await Controller.getConfig();
		const engineConfig: EngineConfigContent = config.getEngineConfig(engine.getName());
		const targetPaths: string[] = [];
		for (const target of targets) {
			// If any of the target paths have glob patterns, find all matching files. Otherwise, just return
			// the target paths.
			if (globby.hasMagic(target)) {
				targetPaths.push(...await globby(target));
			} else if (await this.fileHandler.exists(target)) {
				const stats: Stats = await this.fileHandler.stats(target);
				if (stats.isDirectory() && engineConfig.targetPatterns) {
					// If dir, use globby { cwd: process.cwd() } option
					const relativePaths = await globby(engineConfig.targetPatterns, {cwd: target});
					targetPaths.push(...relativePaths.map(rp => path.join(target, rp)));
				} else {
					targetPaths.push(target);
				}
			}
		}
		return targetPaths;
	}
}
