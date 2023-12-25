import {Logger, SfError} from '@salesforce/core';
import * as assert from 'assert';
import {Stats} from 'fs';
import {inject, injectable} from 'tsyringe';
import {
	EngineExecutionDescriptor,
	Rule,
	RuleGroup,
	RuleResult,
	RuleTarget,
	TelemetryData
} from '../types';
import {isEngineFilter, RuleFilter} from './RuleFilter';
import {EngineOptions, RuleManager, RunOptions} from './RuleManager';
import {RuleCatalog} from './services/RuleCatalog';
import {RuleEngine} from './services/RuleEngine';
import {FileHandler} from './util/FileHandler';
import {PathMatcher} from './util/PathMatcher';
import {Controller} from '../Controller';
import {EVENTS, uxEvents} from './ScannerEvents';
import {CONFIG_FILE, ENGINE, TargetType} from '../Constants';
import * as TelemetryUtil from './util/TelemetryUtil';
import globby = require('globby');
import path = require('path');
import {BundleName, getMessage} from "../MessageCatalog";
import {Results} from "./output/Results";

type RunDescriptor = {
	engine: RuleEngine;
	descriptor: EngineExecutionDescriptor;
};

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
	getRulesMatchingOnlyExplicitCriteria(filters: RuleFilter[]): Rule[] {
		return this.catalog.getRulesMatchingFilters(filters);
	}

	async runRulesMatchingCriteria(filters: RuleFilter[], targets: string[], runOptions: RunOptions, engineOptions: EngineOptions): Promise<Results> {
		// Declare a variable that we can later use to store the engine results, as well as something to help us track
		// which engines actually ran.
		let ruleResults: RuleResult[] = [];
		const executedEngines: Set<string> = new Set();

		// Derives rules from our filters to feed the engines.
		const engines: RuleEngine[] = await this.resolveEngineFilters(filters, engineOptions, runOptions);
		const ruleGroups: RuleGroup[] = await this.catalog.getRuleGroupsMatchingFilters(filters, engines);
		const filteredRules: Rule[] = this.catalog.getRulesMatchingFilters(filters);
		// If the run doesn't allow pilot rules, filter those now.
		const rules = runOptions.withPilot ? filteredRules : filteredRules.filter(r => !r.isPilot);
		const runDescriptorList: RunDescriptor[] = [];
		const matchedTargets: Set<string> = new Set<string>();

		// Storing the paths from eslint and eslint-lwc to track if any are processed by both.
		const paths: Map<string, string[]> = new Map([
			[ENGINE.ESLINT, [] as string[]],
			[ENGINE.ESLINT_LWC, [] as string[]],
		]);

		for (const e of engines) {
			// For each engine, filter for the appropriate groups and rules and targets, and pass
			// them all in. Note that some engines (pmd) need groups while others (eslint) need the rules.
			const engineGroups = ruleGroups.filter(g => g.engine === e.getName());
			const engineRules = rules.filter(r => r.engine === e.getName());
			const engineTargets = await this.unpackTargets(e, targets, matchedTargets);
			this.logger.trace(`For ${e.getName()}, found ${engineGroups.length} groups, ${engineRules.length} rules, ${engineTargets.length} targets`);

			if (e.shouldEngineRun(engineGroups, engineRules, engineTargets, engineOptions)) {
				this.logger.trace(`${e.getName()} is eligible to execute.`);
				executedEngines.add(e.getName());
				// Create a descriptor for this engine run, but do not actually run it just yet. This is because the run
				// itself must begin inside of a try-catch.
				runDescriptorList.push({
					engine: e,
					descriptor: {
						ruleGroups: engineGroups,
						rules: engineRules,
						target: engineTargets,
						engineOptions: engineOptions,
						normalizeSeverity: runOptions.normalizeSeverity
					}
				});
			} else {
				this.logger.trace(`${e.getName()} is not eligible to execute this time.`);
			}

			if (paths.has(e.getName())) {
				for (const t of engineTargets) {
					for (const p of t.paths) {
						paths.get(e.getName()).push(p);
					}
				}
			}
		}

		// Checking if any file paths were processed by eslint and eslint-lwc, which may cause duplicate violations.
		const pathsDoubleProcessed = paths.get(ENGINE.ESLINT).filter(path => paths.get(ENGINE.ESLINT_LWC).includes(path));
		if (pathsDoubleProcessed.length > 0) {
			const numFilesShown = 3;
			const filesToDisplay = pathsDoubleProcessed.slice(0, numFilesShown);
			if (pathsDoubleProcessed.length > numFilesShown) {
				filesToDisplay.push(`and ${pathsDoubleProcessed.length - numFilesShown} more`)
			}
			uxEvents.emit(EVENTS.WARNING_ALWAYS, getMessage(BundleName.DefaultRuleManager, 'warning.pathsDoubleProcessed', [`${Controller.getSfdxScannerPath()}/${CONFIG_FILE}`, `${filesToDisplay.join(', ')}`]));
		}


		this.validateRunDescriptors(runDescriptorList);
		await this.emitRunTelemetry(runDescriptorList, runOptions.sfVersion);
		// Warn the user if any positive targets were skipped
		const unmatchedTargets = targets.filter(t => !t.startsWith('!') && !matchedTargets.has(t));

		if (unmatchedTargets.length > 0) {
			const warningKey = unmatchedTargets.length === 1 ? 'warning.targetSkipped' : 'warning.targetsSkipped';
			uxEvents.emit(EVENTS.WARNING_ALWAYS, getMessage(BundleName.DefaultRuleManager, warningKey, [`${unmatchedTargets.join(', ')}`]));
		}

		// Execute all run promises, each of which returns an array of RuleResults, then concatenate
		// all of the results together from all engines into one set of results.
		try {
			// Now that we're inside of a try-catch, we can turn the run descriptors into actual executions.
			const ps: Promise<RuleResult[]>[] = runDescriptorList.map(({engine, descriptor}) => engine.runEngine(descriptor));
			const psResults: RuleResult[][] = await Promise.all(ps);
			psResults.forEach(r => ruleResults = ruleResults.concat(r));
			this.logger.trace(`Received rule violations: ${JSON.stringify(ruleResults)}`);

			return new Results(ruleResults, executedEngines);

		} catch (e) {
			const message: string = e instanceof Error ? e.message : e as string;
			throw new SfError(message);
		}
	}

	protected validateRunDescriptors(runDescriptorList: RunDescriptor[]): void {
		// If any engine is DFA, then all of them have to be DFA.
		const dfaEngines = runDescriptorList.filter(descriptor => descriptor.engine.isDfaEngine()).map(descriptor => descriptor.engine.getName());
		const pathlessEngines = runDescriptorList.filter(descriptor => !(descriptor.engine.isDfaEngine())).map(descriptor => descriptor.engine.getName());
		if (dfaEngines.length > 0 && pathlessEngines.length > 0) {
			throw new SfError(getMessage(BundleName.DefaultRuleManager, 'error.cannotRunDfaAndNonDfaConcurrently', [JSON.stringify(dfaEngines), JSON.stringify(pathlessEngines)]));
		}
	}

	protected async emitRunTelemetry(runDescriptorList: RunDescriptor[], sfVersion: string): Promise<void> {
		// Get the name of every engine being executed.
		const executedEngineNames: Set<string> = new Set(runDescriptorList.map(d => d.engine.getName().toLowerCase()));
		// Build the base telemetry data.
		const runTelemetryObject: TelemetryData = {
			// This property is a requirement for the object.
			eventName: 'ENGINE_EXECUTION',
			// Knowing how many engines are run with each execution is valuable data.
			executedEnginesCount: executedEngineNames.size,
			// Creating a string of all the executed engines would yield data useful for metrics.
			// Note: Calling `.sort()` without an argument causes a simple less-than to be used.
			executedEnginesString: JSON.stringify([...executedEngineNames.values()].sort()),
			sfVersion
		};

		const allEngines: RuleEngine[] = await Controller.getAllEngines();
		for (const engine of allEngines) {
			const engineName = engine.getName().toLowerCase();
			// In addition to the string, assign each engine a boolean indicating whether it was executed. This will allow
			// us to perform other kinds of analytics than the string.
			runTelemetryObject[engineName] = executedEngineNames.has(engineName);
		}
		// NOTE: In addition to the information that we added here, the following useful information is always captured
		// by default:
		// - node version
		// - plugin version
		// - executed command (e.g., `scanner run`)
		await TelemetryUtil.emitTelemetry(runTelemetryObject);
	}

	/**
	 * Returns a list of engines that match the provided filter criteria.
	 * Additionally, if a {@link RunOptions} object is provided, then only engines
	 * eligible to run under those options will be returned.
	 * @param filters
	 * @param engineOptions
	 * @param [runOptions]
	 * @protected
	 */
	protected async resolveEngineFilters(filters: RuleFilter[], engineOptions: Map<string,string> = new Map(), runOptions?: RunOptions): Promise<RuleEngine[]> {
		let filteredEngineNames: readonly string[] = null;
		for (const filter of filters) {
			if (isEngineFilter(filter)) {
				filteredEngineNames = filter.getEngines();
				break;
			}
		}
		// If there are any engine-specific filter criteria, return those engines whether they're enabled or not. Otherwise,
		// just return any enabled engines.
		// This lets us quietly introduce new engines by making them disabled by default but still available if explicitly
		// specified.
		const enginesMatchingFilter = await (filteredEngineNames
			? Controller.getFilteredEngines(filteredEngineNames as string[], engineOptions)
			: Controller.getEnabledEngines(engineOptions));
		return runOptions
			? enginesMatchingFilter.filter(e => e.isDfaEngine() == runOptions.runDfa)
			: enginesMatchingFilter;
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
	protected async unpackTargets(engine: RuleEngine, targets: string[], matchedTargets: Set<string>): Promise<RuleTarget[]> {
		const ruleTargets: RuleTarget[] = [];
		// Ask engines for their desired target patterns.
		const engineTargets = await engine.getTargetPatterns();
		assert(engineTargets);
		// We also need to do a bit of processing on the patterns we were given.
		const positivePatterns: string[] = [];
		const negativePatterns: string[] = [];
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
			const ruleTargetsInitialLength: number = ruleTargets.length;
			// Positive patterns might use method-level targeting. We only want to do path evaluation against the part
			// that's actually a path.
			const targetPortions = target.split('#');
			// The array will always have at least one entry, since if there's no '#' then it will return a singleton array.
			const targetPath = targetPortions[0];
			if (globby.hasMagic(target)) {
				// The target is a magic glob. Retrieve paths in the working directory that match it, and then filter against
				// our pattern matcher.
				const matchingTargets = await globby(targetPath);
				// Map relative files to absolute paths. This solves ambiguity of current working directory
				const absoluteMatchingTargets = matchingTargets.map(t => path.resolve(t));
				// Filter the targets based on our target patterns.
				const filteredTargets = await pm.filterPathsByPatterns(absoluteMatchingTargets);
				const ruleTarget: RuleTarget = {
					target: targetPath,
					targetType: TargetType.GLOB,
					paths: filteredTargets,
					methods: []
				};
				if (ruleTarget.paths.length > 0) {
					ruleTargets.push(ruleTarget);
				}
			} else if (await this.fileHandler.exists(targetPath)) {
				const stats: Stats = await this.fileHandler.stats(targetPath);
				if (stats.isDirectory()) {
					// If the target is a directory, we should get everything in it, convert relative paths to absolute
					// paths, and then filter based our matcher.
					const relativePaths = await globby(targetPath);
					const ruleTarget: RuleTarget = {
						target: targetPath,
						targetType: TargetType.DIRECTORY,
						paths: await pm.filterPathsByPatterns(relativePaths.map(t => path.resolve(t))),
						methods: []
					};
					if (ruleTarget.paths.length > 0) {
						ruleTargets.push(ruleTarget);
					}
				} else {
					// The target is just a file. Validate it against our matcher, and add it if eligible.
					const absolutePath = path.resolve(targetPath);
					if (await pm.pathMatchesPatterns(absolutePath)) {
						ruleTargets.push({
							target: targetPath,
							targetType: TargetType.FILE,
							paths: [absolutePath],
							// If the pattern has method-level targets, then they're delimited with a semi-colon.
							methods: targetPortions.length === 1 ? [] : targetPortions[1].split(';')
						});
					}
				}
			}

			if (ruleTargetsInitialLength !== ruleTargets.length) {
				matchedTargets.add(target);
			}
		}
		return ruleTargets;
	}
}
