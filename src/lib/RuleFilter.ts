import {SfError} from '@salesforce/core';
import { Catalog, Rule, RuleGroup } from '../types';
import {Bundle, getMessage} from "../MessageCatalog";

/**
 * Filter values preceded by this character are negated
 */
const NEGATION_CHAR = '!';

/**
 * Human readable string for this filter type. Programmatic use of this value is discouraged.
 */
enum FilterDisplayName {
	CATEGORY = 'Category',
	ENGINE = 'Engine',
	LANGUAGE = 'Language',
	RULENAME = 'Rule Name',
	RULESET = 'Ruleset',
	SOURCEPACKAGE = 'Source Package'
}

/**
 * Represents a filter that operates on RuleGroups
 */
export interface RuleGroupFilter {
	/**
	 * Return the RuleGroup array that is relevant to this filter from the catalog
	 */
	getRuleGroups(catalog: Catalog): RuleGroup[];
}

/**
 * Represents a filter that operates on Engines
 */
export interface EngineFilterInterface {
	/**
	 * Return the RuleGroup array that is relevant to this filter from the catalog
	 */
	getEngines(): readonly string[];
}

/**
 * Return true if object implements the RuleGroupFilter interface
 */
export function isRuleGroupFilter(object): object is RuleGroupFilter {
    return 'getRuleGroups' in object;
}

/**
 * Return true if object implements the EngineFilterInterface interface
 */
export function isEngineFilter(object): object is EngineFilterInterface {
    return 'getEngines' in object;
}

export abstract class RuleFilter {
	protected readonly filterValues: ReadonlyArray<string>;
	private readonly negated: boolean;

	protected constructor(filterValues: string[], negated: boolean) {
		this.filterValues = filterValues;
		this.negated = negated;
	}

	public matchesRuleGroup(ruleGroup: RuleGroup): boolean {
		if (this.isEmpty()) {
			return true;
		}

		const includes = this.filterValues.includes(ruleGroup.name);
		return this.negated ? !includes : includes;
	}

	public matchesRule(rule: Rule): boolean {
		if (this.isEmpty()) {
			return true;
		}

		// Positive filtering a rule includes those rules that aren't enabled by default
		// This ensures that negative filtering doesn't include default disabled rules
		if (this.negated && !rule.defaultEnabled) {
			return false;
		}

		const ruleValues = this.getRuleValues(rule);
		const includes = this.filterValues.some(v => ruleValues.includes(v));
		return this.negated ? !includes : includes;
	}

	public prettyPrint(): string {
		return `RuleFilter[filterType=${this.constructor.name}, filterValues=${this.filterValues.toString()}, negated=${this.negated.toString()}]`;
	}

	/**
	 * Extract rule values relevant to this filter and return as a string array.
	 * Single values should be returned as a one length array.
	 */
	protected abstract getRuleValues(rule: Rule): string[];

	private isEmpty(): boolean {
		return (!this.filterValues || this.filterValues.length === 0);
	}

	protected static processForPosAndNegFilterValues(filterValues: string[]): {positive: string[]; negative: string[]} {
		filterValues = filterValues.map(v => v.trim());
		const positive = filterValues.filter(v => !v.startsWith(NEGATION_CHAR));
		const negative = filterValues.filter(v => v.startsWith(NEGATION_CHAR)).map(v => v.slice(1));
		return {positive: positive, negative: negative};
	}
}

/**
 * This class will throw an exception if any of the filter values are negated
 */
abstract class PositiveRuleFilter extends RuleFilter {
    protected constructor(filterDisplayName: FilterDisplayName, filterValues: string[]) {
		const mapped = RuleFilter.processForPosAndNegFilterValues(filterValues);
		if (mapped.negative.length > 0) {
			throw new SfError(getMessage(Bundle.Exceptions, 'RuleFilter.PositiveOnly', [filterDisplayName]));
		}
		super(mapped.positive, false);
	}
}

/**
 * This class will parse filter values for positive and negative filters. Mixed types will throw an exception.
 */
abstract class NegateableRuleFilter extends RuleFilter {
    protected constructor(filterDisplayName: FilterDisplayName, filterValues: string[]) {
		const mapped = RuleFilter.processForPosAndNegFilterValues(filterValues);

		// Throw an exception if there are mixed types
		if (mapped.positive.length > 0 && mapped.negative.length > 0) {
			throw new SfError(getMessage(Bundle.Exceptions, 'RuleFilter.MixedTypes', [filterDisplayName]));
		}

		const negated = mapped.negative.length > 0;
		super(negated ? mapped.negative : mapped.positive, negated);
	}
}

export class EngineFilter extends PositiveRuleFilter implements EngineFilterInterface {
    constructor(filterValues: string[]) {
		super(FilterDisplayName.ENGINE, filterValues);
	}

	protected getRuleValues(rule: Rule): string[] {
		// Rules have one engine, so we'll turn it into a singleton list
		return [rule.engine];
	}

	public getEngines(): readonly string[] {
		return this.filterValues;
	}
}

export class LanguageFilter extends PositiveRuleFilter {
    constructor(filterValues: string[]) {
		super(FilterDisplayName.LANGUAGE, filterValues);
	}

	protected getRuleValues(rule: Rule): string[] {
		return rule.languages;
	}
}

export class RulenameFilter extends PositiveRuleFilter {
    constructor(filterValues: string[]) {
		super(FilterDisplayName.RULENAME, filterValues);
	}

	protected getRuleValues(rule: Rule): string[] {
		// Rules have one name, so we'll turn it into a singleton list
		return [rule.name];
	}
}

export class RulesetFilter extends PositiveRuleFilter implements RuleGroupFilter {
    constructor(filterValues: string[]) {
		super(FilterDisplayName.RULESET, filterValues);
	}

	protected getRuleValues(rule: Rule): string[] {
		return rule.rulesets;
	}

	public getRuleGroups(catalog: Catalog): RuleGroup[] {
		return catalog.rulesets;
	}
}

export class SourcePackageFilter extends PositiveRuleFilter {
    constructor(filterValues: string[]) {
		super(FilterDisplayName.SOURCEPACKAGE, filterValues);
	}

	protected getRuleValues(rule: Rule): string[] {
		// Rules have one sourcepackage, so we'll turn it into a singleton list
		return [rule.sourcepackage];
	}
}

export class CategoryFilter extends NegateableRuleFilter implements RuleGroupFilter {
	constructor(filterValues: string[]) {
		super(FilterDisplayName.CATEGORY, filterValues);
	}

	protected getRuleValues(rule: Rule): string[] {
		return rule.categories;
	}

	public getRuleGroups(catalog: Catalog): RuleGroup[] {
		return catalog.categories;
	}
}
