import {Logger, SfdxError} from '@salesforce/core';
import {inject, injectable, injectAll} from 'tsyringe';
import {NamedPaths, Rule} from '../types';
import {RuleFilter} from './RuleFilter';
import {OUTPUT_FORMAT, RuleManager} from './RuleManager';
import {RuleResultRecombinator} from './RuleResultRecombinator';
import {RuleCatalog} from './services/RuleCatalog';
import {RuleEngine} from './services/RuleEngine';

@injectable()
export class DefaultManager implements RuleManager {
	private logger: Logger;

	// noinspection JSMismatchedCollectionQueryUpdate
	private readonly engines: RuleEngine[];
	private readonly catalog: RuleCatalog;
	private initialized: boolean;

	constructor(
		@injectAll("RuleEngine") engines?: RuleEngine[],
		@inject("RuleCatalog") catalog?: RuleCatalog
	) {
		this.engines = engines;
		this.catalog = catalog;
	}

	async init(): Promise<void> {
		if (this.initialized) return;

		await Promise.all(this.engines.map(e => e.init()));
		await this.catalog.init();
		this.logger = await Logger.child('DefaultManager');

		this.initialized = true;
	}

	async getRulesMatchingCriteria(filters: RuleFilter[]): Promise<Rule[]> {
		return this.catalog.getRulesMatchingFilters(filters);
	}

	async runRulesMatchingCriteria(filters: RuleFilter[], target: string[] | string, format: OUTPUT_FORMAT): Promise<string> {
		// If target is a string, it means it's an alias for an org, instead of a bunch of local file paths. We can't handle
		// running rules against an org yet, so we'll just throw an error for now.
		if (typeof target === 'string') {
			throw new SfdxError('Running rules against orgs is not yet supported');
		}

		// Convert our filters into paths that we can feed to the engines. If we didn't find any paths, we're done.
		const paths: NamedPaths[] = await this.catalog.getNamedPathsMatchingFilters(filters);
		if (paths == null || paths.length === 0) {
			this.logger.trace('No Rule paths found. Nothing to execute.');
			return '';
		}

		const ps = this.engines.map(e => e.run(paths, target));
		const [results] = await Promise.all(ps);
		this.logger.trace(`Received rule violations: ${results}`);

		// Once all of the rules finish running, we'll need to combine their results into a single set of the desired type,
		// which we can then return.
		this.logger.trace(`Recombining results into requested format ${format}`);
		return RuleResultRecombinator.recombineAndReformatResults([results], format);
	}
}
