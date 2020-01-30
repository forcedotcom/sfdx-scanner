package sfdc.isv.swat;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class CatalogRuleset {
  private String name;
  private String path;
  private String language;
  /**
   * Rulesets can directly include individual rules.
   */
  private Set<String> individuallyIncludedRules = new HashSet<>();
  /**
   * Rulesets can refer to entire categories or rulesets, thereby including all rules in that group except for those that are
   * explicitly excluded. So we'll create a map where the keys are included groups, and the values are sets of excluded
   * member rules. If a key maps to an empty set, it means no rules were excluded.
   */
  private Map<String,Set<String>> bulkInclusionMap = new HashMap<>();
  /**
   * It's possible for a ruleset to target rules specified in another ruleset, so we'll need to track whether this ruleset
   * does so.
   */
  private boolean targetsOtherRulesets = false;

  CatalogRuleset(Element root, String language, String path) {
    this.name = root.getAttribute("name");
    this.language = language;
    this.path = path;
    processRoot(root);
  }

  String getName() {
    return name;
  }

  String getPath() {
    return path;
  }

  String getLanguage() {
    return language;
  }

  private void processRoot(Element root) {
    // First, we want to get all of the 'rule'-type child nodes.
    NodeList ruleNodes = root.getElementsByTagName("rule");

    // Process each of the rule nodes iteratively.
    int nodeCount = ruleNodes.getLength();
    for (int i = 0; i < nodeCount; i++) {
      Element rule = (Element) ruleNodes.item(i);
      processRuleNode(rule);
    }
  }

  private void processRuleNode(Element rule) {
    String ref = rule.getAttribute("ref");
    // If we haven't already found nodes that reference other rulesets, check if this one does.
    if (!this.targetsOtherRulesets && ref.startsWith("rulesets")) {
      this.targetsOtherRulesets = true;
    }
    if (ref.endsWith(".xml")) {
      // If the reference property ends in .xml, then it refers to an entire category/ruleset, so we need to see if there are
      // any exclusions.
      // We'll do that by getting all nodes of type "exclude", iterating over them, and adding their "name" properties
      // to our set.
      Set<String> exclusionSet = new HashSet<>();
      NodeList exclusionNodes = rule.getElementsByTagName("exclude");
      int exclusionCount = exclusionNodes.getLength();
      for (int i = 0; i < exclusionCount; i++) {
        Element e = (Element) exclusionNodes.item(i);
        exclusionSet.add(e.getAttribute("name"));
      }
      // Then we'll map the set by the category path.
      this.bulkInclusionMap.put(ref, exclusionSet);
    } else if (ref.contains(".xml")) {
      // If the reference merely contains .xml, then it's a reference to a specific rule, so we should just add it to the set.
      this.individuallyIncludedRules.add(ref);
    } else {
      // If the reference doesn't have ".xml" anywhere in it, then there's something really fishy going on, and we
      // should just throw an error.
      // TODO: THROW AN ERROR INSTEAD OF EXITING.
      System.out.println("Couldn't understand reference '" + ref + "'.");
      System.exit(1);
    }
  }
}
