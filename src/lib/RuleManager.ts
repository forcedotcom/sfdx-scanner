import {Rule} from '../types';
import {SfdxError} from '@salesforce/core';
import {PmdCatalogWrapper} from './pmd/PmdCatalogWrapper';

export enum RULE_FILTER_TYPE {
  CATEGORY,
  RULESET,
  LANGUAGE
}

export class RuleFilter {
  readonly filterType : RULE_FILTER_TYPE;
  readonly filterValues : ReadonlyArray<string>;

  constructor(filterType : RULE_FILTER_TYPE, filterValues : string[]) {
    this.filterType = filterType;
    this.filterValues = filterValues;
  }
}

export class RuleManager {

  public async getRulesMatchingCriteria(filters: RuleFilter[]) : Promise<Rule[]> {
    try {
      const allRules = await RuleManager.getAllRules();
      return allRules.filter(rule => this.ruleSatisfiesFilterConstraints(rule, filters));
    } catch (e) {
      throw new SfdxError(e);
    }
  }

  private static async getAllRules() : Promise<Rule[]> {
    // TODO: Eventually, we'll need a bunch more promises to load rules from their source files in other engines.
    const [pmdRules] : Rule[][] = await Promise.all([RuleManager.getPmdRules()]);
    return [...pmdRules];
  }

  private static async getPmdRules(): Promise<Rule[]> {
    // PmdCatalogWrapper is a layer of abstraction between the commands and PMD, facilitating code reuse and other goodness.
    const catalog = await new PmdCatalogWrapper().getCatalog();
    return catalog.rules;
  }


  private ruleSatisfiesFilterConstraints(rule : Rule, filters : RuleFilter[]) : boolean {
    // If no filters were provided, then the rule is vacuously acceptable and we can just return true.
    if (filters == null || filters.length == 0) {
      return true;
    }

    // Otherwise, we'll iterate over all provided criteria and make sure that the rule satisfies them.
    for (let i = 0; i < filters.length; i++) {
      let filterType = filters[i].filterType;
      let filterValues = filters[i].filterValues;

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

  private listContentsOverlap<T>(list1 : ReadonlyArray<T>, list2 : T[]) : boolean {
    return list1.some(x => list2.includes(x));
  }
}
