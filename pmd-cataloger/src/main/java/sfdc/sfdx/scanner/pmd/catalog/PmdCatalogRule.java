package sfdc.sfdx.scanner.pmd.catalog;

import java.util.*;

import org.w3c.dom.*;
import org.json.simple.*;
import sfdc.sfdx.scanner.ExitCode;

public class PmdCatalogRule {
  public static final String ATTR_NAME = "name";
  public static final String ATTR_MESSAGE = "message";
  public static final String ATTR_DESCRIPTION = "description";
  public static final String JSON_NAME = "name";
  public static final String JSON_MESSAGE = "message";
  public static final String JSON_DESCRIPTION = "description";
  public static final String JSON_LANGUAGES = "languages";
  public static final String JSON_CATEGORIES = "categories";
  public static final String JSON_RULESETS = "rulesets";

  private String name;
  private String message;
  private String description;
  private String language;
  /**
   * Seemingly all rules are defined in category XML files, so we can reasonably assume that each rule is a member of only
   * one category.
   */
  private PmdCatalogCategory category;
  /**
   * Rules can be included in an arbitrary number of rulesets.
   */
  private Set<PmdCatalogRuleset> rulesets = new HashSet<>();


  public PmdCatalogRule(Element element, PmdCatalogCategory category, String language) {
    this.name = element.getAttribute(ATTR_NAME);
    this.message = element.getAttribute(ATTR_MESSAGE);
    this.language = language;
    this.category = category;
    this.description = getDescription(element);
  }

  String getFullName() {
    return getCategoryPath() + "/" + getName();
  }

  String getName() {
    return name;
  }

  String getCategoryPath() {
    return category.getPath();
  }

  /**
   * Adds the provided ruleset to the list of rulesets of which this rule is a member.
   * @param ruleset - A ruleset of which this rule should be a member.
   */
  void addRuleset(PmdCatalogRuleset ruleset) {
    rulesets.add(ruleset);
  }

  /**
   *
   * @param element - A 'rule'-type node.
   * @return - A (possibly multi-line) string pulled from the input node's description-type child.
   */
  private String getDescription(Element element) {
    // The rule node should have at most one "description" node, so get that.
    NodeList nl = element.getElementsByTagName(ATTR_DESCRIPTION);
    String res = null;
    switch (nl.getLength()) {
      case 0:
        // Technically there should always be a description node, but if there wasn't one, we'll just return an empty string.
        res = "";
        break;
      case 1:
        Element descriptionNode = (Element) nl.item(0);
        res = descriptionNode.getTextContent();
        break;
      default:
        // If there was more than one description node, then something's gone crazy wrong and we should exit as gracefully
        // as possible.
        System.err.println("PMD Rule [" + getFullName() + "] has " + nl.getLength() + " 'description' elements. Please reduce this number to 1.");
        System.exit(ExitCode.PMD_MULTIPLE_RULE_DESCRIPTIONS.getCode());
    }
    return res;
  }

  /**
   * Converts this rule into a JSONObject.
   * @return - A JSONObject representing this rule.
   */
  JSONObject toJson() {
    Map<String,Object> m = new HashMap<>();
    m.put(JSON_NAME, this.name);
    m.put(JSON_MESSAGE, this.message);
    m.put(JSON_DESCRIPTION, this.description);

    // We want 'languages' to be represented as an array even though PMD rules only run against one language, because
    // this way it's easier to integrate with the language-agnostic framework that we ultimately want.
    List<String> langs = new ArrayList<>(Collections.singletonList((this.language)));
    m.put(JSON_LANGUAGES, langs);

    // We also want 'categories' to be an array for the same reason.
    List<String> categoryNames = new ArrayList<>(Collections.singletonList(this.category.getName()));
    m.put(JSON_CATEGORIES, categoryNames);

    List<String> rulesetNames = new ArrayList<>();
    for (PmdCatalogRuleset ruleset : rulesets) {
      rulesetNames.add(ruleset.getName());
    }
    m.put(JSON_RULESETS, rulesetNames);

    return new JSONObject(m);
  }
}
