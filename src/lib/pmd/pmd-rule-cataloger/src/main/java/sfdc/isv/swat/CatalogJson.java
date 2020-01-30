package sfdc.isv.swat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.*;

class CatalogJson {
  private List<CatalogRule> rules;
  private List<CatalogCategory> categories;
  private List<CatalogRuleset> rulesets;

  CatalogJson(List<CatalogRule> rules, List<CatalogCategory> categories, List<CatalogRuleset> rulesets) {
    this.rules = rules;
    this.categories = categories;
    this.rulesets = rulesets;
  }

  JSONObject constructJson() {
    JSONObject result = new JSONObject();

    result.put("rules", constructRulesList());
    result.put("categories", constructCategoriesMap());
    result.put("rulesets", constructRulesetsMap());

    return result;
  }

  private List<JSONObject> constructRulesList() {
    List<JSONObject> ruleJsons = new ArrayList<>();
    for (CatalogRule rule : this.rules) {
      ruleJsons.add(rule.toJson());
    }
    return ruleJsons;
  }

  private JSONObject constructCategoriesMap() {
    // We're going to iterate over every category we've got, and combine all categories with the same name into a single
    // entity in the catalog.
    Map<String,List<String>> categoryPathsByAlias = new HashMap<>();

    for (CatalogCategory category : this.categories) {
      String alias = category.getName();
      List<String> matchingPaths = categoryPathsByAlias.containsKey(alias) ? categoryPathsByAlias.get(alias) : new ArrayList<>();
      matchingPaths.add(category.getPath());
      categoryPathsByAlias.put(alias, matchingPaths);
    }

    return new JSONObject(categoryPathsByAlias);
  }

  private JSONObject constructRulesetsMap() {
    // We're going to iterate over every category we've got, and combine all categories with the same name into a single
    // entity in the catalog.
    Map<String,List<String>> rulesetPathsByAlias = new HashMap<>();

    for (CatalogRuleset ruleset : this.rulesets) {
      String alias = ruleset.getName();
      List<String> matchingPaths = rulesetPathsByAlias.containsKey(alias) ? rulesetPathsByAlias.get(alias) : new ArrayList<>();
      matchingPaths.add(ruleset.getPath());
      rulesetPathsByAlias.put(alias, matchingPaths);
    }

    return new JSONObject(rulesetPathsByAlias);
  }
}
