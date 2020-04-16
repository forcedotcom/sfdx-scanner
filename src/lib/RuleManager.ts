import {Logger, SfdxError} from '@salesforce/core';
import {multiInject} from 'inversify';
import {Container, Services} from '../ioc.config';
import {Rule} from '../types';
import {RuleResultRecombinator} from './RuleResultRecombinator';
import {RuleEngine} from './services/RuleEngine';
import * as PrettyPrinter from './util/PrettyPrinter';

export enum RULE_FILTER_TYPE {
	RULENAME,
	CATEGORY,
	RULESET,
	LANGUAGE
}

export enum OUTPUT_FORMAT {
	XML = 'xml',
	JUNIT = 'junit',
	CSV = 'csv',
	TABLE = 'table'
}

export class RuleFilter {
	readonly filterType: RULE_FILTER_TYPE;
	readonly filterValues: ReadonlyArray<string>;

	constructor(filterType: RULE_FILTER_TYPE, filterValues: string[]) {
		this.filterType = filterType;
		this.filterValues = filterValues;
	}
}

export class RuleManager {
	public static async create(): Promise<RuleManager> {
		const engines = Container.getAll<RuleEngine>(Services.RuleEngine);
		const manager = new RuleManager(engines);
		await manager.init();
		return manager;
	}

	private logger: Logger;
	private readonly engines: RuleEngine[] = [];


	constructor(@multiInject("RuleEngine") engines: RuleEngine[]) {
		this.engines = engines;
	}

	protected async init(): Promise<void> {
		this.logger = await Logger.child('RuleManager');
		for (const e of this.engines) {
			await e.init();
		}
	}

	public async getRulesMatchingCriteria(filters: RuleFilter[]): Promise<Rule[]> {
		this.logger.trace(`Fetching rules that match the criteria ${PrettyPrinter.stringifyRuleFilters(filters)}`);

		try {
			const allRules = await this.getAllRules();
			const rulesThatMatchCriteria = allRules.filter(rule => this.ruleSatisfiesFilterConstraints(rule, filters));
			this.logger.trace(`Rules that match the criteria: ${PrettyPrinter.stringifyRules(rulesThatMatchCriteria)}`);
			return rulesThatMatchCriteria;
		} catch (e) {
			throw new SfdxError(e.message || e);
		}
	}

	public async runRulesMatchingCriteria(filters: RuleFilter[], target: string[] | string, format: OUTPUT_FORMAT): Promise<string> {
		// If target is a string, it means it's an alias for an org, instead of a bunch of local file paths. We can't handle
		// running rules against an org yet, so we'll just throw an error for now.
		if (typeof target === 'string') {
			throw new SfdxError('Running rules against orgs is not yet supported');
		}
		const ps = this.engines.map(e => e.run(filters, target));
		const [results] = await Promise.all(ps);
		this.logger.trace(`Received rule violations: ${results}`);

		// Once all of the rules finish running, we'll need to combine their results into a single set of the desired type,
		// which we can then return.
		this.logger.trace(`Recombining results into requested format ${format}`);
		return RuleResultRecombinator.recombineAndReformatResults([results], format);
	}

	private async getAllRules(): Promise<Rule[]> {
		this.logger.trace('Getting all rules.');

		const ps = this.engines.map(e => e.getAll());
		const [rules]: Rule[][] = await Promise.all(ps);
		return [...rules];
	}

	private ruleSatisfiesFilterConstraints(rule: Rule, filters: RuleFilter[]): boolean {
		// If no filters were provided, then the rule is vacuously acceptable and we can just return true.
		if (filters == null || filters.length === 0) {
			return true;
		}

		// Otherwise, we'll iterate over all provided criteria and make sure that the rule satisfies them.
		for (const filter of filters) {
			const filterType = filter.filterType;
			const filterValues = filter.filterValues;

			// Which property of the rule we're testing against depends on this filter's type.
			let ruleValues = null;
			switch (filterType) {
				case RULE_FILTER_TYPE.CATEGORY:
					ruleValues = rule.categories;
					break;
				case RULE_FILTER_TYPE.RULESET:
					ruleValues = rule.rulesets;
					break;
				case RULE_FILTER_TYPE.LANGUAGE:
					ruleValues = rule.languages;
					break;
				case RULE_FILTER_TYPE.RULENAME:
					// Rules only have one name, so we'll just turn that name into a singleton list so we can compare names the
					// same way we compare everything else.
					ruleValues = [rule.name];
					break;
			}

			// For each filter, one of the values it specifies as acceptable must be present in the rule's corresponding list.
			// e.g., if the user specified one or more categories, the rule must be a member of at least one of those categories.
			if (filterValues.length > 0 && !this.listContentsOverlap(filterValues, ruleValues)) {
				return false;
			}
		}
		// If we're at this point, it's because we looped through all of the filter criteria without finding a single one that
		// wasn't satisfied, which means the rule is good.
		return true;
	}

	private listContentsOverlap<T>(list1: ReadonlyArray<T>, list2: T[]): boolean {
		return list1.some(x => list2.includes(x));
	}
}
