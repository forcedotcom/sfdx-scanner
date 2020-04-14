import {SfdxError, Logger} from '@salesforce/core';
import {AsyncCreatable} from '@salesforce/kit';
import {Rule} from '../types';
import {PmdCatalogWrapper} from './pmd/PmdCatalogWrapper';
import PmdWrapper from './pmd/PmdWrapper';
import {RuleResultRecombinator} from './RuleResultRecombinator';
import * as PrettyPrinter from './util/PrettyPrinter';

export enum RULE_FILTER_TYPE {
	RULENAME,
	CATEGORY,
	RULESET,
	LANGUAGE,
	SOURCEPACKAGE
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

export class RuleManager extends AsyncCreatable {
	private pmdCatalogWrapper: PmdCatalogWrapper;
	private logger: Logger;

	protected async init(): Promise<void> {
		this.pmdCatalogWrapper = await PmdCatalogWrapper.create({});
		this.logger = await Logger.child('RuleManager');
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
		// If target is a string, it means it's an alias for an org, instead of a bunch of local filepaths. We can't handle
		// running rules against an org yet, so we'll just throw an error for now.
		if (typeof target === 'string') {
			throw new SfdxError('Running rules against orgs is not yet supported');
		}
		// TODO: Eventually, we'll need a bunch more promises to run rules existing in other engines.
		const [pmdResults] = await Promise.all([this.runPmdRulesMatchingCriteria(filters, target)]);
		this.logger.trace(`Received rule violations: ${pmdResults}`);

		// Once all of the rules finish running, we'll need to combine their results into a single set of the desired type,
		// which we can then return.
		this.logger.trace(`Recombining results into requested format ${format}`);
		return RuleResultRecombinator.recombineAndReformatResults([pmdResults], format);
	}

	private async getAllRules(): Promise<Rule[]> {
		this.logger.trace('Getting all rules.');

		// TODO: Eventually, we'll need a bunch more promises to load rules from their source files in other engines.
		const [pmdRules]: Rule[][] = await Promise.all([this.getPmdRules()]);
		return [...pmdRules];
	}

	private async getPmdRules(): Promise<Rule[]> {
		// PmdCatalogWrapper is a layer of abstraction between the commands and PMD, facilitating code reuse and other goodness.
		this.logger.trace('Getting PMD rules.');
		const catalog = await this.pmdCatalogWrapper.getCatalog();
		return catalog.rules;
	}

	private async runPmdRulesMatchingCriteria(filters: RuleFilter[], target: string[]): Promise<string> {
		this.logger.trace(`About to run PMD rules. Target count: ${target.length}, filter count: ${filters.length}`);
		try {
			// Convert our filters into paths that we can feed back into PMD.
			const paths: string[] = await this.pmdCatalogWrapper.getPathsMatchingFilters(filters);
			// If we didn't find any paths, we're done.
			if (paths == null || paths.length === 0) {
				this.logger.trace('No Rule paths found. Nothing to execute.')
				return '';
			}
			// Otherwise, run PMD and see what we get.
			// TODO: Weird translation to next layer. target=path and path=rule path. Consider renaming
			const [violationsFound, stdout] = await PmdWrapper.execute(target.join(','), paths.join(','));

			if (violationsFound) {
				this.logger.trace('Found rule violations.');
				// If we found any violations, they'll be in an XML document somewhere in stdout, which we'll need to find and process.
				const xmlStart = stdout.indexOf('<?xml');
				const xmlEnd = stdout.lastIndexOf('</pmd>') + 6;
				const ruleViolationsXml = stdout.slice(xmlStart, xmlEnd);

				this.logger.trace(`Rule violations in the original XML format: ${ruleViolationsXml}`);
				return ruleViolationsXml;
			} else {
				// If we didn't find any violations, we can just return an empty string.
				this.logger.trace('No rule violations found.');
				return '';
			}
		} catch (e) {
			throw new SfdxError(e.message || e);
		}
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
				case RULE_FILTER_TYPE.SOURCEPACKAGE:
					// Rules also only have one source package, so we'll turn it into a singleton list just like we do with 'name'.
					ruleValues = [rule.sourcepackage];
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
