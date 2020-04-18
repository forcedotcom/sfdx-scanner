package sfdc.sfdx.scanner.pmd.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.*;

public class PmdCatalogJson {
	public static final String JSON_RULES = "rules";
	public static final String JSON_CATEGORIES = "categories";
	public static final String JSON_RULESETS = "rulesets";
	public static final String JSON_NAME = "name";
	public static final String JSON_PATHS = "paths";

	private final List<PmdCatalogRule> rules;
	private final List<PmdCatalogCategory> categories;
	private final List<PmdCatalogRuleset> rulesets;

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

		result.put(JSON_RULES, constructRulesJson());
		result.put(JSON_CATEGORIES, constructCategoriesJson());
		result.put(JSON_RULESETS, constructRulesetsJson());

		return result;
	}

	/**
	 * @return - A list of JSONs representing rules.
	 */
	private JSONArray constructRulesJson() {
		JSONArray ruleJsons = new JSONArray();
		for (PmdCatalogRule rule : this.rules) {
			ruleJsons.add(rule.toJson());
		}
		return ruleJsons;
	}

	/**
	 * @return - A JSON mapping category names by matching paths.
	 */
	private JSONArray constructCategoriesJson() {
		// Iterate over every category we've got, and combine all with the same name into a single entity
		Map<String, JSONObject> categoryPathsByAlias = new HashMap<>();
		for (PmdCatalogCategory category : this.categories) {
			addRulePath(categoryPathsByAlias, category.getName(), category.getPath());
		}

		JSONArray json = new JSONArray();
		json.addAll(categoryPathsByAlias.values());
		return json;
	}

	/**
	 * @return - A JSON mapping ruleset names to matching paths.
	 */
	private JSONArray constructRulesetsJson() {
		// Iterate over every ruleset we've got, and combine all with the same name into a single entity
		Map<String, JSONObject> rulesetPathsByAlias = new HashMap<>();
		for (PmdCatalogRuleset ruleset : this.rulesets) {
			addRulePath(rulesetPathsByAlias, ruleset.getName(), ruleset.getPath());
		}

		JSONArray json = new JSONArray();
		json.addAll(rulesetPathsByAlias.values());
		return json;
	}

	private void addRulePath(Map<String, JSONObject> pathsByAlias, String alias, String path) {
		JSONObject obj = pathsByAlias.get(alias);
		if (obj == null) {
			obj = new JSONObject();
			obj.put(JSON_NAME, alias);
			obj.put(JSON_PATHS, new ArrayList<String>());
			pathsByAlias.put(alias, obj);
		}
		ArrayList<String> paths = (ArrayList<String>) obj.get(JSON_PATHS);
		paths.add(path);
	}
}
