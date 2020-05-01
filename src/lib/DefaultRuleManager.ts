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
import picomatch = require('picomatch');

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
			// For each engine, filter for the appropriate groups and rules and targets, and pass
			// them all in. Note that some engines (pmd) need groups while others (eslint) need the rules.
			const engineGroups = ruleGroups.filter(g => g.engine === e.getName());
			const engineRules = rules.filter(r => r.engine === e.getName());
			const engineTargets = await this.unpackTargets(e, targets);
			ps.push(e.run(engineGroups, engineRules, engineTargets));
		}

		// Execute all run promises, each of which returns an array of RuleResults, then concatenate
		// all of the results together from all engines into one report.
		const psResults: RuleResult[][] = await Promise.all(ps);
		psResults.forEach(r => results = results.concat(r));
		this.logger.trace(`Received rule violations: ${results}`);
		this.logger.trace(`Recombining results into requested format ${format}`);
		return RuleResultRecombinator.recombineAndReformatResults(results, format);
	}

	/**
	 * Given a simple list of top-level targets and the engine to be executed, retrieve the full file listing
	 * to target.
	 * 1. If a target has a pattern (i.e. hasMagic) resolve it using globby.
	 * 2. If a target is a directory, get its contents using the target patterns specified for the engine.
	 * 3. If the target is a file, make sure it matches the engine's target patterns.
	 */
	private async unpackTargets(engine: RuleEngine, targets: string[]): Promise<string[]> {
		// Add any default target patterns from config.
		const config = await Controller.getConfig();
		const engineConfig: EngineConfigContent = config.getEngineConfig(engine.getName());
		const targetPaths: string[] = [];
		for (const target of targets) {
			if (globby.hasMagic(target)) {
				// The target is a magic globby glob.  Retrieve paths in the working dir that match it.
				targetPaths.push(...await globby(target));
			} else if (await this.fileHandler.exists(target)) {
				const stats: Stats = await this.fileHandler.stats(target);
				if (stats.isDirectory()) {
					// The target is a directory.  If the engine has target patterns, which is always should,
					// call globby with the directory as the working dir, and use the patterns to match its contents.
					if (engineConfig.targetPatterns) {
						// If dir, use globby { cwd: process.cwd() } option
						const relativePaths = await globby(engineConfig.targetPatterns, {cwd: target});
						targetPaths.push(...relativePaths.map(rp => path.join(target, rp)));
					} else {
						// Without target patterns for the engine, just add the dir itself and hope for the best.
						targetPaths.push(target);
					}
				} else {
					// Files are trickier than dirs.  We need to treat inclusive patterns separate from exclusive.
					// First see if the file path matches the inclusive patterns (is this file included?).  If so,
					// then run the exclusive patterns to see if the file was expressly excluded.
					// Why? Globby treats all given patterns as an OR operation.  Say we scan a .cls file using eslint.
					// The inclusive patterns (**/*.js, **/*.ts) return false, yes.  But the exclusive pattern
					// (!node_modules/*) returns true, which is correct, but since globby ORs them together, it gives
					// a positive match when it should do the opposite.
					// (Note: picomatch is the pattern matching library used by globby.)
					const isInclusiveMatch = picomatch(engineConfig.targetPatterns.filter(p => !p.startsWith("!")));
					if (isInclusiveMatch(target)) {
						// If target matches inclusive patterns, only then check if it matches exclusive patterns.
						const isExclusiveMatch = picomatch(engineConfig.targetPatterns.filter(p => p.startsWith("!")));
						if (isExclusiveMatch(target)) {
							targetPaths.push(target);
						}
					}
				}
			}
		}
		return targetPaths;
	}
}
