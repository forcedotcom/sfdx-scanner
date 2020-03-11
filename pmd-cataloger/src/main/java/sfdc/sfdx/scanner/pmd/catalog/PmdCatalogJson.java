package sfdc.sfdx.scanner.pmd.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.*;

@SuppressWarnings("unchecked")
public class PmdCatalogJson {
  public static final String JSON_RULES = "rules";
  public static final String JSON_CATEGORIES = "categories";
  public static final String JSON_RULESETS = "rulesets";

  private List<PmdCatalogRule> rules;
  private List<PmdCatalogCategory> categories;
  private List<PmdCatalogRuleset> rulesets;

  public PmdCatalogJson(List<PmdCatalogRule> rules, List<PmdCatalogCategory> categories, List<PmdCatalogRuleset> rulesets) {
    this.rules = rules;
    this.categories = categories;
    this.rulesets = rulesets;
  }

  /**
   * @return - A JSONObject describing the catalog's rules, rulesets, and categories.
   */
  public JSONObject constructJson() {
    JSONObject result = new JSONObject();

    result.put(JSON_RULES, constructRulesList());
    result.put(JSON_CATEGORIES, constructCategoriesMap());
    result.put(JSON_RULESETS, constructRulesetsMap());

    return result;
  }

  /**
   *
   * @return - A list of JSONs representing rules.
   */
  private List<JSONObject> constructRulesList() {
    List<JSONObject> ruleJsons = new ArrayList<>();
    for (PmdCatalogRule rule : this.rules) {
      ruleJsons.add(rule.toJson());
    }
    return ruleJsons;
  }

  /**
   *
   * @return - A JSON mapping category names by matching paths.
   */
  private JSONObject constructCategoriesMap() {
    // We're going to iterate over every category we've got, and combine all categories with the same name into a single
    // entity in the catalog.
    Map<String,List<String>> categoryPathsByAlias = new HashMap<>();

    for (PmdCatalogCategory category : this.categories) {
      String alias = category.getName();
      List<String> matchingPaths = categoryPathsByAlias.containsKey(alias) ? categoryPathsByAlias.get(alias) : new ArrayList<>();
      matchingPaths.add(category.getPath());
      categoryPathsByAlias.put(alias, matchingPaths);
    }

    return new JSONObject(categoryPathsByAlias);
  }

  /**
   *
   * @return - A JSON mapping ruleset names to matching paths.
   */
  private JSONObject constructRulesetsMap() {
    // We're going to iterate over every category we've got, and combine all categories with the same name into a single
    // entity in the catalog.
    Map<String,List<String>> rulesetPathsByAlias = new HashMap<>();

    for (PmdCatalogRuleset ruleset : this.rulesets) {
      String alias = ruleset.getName();
      List<String> matchingPaths = rulesetPathsByAlias.containsKey(alias) ? rulesetPathsByAlias.get(alias) : new ArrayList<>();
      matchingPaths.add(ruleset.getPath());
      rulesetPathsByAlias.put(alias, matchingPaths);
    }

    return new JSONObject(rulesetPathsByAlias);
  }
}
