import {Logger, Messages, SfdxError} from '@salesforce/core';
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
import {uxEvents} from './ScannerEvents';

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'DefaultRuleManager');

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

	/**
	 * Returns rules matching the filter criteria provided, and any non-conflicting implicit filters.
	 * @param {RuleFilter[]} filters - A collection of filters.
	 */
	async getRulesMatchingCriteria(filters: RuleFilter[]): Promise<Rule[]> {
		const engineNames: string[] = (await this.resolveEngineFilters(filters)).map(e => e.getName());
		const rules: Rule[] = this.catalog.getRulesMatchingFilters(filters);
		return rules.filter(r => engineNames.includes(r.engine));
	}

	/**
	 * Returns rules that match only the provided filters, completely ignoring any implicit filtering.
	 * @param filters
	 */
	getRulesMatchingOnlyExplicitCriteria(filters: RuleFilter[]): Promise<Rule[]> {
		return Promise.resolve(this.catalog.getRulesMatchingFilters(filters));
	}

	async runRulesMatchingCriteria(filters: RuleFilter[], targets: string[], format: OUTPUT_FORMAT, engineOptions: Map<string, string>): Promise<RecombinedRuleResults> {
		let results: RuleResult[] = [];

		// Derives rules from our filters to feed the engines.
		const ruleGroups: RuleGroup[] = this.catalog.getRuleGroupsMatchingFilters(filters);
		const rules: Rule[] = this.catalog.getRulesMatchingFilters(filters);
		const ps: Promise<RuleResult[]>[] = [];
		const engines: RuleEngine[] = await this.resolveEngineFilters(filters);
		const matchedTargets: Set<string> = new Set<string>();
		for (const e of engines) {
			// For each engine, filter for the appropriate groups and rules and targets, and pass
			// them all in. Note that some engines (pmd) need groups while others (eslint) need the rules.
			const engineGroups = ruleGroups.filter(g => g.engine === e.getName());
			const engineRules = rules.filter(r => r.engine === e.getName());
			const engineTargets = await this.unpackTargets(e, targets, matchedTargets);
			this.logger.trace(`For ${e.getName()}, found ${engineGroups.length} groups, ${engineRules.length} rules, ${engineTargets.length} targets`);
			
			if (e.shouldEngineRun(engineGroups, engineRules, engineTargets, engineOptions)) {
				this.logger.trace(`${e.getName()} is eligible to execute.`);
				ps.push(e.run(engineGroups, engineRules, engineTargets, engineOptions));
			} else {
				this.logger.trace(`${e.getName()} is not eligible to execute this time.`);
			}

		}

		// Warn the user if any positive targets were skipped
		const unmatchedTargets = targets.filter(t => !t.startsWith('!') && !matchedTargets.has(t));

		if (unmatchedTargets.length > 0) {
			const warningKey = unmatchedTargets.length === 1 ? 'warning.targetSkipped' : 'warning.targetsSkipped';
			uxEvents.emit('warning-always', messages.getMessage(warningKey, [`${unmatchedTargets.join(', ')}`]));
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

	protected async resolveEngineFilters(filters: RuleFilter[]): Promise<RuleEngine[]> {
		let filteredEngineNames: readonly string[] = null;
		for (const filter of filters) {
			if (filter.filterType === FilterType.ENGINE) {
				filteredEngineNames = filter.filterValues;
				break;
			}
		}
		// If there are any engine-specific filter criteria, return those engines whether they're enabled or not. Otherwise,
		// just return any enabled engines.
		// This lets us quietly introduce new engines by making them disabled by default but still available if explicitly
		// specified.
		return filteredEngineNames ? Controller.getFilteredEngines(filteredEngineNames as string[]) : Controller.getEnabledEngines();
	}

	/**
	 * Given a simple list of top-level targets and the engine to be executed, retrieve the full file listing
	 * to target.
	 * 1. If a target has a pattern (i.e. hasMagic) resolve it using globby.
	 * 2. If a target is a directory, get its contents using the target patterns specified for the engine.
	 * 3. If the target is a file, make sure it matches the engine's target patterns.
	 *
	 * Any items from the 'targets' array that result in a match are added to the 'matchedTargets' Set.
	 */
	private async unpackTargets(engine: RuleEngine, targets: string[], matchedTargets: Set<string>): Promise<RuleTarget[]> {
		const ruleTargets: RuleTarget[] = [];
		// Ask engines for their desired target patterns.
		const engineTargets = await engine.getTargetPatterns();
		assert(engineTargets);
		// We also need to do a bit of processing on the patterns we were given.
		const positivePatterns = [];
		const negativePatterns = [];
		// This regex will help us identify and resolve relative paths.
		const dotNotationRegex = /(\.{1,2}\/)+/;
		targets.forEach((t) => {
			if (t.startsWith('!**') || t.startsWith('!/')) {
				// If a negative glob starts with a ** or /, it's an absolute path and we can just add it to our array.
				negativePatterns.push(t);
			} else if (t.startsWith('!')) {
				// We should turn relative negative globs into absolute globs.
				// First, identify whether this glob contains any dot-notation (e.g., ./ or ../../).
				const dotNotationDescriptor = dotNotationRegex.exec(t);
				// If the regex found anything, that's our dot notation prefix. Otherwise, there's an implicit prefix of '.'.
				const dotNotationPrefix = dotNotationDescriptor ? dotNotationDescriptor[0] :  '.';
				// The actual glob-portion of the target starts at the end of the dot notation stuff (or just immediately
				// after the exclamation point if no dot notation was used).
				const globStartPoint = (dotNotationDescriptor ? dotNotationPrefix.length : 0) + 1;
				// Resolve the dot notation prefix into an actual path.
				const resolvedRelativePath = path.resolve(dotNotationPrefix);
				// Construct a new glob using an exclamation point, our resolved dot-prefix, and the glob portion of the
				// original.
				negativePatterns.push(`!${resolvedRelativePath}/${t.slice(globStartPoint)}`);
			} else {
				// Everything else is a positive pattern.
				positivePatterns.push(t);
			}
		});

		// We want to use a path matcher that can filter based on the engine's target patterns and any negative globs
		// provided to us.
		const pm = new PathMatcher([...engineTargets, ...negativePatterns]);
		for (const target of positivePatterns) {
			// Used to detect if the target resulted in a match
			const startLength: number = ruleTargets.length;
			if (globby.hasMagic(target)) {
				// The target is a magic glob. Retrieve paths in the working directory that match it, and then filter against
				// our pattern matcher.
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
					// If the target is a directory, we should get everything in it, convert relative paths to absolute
					// paths, and then filter based our matcher.
					const relativePaths = await globby(target);
					ruleTargets.push({target, isDirectory: true, paths: pm.filterPathsByPatterns(relativePaths.map(t => path.resolve(t)))});
				} else {
					// The target is just a file. Validate it against our matcher, and add it if eligible.
					const absolutePath = path.resolve(target);
					if (pm.pathMatchesPatterns(absolutePath)) {
						ruleTargets.push({target, paths: [absolutePath]});
					}
				}
			}

			if (startLength !== ruleTargets.length) {
				matchedTargets.add(target);
			}
		}
		return ruleTargets;
	}
}
