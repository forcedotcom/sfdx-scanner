package sfdc.isv.swat;

import java.util.*;

import org.w3c.dom.*;
import org.json.simple.*;

class CatalogRule {
  private String name;
  private String message;
  private String description;
  private String language;
  /**
   * Seemingly all rules are defined in category XML files, so we can reasonably assume that each rule is a member of only
   * one category.
   */
  private CatalogCategory category;
  /**
   * Rules can be included in an arbitrary number of rulesets.
   */
  private Set<CatalogRuleset> rulesets = new HashSet<>();


  CatalogRule(Element element, CatalogCategory category, String language) {
    this.name = element.getAttribute("name");
    this.message = element.getAttribute("message");
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
  void addRuleset(CatalogRuleset ruleset) {
    rulesets.add(ruleset);
  }

  /**
   *
   * @param element - A 'rule'-type node.
   * @return - A (possibly multi-line) string pulled from the input node's description-type child.
   */
  private String getDescription(Element element) {
    // The rule node should have at most one "description" node, so get that.
    NodeList nl = element.getElementsByTagName("description");
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
        System.exit(ExitCode.MULTIPLE_RULE_DESCRIPTIONS.getCode());
    }
    return res;
  }

  /**
   * Converts this rule into a JSONObject.
   * @return - A JSONObject representing this rule.
   */
  JSONObject toJson() {
    Map<String,Object> m = new HashMap<>();
    m.put("name", this.name);
    m.put("message", this.message);
    m.put("description", this.description);

    // We want 'languages' to be represented as an array even though PMD rules only run against one language, because
    // this way it's easier to integrate with the language-agnostic framework that we ultimately want.
    List<String> langs = new ArrayList<>(Collections.singletonList((this.language)));
    m.put("languages", langs);

    // We also want 'categories' to be an array for the same reason.
    List<String> categoryNames = new ArrayList<>(Collections.singletonList(this.category.getName()));
    m.put("categories", categoryNames);

    List<String> rulesetNames = new ArrayList<>();
    for (CatalogRuleset ruleset : rulesets) {
      rulesetNames.add(ruleset.getName());
    }
    m.put("rulesets", rulesetNames);

    return new JSONObject(m);
  }
}
