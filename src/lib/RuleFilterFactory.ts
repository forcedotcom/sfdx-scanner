import {Inputs} from "./../types";
import {CategoryFilter, EngineFilter, LanguageFilter, RuleFilter, RulenameFilter, RulesetFilter} from "./RuleFilter";
import {stringArrayTypeGuard} from "./util/Utils";

export interface RuleFilterFactory {
	createRuleFilters(inputs: Inputs): RuleFilter[];
}

export class RuleFilterFactoryImpl implements RuleFilterFactory {
	public createRuleFilters(inputs: Inputs): RuleFilter[] {
		const filters: RuleFilter[] = [];

		let usingDefaultEngines: boolean = true;

		// Create a filter for any provided engines.
		if (inputs.engine && stringArrayTypeGuard(inputs.engine) && inputs.engine.length) {
			filters.push(new EngineFilter(inputs.engine));
			usingDefaultEngines = false;
		}

		// Create a filter for any provided categories.
		if (inputs.category && stringArrayTypeGuard(inputs.category) && inputs.category.length) {
			filters.push(new CategoryFilter(inputs.category, usingDefaultEngines));
		}

		// Create a filter for any provided rulesets.
		if (inputs.ruleset && stringArrayTypeGuard(inputs.ruleset) && inputs.ruleset.length) {
			filters.push(new RulesetFilter(inputs.ruleset));
		}

		// Create a filter for any provided languages.
		if (inputs.language && stringArrayTypeGuard(inputs.language) && inputs.language.length) {
			filters.push(new LanguageFilter(inputs.language));
		}

		// Create a filter for provided rule names.
		// NOTE: Only a single rule name can be provided. It will be treated as a singleton list.
		if (inputs.rulename && typeof inputs.rulename === 'string') {
			filters.push(new RulenameFilter([inputs.rulename]));
		}

		return filters;
	}
}
