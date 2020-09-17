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
import {PathMatcher} from './util/PathMatcher';
import {Controller} from '../Controller';
import globby = require('globby');
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
		// The target patterns provided by the engine's config form the basis of our pattern matching.
		let filterPatterns: string[] = await engine.getTargetPatterns();
		assert(filterPatterns);
		// We'll also add any negative globs we were given to the filterPatterns, and then we can generate our matcher.
		targets.forEach((t) => {
			if (t.startsWith('!**') || t.startsWith('!/')) {
				// If the glob begins with !** or !/, then its path is absolute and we can just add it directly to the
				// list with no changes.
				filterPatterns = [...filterPatterns, t];
			} else if (t.startsWith('!')) {
				// If the glob starts with '!' but is a relative path, we should convert it to an absolute path by just
				// adding in a ** at the start. We could have chosen to convert it by prepending the current working directory
				// instead, but this is so much easier so it's what we're doing. If customers complain loudly enough,
				// we should feel free to change it.
				filterPatterns = [...filterPatterns, '!**/'+ t.slice(1)];
			}
		});
		const pm = new PathMatcher(filterPatterns);

		// Now, we'll iterate over all of the positive globs, files, and directories in the targets.
		for (const target of targets) {
			// Skip negative globs, since we've already covered them.
			if (target.startsWith('!')) {
				continue;
			}
			if (globby.hasMagic(target) && !target.startsWith('!')) {
				// The target is a magic globby glob. Retrieve paths in the working dir that match it, and then filter
				// each with the config patterns and negative globs.
				const matchingTargets = await globby(target);
				// Map relative files to absolute paths. This solves ambiguity of current working directory
				const absoluteMatchingTargets = matchingTargets.map(t => path.resolve(t));
				// Filter the targets based on our target patterns.
				const filteredTargets = pm.filterPathsByPatterns(absoluteMatchingTargets);
				const ruleTarget = {
					target,
					paths: filteredTargets
				};
				if (ruleTarget.paths.length > 0) {
					ruleTargets.push(ruleTarget);
				}
			} else if (await this.fileHandler.exists(target)) {
				const stats: Stats = await this.fileHandler.stats(target);
				if (stats.isDirectory()) {
					// For directories, if the engine has any filter patterns (which it always should), call globby with
					// the specified directory as the working dir, and use the filter patterns to match its contents.
					if (filterPatterns) {
						// If dir, use globby { cwd: process.cwd() } option
						const relativePaths = await globby(filterPatterns, {cwd: target});
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
					if (pm.pathMatchesPatterns(absolutePath)) {
						ruleTargets.push({target, paths: [absolutePath]});
					}
				}
			}
		}
		return ruleTargets;
	}
}
