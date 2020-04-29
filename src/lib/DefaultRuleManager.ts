import {Logger, SfdxError} from '@salesforce/core';
import {inject, injectable, injectAll} from 'tsyringe';
import {Rule, RuleGroup, RuleResult} from '../types';
import {RuleFilter} from './RuleFilter';
import {OUTPUT_FORMAT, RuleManager} from './RuleManager';
import {RuleResultRecombinator} from './RuleResultRecombinator';
import {RuleCatalog} from './services/RuleCatalog';
import {RuleEngine} from './services/RuleEngine';

@injectable()
export class DefaultRuleManager implements RuleManager {
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
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child('DefaultManager');
		for (const engine of this.engines) {
			await engine.init();
		}
		await this.catalog.init();

		this.initialized = true;
	}

	async getRulesMatchingCriteria(filters: RuleFilter[]): Promise<Rule[]> {
		return this.catalog.getRulesMatchingFilters(filters);
	}

	async runRulesMatchingCriteria(filters: RuleFilter[], target: string[] | string, format: OUTPUT_FORMAT): Promise<string | { columns; rows }> {
		// If target is a string, it means it's an alias for an org, instead of a bunch of local file paths. We can't handle
		// running rules against an org yet, so we'll just throw an error for now.
		if (typeof target === 'string') {
			throw new SfdxError('Running rules against orgs is not yet supported');
		}

		let results: RuleResult[] = [];

		// Derives rules from our filters to feed the engines.
		const ruleGroups: RuleGroup[] = await this.catalog.getRuleGroupsMatchingFilters(filters);
		const rules: Rule[] = await this.catalog.getRulesMatchingFilters(filters);
		const ps: Promise<RuleResult[]>[] = [];
		for (const e of this.engines) {
			const engineGroups = ruleGroups.filter(g => g.engine === e.getName());
			const engineRules = rules.filter(r => r.engine === e.getName());
			ps.push(e.run(engineGroups, engineRules, target));
		}
		const psResults: RuleResult[][] = await Promise.all(ps);
		psResults.forEach(r => results = results.concat(r));
		this.logger.trace(`Received rule violations: ${results}`);
		this.logger.trace(`Recombining results into requested format ${format}`);
		return RuleResultRecombinator.recombineAndReformatResults(results, format);
	}
}
