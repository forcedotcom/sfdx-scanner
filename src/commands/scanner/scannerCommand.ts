import {SfdxCommand} from '@salesforce/command';
import {RULE_FILTER_TYPE, RuleFilter} from '../../lib/RuleManager';

export abstract class ScannerCommand extends SfdxCommand {

  protected buildRuleFilters(): RuleFilter[] {
    const filters: RuleFilter[] = [];
    // Create a filter for any provided categories.
    if ((this.flags.category || []).length > 0) {
      filters.push(new RuleFilter(RULE_FILTER_TYPE.CATEGORY, this.flags.category));
    }

    // Create a filter for any provided rulesets.
    if ((this.flags.ruleset || []).length > 0) {
      filters.push(new RuleFilter(RULE_FILTER_TYPE.RULESET, this.flags.ruleset));
    }

    // Create a filter for any provided languages.
    if ((this.flags.language || []).length > 0) {
      filters.push(new RuleFilter(RULE_FILTER_TYPE.LANGUAGE, this.flags.language));
    }

    // Create a filter for provided rule names.
    // NOTE: Only a single rule name can be provided. It will be treated as a singleton list.
    if (this.flags.rulename) {
      filters.push(new RuleFilter(RULE_FILTER_TYPE.RULENAME, [this.flags.rulename]));
    }

    return filters;
  }
}
