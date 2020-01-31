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

  // TODO: COMPLETE THIS CONSTRUCTOR.
  CatalogRule(Element element, CatalogCategory category, String language) {
    this.name = element.getAttribute("name");
    this.message = element.getAttribute("message");
    this.description = getDescription(element);
    this.language = language;
    this.category = category;
  }

  String getName() {
    return name;
  }

  String getCategoryPath() {
    return category.getPath();
  }

  void addRuleset(CatalogRuleset ruleset) {
    rulesets.add(ruleset);
  }

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
        // TODO: IMPROVE THE ERROR HANDLING HERE.
        System.out.println("Instead of one description node, rule " + this.name + " has this many: " + nl.getLength() + ". Burn it all down");
        System.exit(1);
    }
    return res;
  }


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
