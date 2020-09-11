import {Logger, SfdxError} from '@salesforce/core';
import * as assert from 'assert';
import {Stats} from 'fs';
import {inject, injectable} from 'tsyringe';
import {RecombinedRuleResults, Rule, RuleGroup, RuleResult, RuleTarget} from '../types';
import {FilterType, RuleFilter} from './RuleFilter';
import {OUTPUT_FORMAT, RuleManager} from './RuleManager';
import {RuleResultRecombinator} from './RuleResultRecombinator';
import {RuleCatalog} from './services/RuleCatalog';
import {RuleEngine} from './services/RuleEngine';
import {FileHandler} from './util/FileHandler';
import {Controller} from '../Controller';
import globby = require('globby');
import picomatch = require('picomatch');
import path = require('path');

@injectable()
export class DefaultRuleManager implements RuleManager {
	private logger: Logger;

	// noinspection JSMismatchedCollectionQueryUpdate
	private readonly catalog: RuleCatalog;
	private fileHandler: FileHandler;
	private initialized: boolean;

	constructor(
		@inject("RuleCatalog") catalog?: RuleCatalog
	) {
		this.catalog = catalog;
	}

	async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child('DefaultManager');
		this.fileHandler = new FileHandler();
		await this.catalog.init();

		this.initialized = true;
	}

	getRulesMatchingCriteria(filters: RuleFilter[]): Rule[] {
		return this.catalog.getRulesMatchingFilters(filters);
	}

	async runRulesMatchingCriteria(filters: RuleFilter[], targets: string[], format: OUTPUT_FORMAT, engineOptions: Map<string, string>): Promise<RecombinedRuleResults> {
		let results: RuleResult[] = [];

		// Derives rules from our filters to feed the engines.
		const ruleGroups: RuleGroup[] = this.catalog.getRuleGroupsMatchingFilters(filters);
		const rules: Rule[] = this.catalog.getRulesMatchingFilters(filters);
		const ps: Promise<RuleResult[]>[] = [];
		let filteredNames = null;
		for (const filter of filters) {
			if (filter.filterType === FilterType.ENGINE) {
				filteredNames = filter.filterValues;
				break;
			}
		}
		const engines: RuleEngine[] = await (filteredNames ? Controller.getFilteredEngines(filteredNames) : Controller.getEnabledEngines());
		for (const e of engines) {
			// For each engine, filter for the appropriate groups and rules and targets, and pass
			// them all in. Note that some engines (pmd) need groups while others (eslint) need the rules.
			const engineGroups = ruleGroups.filter(g => g.engine === e.getName());
			const engineRules = rules.filter(r => r.engine === e.getName());
			const engineTargets = await this.unpackTargets(e, targets);
			this.logger.trace(`For ${e.getName()}, found ${engineGroups.length} groups, ${engineRules.length} rules, ${engineTargets.length} targets`);
			if (engineRules.length > 0 && engineTargets.length > 0) {
				this.logger.trace(`${e.getName()} is eligible to execute.`);
				ps.push(e.run(engineGroups, engineRules, engineTargets, engineOptions));
			} else {
				this.logger.trace(`${e.getName()} is not eligible to execute this time.`);
			}

		}

		// Execute all run promises, each of which returns an array of RuleResults, then concatenate
		// all of the results together from all engines into one report.
		try {
			const psResults: RuleResult[][] = await Promise.all(ps);
			psResults.forEach(r => results = results.concat(r));
			this.logger.trace(`Received rule violations: ${results}`);
			this.logger.trace(`Recombining results into requested format ${format}`);
			return await RuleResultRecombinator.recombineAndReformatResults(results, format);
		} catch (e) {
			throw new SfdxError(e.message || e);
		}
	}

	/**
	 * Given a simple list of top-level targets and the engine to be executed, retrieve the full file listing
	 * to target.
	 * 1. If a target has a pattern (i.e. hasMagic) resolve it using globby.
	 * 2. If a target is a directory, get its contents using the target patterns specified for the engine.
	 * 3. If the target is a file, make sure it matches the engine's target patterns.
	 */
	private async unpackTargets(engine: RuleEngine, targets: string[]): Promise<RuleTarget[]> {
		const ruleTargets: RuleTarget[] = [];
		for (const target of targets) {
			// Ask engines for their desired target patterns.
			const targetPatterns: string[] = await engine.getTargetPatterns(target);
			assert(targetPatterns);
			// Picomatch OR's together the patterns you provide it, which means we need to be careful about how we construct
			// our matchers.
			// Inclusion patterns denote files we want to include, and we want all files that match even a single pattern,
			// so we can just create a single matcher for all inclusion patterns.
			const inclusionMatcher = picomatch(targetPatterns.filter(p => !p.startsWith("!")));
			// Exclusion patterns denote files we want to exclude, and we want all files that match every single pattern,
			// i.e., doesn't match the inversion of any patterns. So we invert each exclusion pattern and combine them
			// into a single matcher whose value we'll negate when we need it.
			const exclusionMatcher = picomatch(targetPatterns.filter(p => p.startsWith("!")).map(p => p.slice(1)));

			const fileExists = await this.fileHandler.exists(target);
			if (globby.hasMagic(target)) {
				// The target is a magic globby glob.  Retrieve paths in the working dir that match it, and then
				// filter each with the engine's own patterns.  First test any inclusive patterns, then AND them with
				// any exclusive patterns.
				const matchingTargets = await globby(target);
				// Map relative files to absolute paths. This solves ambiguity of current working directory
				const absoluteMatchingTargets = matchingTargets.map(t => path.resolve(t));
				const ruleTarget = {
					target,
					paths: absoluteMatchingTargets.filter(t => inclusionMatcher(t) && !exclusionMatcher(t))
				};
				if (ruleTarget.paths.length > 0) {
					ruleTargets.push(ruleTarget);
				}
			} else {
				if (fileExists) {
					const stats: Stats = await this.fileHandler.stats(target);
					if (stats.isDirectory()) {
						// The target is a directory.  If the engine has target patterns, which is always should,
						// call globby with the directory as the working dir, and use the patterns to match its contents.
						if (targetPatterns) {
							// If dir, use globby { cwd: process.cwd() } option
							const relativePaths = await globby(targetPatterns, {cwd: target});
							// Join the relative path to the files that were found
							const joinedPaths = relativePaths.map(t => path.join(target, t));
							// Resolve the relative paths to their absolute paths
							const absolutePaths = joinedPaths.map(t => path.resolve(t));
							ruleTargets.push({target, isDirectory: true, paths: absolutePaths});
						} else {
							// Without target patterns for the engine, just add the dir itself and hope for the best.
							ruleTargets.push({target, isDirectory: true, paths: ["."]});
						}
					} else {
						// The target is a simple file.  Validate it against the engine's own patterns.  First test
						// any inclusive patterns, then with any exclusive patterns.
						const absolutePath = path.resolve(target);
						if (inclusionMatcher(absolutePath) && !exclusionMatcher(absolutePath)) {
							ruleTargets.push({target, paths: [absolutePath]});
						}
					}
				}
			}
		}
		return ruleTargets;
	}
}
